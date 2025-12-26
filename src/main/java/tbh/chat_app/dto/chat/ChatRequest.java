package tbh.chat_app.dto.chat;

import java.util.List;

public record ChatRequest(
    String type, // "DIRECT" or "GROUP"
    String name, // Required for GROUP
    String owner, // Required for GROUP
    String gpImageUrl,
    List<String> participantIds // At least 1 for DIRECT, 2+ for GROUP
) {}
