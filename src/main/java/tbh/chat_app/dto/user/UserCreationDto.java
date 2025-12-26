package tbh.chat_app.dto.user;

import java.util.List;

import jakarta.validation.constraints.*;
import tbh.chat_app.utils.const_variables.Role;

public record UserCreationDto(
    @Email
    String email,

    @NotBlank
    String password,
    
    @NotBlank
    String firstName,
    
    @NotBlank
    String lastName,

    List<Role> role
) {}

