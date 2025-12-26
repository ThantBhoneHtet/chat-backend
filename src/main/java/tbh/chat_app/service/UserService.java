package tbh.chat_app.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

import tbh.chat_app.dto.user.UserCreationDto;
import tbh.chat_app.dto.user.UserUpdateDto;
import tbh.chat_app.model.User;
import tbh.chat_app.repository.UserRepository;
import tbh.chat_app.utils.UserCustomRepo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCustomRepo userCustomRepo;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private Firestore firestore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Mono<User> createUser(UserCreationDto dto) {
        if (userCustomRepo.existsByEmail(dto.email())) {
            return Mono.error(new IllegalArgumentException("Email already exists"));
        }

        // UserDetails userDetails = org.springframework.security.core.userdetails
        //                     .User.withUsername(dto.email())
        //                     .password(passwordEncoder.encode(dto.password()))
        //                     .roles(dto.role())
        //                     .build();
                            

        User user = new User();
        user.setEmail(dto.email());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPassword(passwordEncoder.encode(dto.password()));
        // user.setPassword(dto.password());
        user.setStatus("active");
        user.setCreatedAt(Timestamp.now());
        user.setStatus("active");
        user.setRoles(dto.role());
        user.setSavedMessages(new ArrayList<>());

        return userRepository.save(user);
    }

    public User getUser(String id) {
        return userRepository.findById(id).block();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll().collectList().block();
    }

    public Mono<User> updateUser(String id, UserUpdateDto dto, MultipartFile image)
            throws IOException, ExecutionException, InterruptedException { // Declare exceptions to be thrown

        User user = userRepository.findById(id).block(); // Block and get the user
        if (user == null) {
            throw new RuntimeException("User with ID " + id + " not found.");
        }

        // Handle image upload if a new file is provided
        if (image != null) {
            // Optional: Consider deleting the old image from S3 if the URL is changing.
            // This would require fetching the old avatarUrl before setting the new one.
            String newImageUrl = s3Service.uploadFile(image);
            user.setAvatarUrl(newImageUrl); // Set the new URL
        } else if (dto.avatarUrl() != null) {
            user.setAvatarUrl(dto.avatarUrl());
        }

        // Update other fields from DTO if they are not null
        if (dto.password() != null) {
            user.setPassword(dto.password());
        }
        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
        }
        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
        }

        if (dto.location() != null) {
            String city = dto.location().city() != null ? dto.location().city()
                    : (user.getLocation() != null ? user.getLocation().getCity() : null);
            String country = dto.location().country() != null ? dto.location().country()
                    : (user.getLocation() != null ? user.getLocation().getCoutry() : null);
            user.setLocation(new User.Location(country, city));
        }

        if (dto.bio() != null) {
            user.setBio(dto.bio());
        }

        if (dto.phoneNumber() != null) {
            user.setPhoneNumber(dto.phoneNumber());
        }

        if (dto.gender() != null) {
            user.setGender(dto.gender());
        }

        if (dto.privacySettings() != null) {
            user.setPrivacySettings(
                    new User.PrivacySettings(
                            dto.privacySettings().profileVisibility(),
                            dto.privacySettings().searchVisibility()));
        } else if (user.getPrivacySettings() == null) {
            // If DTO privacy settings are null and existing are null, initialize default
            user.setPrivacySettings(new User.PrivacySettings(null, null));
        }

        // Update the document in Firestore
        ApiFuture<WriteResult> future = firestore.collection("users").document(id).set(user);
        future.get(); // Wait for the write to complete

        // If you are also using a Spring Data Reactive Repository, you might save it
        // here too synchronously
        return userRepository.save(user); // Block on saving to the repository

    }

    public Mono<Void> deleteUser(String id) {
        return userRepository.deleteById(id);
    }

    public boolean existByEmail(String email) {
        return userCustomRepo.existsByEmail(email);
    }

    // public User login(String email, String password) {
    //     User user = userRepository.findByEmail(email).block();
    //     if (user != null && passwordEncoder.matches(password, user.getPassword())) {
    //         return user;
    //     }

    //     return null;
    // }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).block();
    }

    public void saveUser(User user) {
        userRepository.save(user).block();
    }

    public Mono<User> findUserById(String id) {
        return userRepository.findById(id);
    }

    public void logout(String id) {
        User user = userRepository.findById(id).block();
        if (user != null) {
            user.setLastLogin(Timestamp.now());
            userRepository.save(user).block();
        }
    }

    public void updateStauts(String id, String status) {
        User user = userRepository.findById(id).block();
        if (user != null) {
            String previousStatus = user.getStatus();
            user.setStatus(status);
            userRepository.save(user).block();

            // // Log activity for status changes
            // if (!status.equals(previousStatus)) {
            //     if ("suspended".equals(status)) {
            //         activityLogger.logUserSuspension(
            //                 id,
            //                 user.getFirstName() + " " + user.getLastName(),
            //                 "system", // You might want to pass actual admin ID
            //                 "System Admin",
            //                 7, // Default suspension period
            //                 "Account suspended by admin");
            //     } else if ("active".equals(status) && "suspended".equals(previousStatus)) {
            //         activityLogger.logUserReactivation(
            //                 id,
            //                 user.getFirstName() + " " + user.getLastName(),
            //                 "system", // You might want to pass actual admin ID
            //                 "System Admin");
            //     }
            // }
        }
    }

    /**
     * Get the total number of active users (excluding suspended ones)
     * 
     * @return Mono<Long> containing the count of active users
     */
    public Mono<Long> getTotalActiveUsers() {
        return userRepository.findAll()
                .filter(user -> "active".equals(user.getStatus()))
                .count()
                .onErrorResume(error -> {
                    System.err.println("Error counting active users: " + error.getMessage());
                    return Mono.just(0L);
                });
    }

    public List<User> searchUsersByKeyword(String nameKeyword) {
        return userCustomRepo.searchUsersByKeyword(nameKeyword);
    }

    public void saveMessageForUser(String userId, String messageId) {
        User user = userRepository.findById(userId).block();
        if (user == null) return;

        List<String> savedMessages = user.getSavedMessages();
        if (savedMessages == null) {
            savedMessages = new ArrayList<>();
            System.out.println("Initialized saved messages list for user " + userId);
        }

        if (savedMessages.contains(messageId)) {
            savedMessages.remove(messageId);
            System.out.println("Message " + messageId + " removed from saved messages for user " + userId);
        } else if (savedMessages.size() < 50) {
            savedMessages.add(messageId);
            System.out.println("Message " + messageId + " added to saved messages for user " + userId);
        }

        user.setSavedMessages(savedMessages);
        userRepository.save(user).block();
        System.out.println("Updated saved messages for user " + userId + ": " + savedMessages);
    }

    public List<String> getSavedMessages(String userId) {
        User user = userRepository.findById(userId).block();
        if (user == null) return new ArrayList<>();

        List<String> savedMessages = user.getSavedMessages();
        if (savedMessages == null) {
            savedMessages = new ArrayList<>();
        }

        return savedMessages;
    }
}
