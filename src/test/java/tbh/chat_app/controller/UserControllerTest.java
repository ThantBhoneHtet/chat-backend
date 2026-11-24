package tbh.chat_app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import tbh.chat_app.config.jwt.UserPrincipal;
import tbh.chat_app.dto.user.UserUpdateDto;
import tbh.chat_app.service.UserService;

class UserControllerTest {

    @Test
    void preventsUpdatingAnotherUsersProfile() {
        var controller = new UserController();
        var userService = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
        var principal = new UserPrincipal("user-1", "user@example.com", "", List.of());

        var response = controller.updateUser(
                "user-2",
                new UserUpdateDto(null, null, null, null, null, null, null, null, null, null),
                null,
                principal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(userService);
    }
}
