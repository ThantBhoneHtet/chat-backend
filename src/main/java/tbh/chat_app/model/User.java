package tbh.chat_app.model;


import java.util.List;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tbh.chat_app.utils.const_variables.AuthProvider;
import tbh.chat_app.utils.const_variables.Role;

@Data
@Document(collectionName = "users")
public class User {
    @DocumentId
    private String userId;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Location location;
    private String bio;
    private String gender;
    private String phoneNumber;
    private String status; // "active" | "suspended"
    private Timestamp createdAt;
    private Timestamp lastLogin;
    private PrivacySettings privacySettings;
    private Timestamp lastActive;
    private Boolean isOnline;
    private List<Role> roles;
    private List<String> savedMessages;

    // OAuth fields
    private AuthProvider provider;
    private String providerId;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Location {
        private String coutry;
        private String city;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrivacySettings {
        private String profileVisibility; // "public" | "connections" | "private"
        private Boolean searchVisibility;
    }
}