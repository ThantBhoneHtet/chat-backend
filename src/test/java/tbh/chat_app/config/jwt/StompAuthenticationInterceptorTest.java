package tbh.chat_app.config.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class StompAuthenticationInterceptorTest {

    private JwtUtils jwtUtils;
    private CustomUserDetailsService userDetailsService;
    private StompAuthenticationInterceptor interceptor;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        interceptor = new StompAuthenticationInterceptor(jwtUtils, userDetailsService);
        principal = new UserPrincipal("user-1", "user@example.com", "", List.of());
        when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(principal);
    }

    @Test
    void validatesConnectTokenAndBindsResolvedUserIdentity() {
        Message<?> message = connectMessage("Bearer valid-token");

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) StompHeaderAccessor.wrap(result).getUser();

        assertSame(principal, authentication.getPrincipal());
        assertEquals("user-1", ((UserPrincipal) authentication.getPrincipal()).getId());
        verify(userDetailsService).loadUserByUsername("user@example.com");
    }

    @Test
    void rejectsConnectWithoutBearerToken() {
        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preSend(connectMessage(null), mock(MessageChannel.class)));
    }

    @Test
    void rejectsInvalidToken() {
        when(jwtUtils.validateJwtToken("invalid-token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> interceptor.preSend(
                        connectMessage("Bearer invalid-token"), mock(MessageChannel.class)));
    }

    private Message<?> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
