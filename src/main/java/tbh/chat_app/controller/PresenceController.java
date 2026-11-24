package tbh.chat_app.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import tbh.chat_app.config.jwt.UserPrincipal;
import tbh.chat_app.service.PresenceService;

@Controller
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Object eventPrincipal = event.getUser() instanceof Authentication authentication
                ? authentication.getPrincipal()
                : event.getUser();
        if (eventPrincipal instanceof UserPrincipal principal) {
            presenceService.connect(accessor.getSessionId(), principal.getId());
        }
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        presenceService.disconnect(event.getSessionId());
    }

    @MessageMapping("/presence/heartbeat")
    public void heartbeat(MessageHeaders headers) {
        presenceService.heartbeat((String) headers.get(SimpMessageHeaderAccessor.SESSION_ID_HEADER));
    }
}
