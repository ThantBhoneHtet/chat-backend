package tbh.chat_app.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import tbh.chat_app.model.User;

class PresenceServiceTest {

    private UserService userService;
    private SimpMessagingTemplate messagingTemplate;
    private PresenceService presenceService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = org.mockito.Mockito.mock(UserService.class);
        messagingTemplate = org.mockito.Mockito.mock(SimpMessagingTemplate.class);
        presenceService = new PresenceService(userService, messagingTemplate);
        user = new User();
        user.setUserId("user-1");
        when(userService.getUser("user-1")).thenReturn(user);
    }

    @Test
    void broadcastsOnlyWhenTheLastSessionChangesPresence() {
        presenceService.connect("session-1", "user-1");
        presenceService.connect("session-2", "user-1");
        presenceService.disconnect("session-1");

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user-status"), anyMap());
        verify(userService, times(1)).saveUser(user);
        org.junit.jupiter.api.Assertions.assertTrue(presenceService.isOnline("user-1"));

        presenceService.disconnect("session-2");

        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/user-status"), anyMap());
        verify(userService, times(2)).saveUser(user);
        org.junit.jupiter.api.Assertions.assertFalse(presenceService.isOnline("user-1"));
    }

    @Test
    void heartbeatExtendsSessionAndExpiredSessionCanBecomeActiveAgain() {
        presenceService.connect("session-1", "user-1");
        presenceService.expireStaleSessions(Instant.now().plusSeconds(55));
        presenceService.heartbeat("session-1");
        presenceService.expireStaleSessions(Instant.now().plusSeconds(59));

        org.junit.jupiter.api.Assertions.assertTrue(presenceService.isOnline("user-1"));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user-status"), anyMap());

        presenceService.expireStaleSessions(Instant.now().plusSeconds(61));

        org.junit.jupiter.api.Assertions.assertFalse(presenceService.isOnline("user-1"));
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/user-status"), anyMap());

        presenceService.heartbeat("session-1");
        org.junit.jupiter.api.Assertions.assertTrue(presenceService.isOnline("user-1"));
        presenceService.disconnect("session-1");
        org.junit.jupiter.api.Assertions.assertFalse(presenceService.isOnline("user-1"));
        verify(messagingTemplate, times(4)).convertAndSend(eq("/topic/user-status"), anyMap());
    }

    @Test
    void unknownDisconnectDoesNotChangePresence() {
        presenceService.disconnect("unknown-session");

        verify(userService, never()).saveUser(any());
        verify(messagingTemplate, never()).convertAndSend(any(String.class), anyMap());
    }
}
