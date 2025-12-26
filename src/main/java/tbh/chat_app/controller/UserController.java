package tbh.chat_app.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tbh.chat_app.dto.user.UserCreationDto;
import tbh.chat_app.dto.user.UserUpdateDto;
import tbh.chat_app.model.User;
import tbh.chat_app.service.UserService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreationDto dto) {
        try {
            User createdUser = userService.createUser(dto).block();
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error creating user"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(userService.getUser(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        if (email == null) return ResponseEntity.badRequest().body(null);
        
        try{
            return ResponseEntity.ok(userService.findUserByEmail(email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) String nameKeyword) {
        if (nameKeyword != null && !nameKeyword.isBlank()) {
            return ResponseEntity.ok(userService.searchUsersByKeyword(nameKeyword));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestPart("dto") UserUpdateDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, dto, image).block());
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        try {
            return ResponseEntity.ok(userService.deleteUser(id).block());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(Map.of(
                    "completedTasks", 10,
                    "ongoingTasks", 2,
                    "skills", List.of("Java", "Spring Boot")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error retrieving stats"));
        }
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable String userId,
            @RequestBody Map<String, String> status) {
        try {
            userService.updateStauts(userId, status.get("status"));
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error updating status"));
        }
    }

    @GetMapping("/count/active")
    public Mono<Long> getTotalActiveUsers() {
        return userService.getTotalActiveUsers();
    }

    @GetMapping("/{userId}/saved-messages")
    public ResponseEntity<List<String>> getSavedMessages(@PathVariable String userId) {
        try {
            List<String> savedMessages = userService.getSavedMessages(userId);
            return ResponseEntity.ok(savedMessages);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

}