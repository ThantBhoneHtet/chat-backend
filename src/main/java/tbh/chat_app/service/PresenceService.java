package tbh.chat_app.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;

import tbh.chat_app.model.User;

@Service
public class PresenceService {

    public static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(20);
    public static final Duration SESSION_TIMEOUT = Duration.ofSeconds(60);

    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Object lock = new Object();
    private final Map<String, Map<String, Instant>> sessionsByUser = new HashMap<>();
    private final Map<String, String> usersBySession = new HashMap<>();

    public PresenceService(UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    public void connect(String sessionId, String userId) {
        synchronized (lock) {
            if (sessionId == null || userId == null || usersBySession.containsKey(sessionId)) {
                return;
            }
            Map<String, Instant> sessions = sessionsByUser.computeIfAbsent(userId, key -> new HashMap<>());
            boolean transitionedOnline = sessions.isEmpty();
            sessions.put(sessionId, Instant.now());
            usersBySession.put(sessionId, userId);
            if (transitionedOnline) {
                updatePresence(userId, true);
            }
        }
    }

    public void heartbeat(String sessionId) {
        synchronized (lock) {
            String userId = usersBySession.get(sessionId);
            if (userId == null) {
                return;
            }
            Map<String, Instant> sessions = sessionsByUser.computeIfAbsent(userId, key -> new HashMap<>());
            if (sessions.isEmpty()) {
                updatePresence(userId, true);
            }
            sessions.put(sessionId, Instant.now());
        }
    }

    public void disconnect(String sessionId) {
        synchronized (lock) {
            String userId = usersBySession.remove(sessionId);
            if (userId == null) {
                return;
            }
            Map<String, Instant> sessions = sessionsByUser.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    sessionsByUser.remove(userId);
                    updatePresence(userId, false);
                }
            }
        }
    }

    public boolean isOnline(String userId) {
        synchronized (lock) {
            Map<String, Instant> sessions = sessionsByUser.get(userId);
            return sessions != null && !sessions.isEmpty();
        }
    }

    public int countOnlineUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        synchronized (lock) {
            Set<String> uniqueIds = new HashSet<>(userIds);
            return (int) uniqueIds.stream().filter(sessionsByUser::containsKey).count();
        }
    }

    public int countOnlineUsers() {
        synchronized (lock) {
            return sessionsByUser.size();
        }
    }

    @Scheduled(fixedDelay = 1_000)
    public void expireStaleSessions() {
        expireStaleSessions(Instant.now());
    }

    void expireStaleSessions(Instant now) {
        List<String> staleSessions = new ArrayList<>();
        Instant cutoff = now.minus(SESSION_TIMEOUT);
        synchronized (lock) {
            sessionsByUser.values().forEach(sessions -> sessions.forEach((sessionId, lastHeartbeat) -> {
                if (!lastHeartbeat.isAfter(cutoff)) {
                    staleSessions.add(sessionId);
                }
            }));
        }
        staleSessions.forEach(sessionId -> expireSession(sessionId, cutoff));
    }

    private void expireSession(String sessionId, Instant cutoff) {
        synchronized (lock) {
            String userId = usersBySession.get(sessionId);
            Map<String, Instant> sessions = userId == null ? null : sessionsByUser.get(userId);
            Instant lastHeartbeat = sessions == null ? null : sessions.get(sessionId);
            if (lastHeartbeat == null || lastHeartbeat.isAfter(cutoff)) {
                return;
            }
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId);
                updatePresence(userId, false);
            }
        }
    }

    private void updatePresence(String userId, boolean isOnline) {
        User user = userService.getUser(userId);
        if (user == null) {
            return;
        }
        user.setIsOnline(isOnline);
        if (!isOnline) {
            user.setLastActive(Timestamp.now());
        }
        userService.saveUser(user);
        messagingTemplate.convertAndSend(
                "/topic/user-status",
                Map.of("userId", userId, "isOnline", isOnline));
    }
}
