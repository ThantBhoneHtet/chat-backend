package tbh.chat_app.dto.chat;

import com.google.cloud.Timestamp;

public record ChatParticipantsDto (
        String id,
        String firstName,
        String lastName,
        String avatarUrl,
        Boolean isOnline,
        Timestamp lastActive
) {}

