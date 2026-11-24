package tbh.chat_app.config.jwt;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    public StompAuthenticationInterceptor(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        MessageHeaderAccessor mutableAccessor = MessageHeaderAccessor.getMutableAccessor(message);
        StompHeaderAccessor accessor = mutableAccessor instanceof StompHeaderAccessor stompAccessor
                ? stompAccessor
                : null;
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("A bearer JWT is required for STOMP connections");
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty() || !jwtUtils.validateJwtToken(token)) {
            throw new IllegalArgumentException("Invalid STOMP JWT");
        }

        String email = jwtUtils.getUserNameFromJwtToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!(userDetails instanceof UserPrincipal principal)
                || !email.equals(principal.getUsername())) {
            throw new IllegalArgumentException("STOMP JWT identity could not be resolved");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}
