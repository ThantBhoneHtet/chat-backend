package tbh.chat_app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping("/users")
public class UserStatusController {

    @PostMapping("/{userId}/status/{isOnline}")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable String userId, @PathVariable boolean isOnline) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body("Presence is managed by authenticated WebSocket connections.");
    }
}
