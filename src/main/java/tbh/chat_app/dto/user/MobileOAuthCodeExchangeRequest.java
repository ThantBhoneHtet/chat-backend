package tbh.chat_app.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MobileOAuthCodeExchangeRequest(
        @NotBlank
        @Size(min = 43, max = 43)
        @Pattern(regexp = "[A-Za-z0-9_-]+")
        String code,

        @NotBlank
        @Size(min = 43, max = 128)
        @Pattern(regexp = "[A-Za-z0-9._~-]+")
        String codeVerifier) {}
