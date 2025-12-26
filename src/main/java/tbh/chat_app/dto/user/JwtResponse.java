package tbh.chat_app.dto.user;

public record JwtResponse(
    String token,
    String message
    
    // other necessary fields here
) {}
