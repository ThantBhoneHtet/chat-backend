package tbh.chat_app.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tbh.chat_app.model.User;
import tbh.chat_app.service.UserService;

@RestController
@CrossOrigin("*")
@RequestMapping("/users")
public class UserStatusController {

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{userId}/status/{isOnline}")
    public void updateUserStatus(@PathVariable String userId, @PathVariable boolean isOnline){
        boolean updated = false;

        User user = userService.findUserById(userId).block();
        if (user != null) {
            user.setIsOnline(isOnline);
            userService.saveUser(user);
            updated = true;
        }

        if (updated) {
            // Broadcast status change over WebSocket
            messagingTemplate.convertAndSend(
                "/topic/user-status", 
                Map.of("userId", userId, "isOnline", isOnline)
            );
        }
    }
}
