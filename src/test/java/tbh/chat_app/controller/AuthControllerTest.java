package tbh.chat_app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import tbh.chat_app.config.jwt.UserPrincipal;
import tbh.chat_app.model.User;
import tbh.chat_app.service.UserService;
import tbh.chat_app.utils.const_variables.Role;

class AuthControllerTest {

    @Test
    void currentUserReturnsApplicationProfileForOAuth2UserPrincipal() {
        UserService userService = mock(UserService.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "userService", userService);

        User user = new User();
        user.setUserId("user-123");
        user.setEmail("user@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRoles(List.of(Role.ROLE_USER));

        UserPrincipal principal = UserPrincipal.create(user, Map.of("email", "provider@example.com"));
        when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        var response = controller.getCurrentUser(authentication);

        assertEquals(user.getUserId(), ((Map<?, ?>) response.getBody()).get("userId"));
        assertEquals(user.getEmail(), ((Map<?, ?>) response.getBody()).get("email"));
    }
}
