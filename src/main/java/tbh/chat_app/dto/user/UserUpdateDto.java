package tbh.chat_app.dto.user;

import java.util.List;

public record UserUpdateDto (
    String password,
    String firstName,
    String lastName,
    String avatarUrl,
    Location location,
    String bio,
    PrivacySettings privacySettings,
        List<String> availability,
    String phoneNumber,
    String gender
){
    public record PrivacySettings(
        String profileVisibility,
        Boolean searchVisibility
    ) {}

    public record Location(
        String country,
        String city
    ) {}
}


