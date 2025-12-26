package tbh.chat_app.dto.user;

public record LoginRequest(
    String email,
    String password
) {} 