package tbh.chat_app.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import tbh.chat_app.config.jwt.JwtUtils;
import tbh.chat_app.dto.user.JwtResponse;
import tbh.chat_app.dto.user.LoginRequest;
import tbh.chat_app.dto.user.UserCreationDto;
import tbh.chat_app.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @GetMapping("/oauth-user")
    public Map<String, Object> getOAuthUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }

    // // OAuth2 success handler endpoint
    // @GetMapping("/oauth2/success")
    // public ResponseEntity<?> oauth2Success(Authentication authentication) {
    //     try {
    //         OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            
    //         // Generate JWT token for OAuth2 user
    //         String jwtToken = prepareJwtToken(authentication);
    //         JwtResponse response = new JwtResponse(jwtToken, "OAuth2 login successful.");
            
    //         return ResponseEntity.ok(response);
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //             .body("OAuth2 authentication failed");
    //     }
    // }

    // OAuth2 failure handler endpoint
    @GetMapping("/oauth2/failure")
    public ResponseEntity<?> oauth2Failure() {
        Map<String, Object> map = new HashMap<>();
        map.put("message", "OAuth2 authentication failed");
        map.put("status", false);
        return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserCreationDto signUpRequest) {
        if (userService.existByEmail(signUpRequest.email())) {
            return ResponseEntity.badRequest().body("Email already exist!");
        }
        
        userService.createUser(signUpRequest).block();


        // Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                signUpRequest.email(),
                signUpRequest.password()
            )
        );

        String jwt = prepareJwtToken(authentication);

        return ResponseEntity.created(null).body(new JwtResponse(jwt, "Registration successful."));
    }
    
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
        } catch (AuthenticationException exception) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwtToken = prepareJwtToken(authentication);
        JwtResponse response = new JwtResponse(jwtToken, "Login successful.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Full authentication is required to access this resource");
            map.put("status", HttpStatus.UNAUTHORIZED.value());
            return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        // If OAuth2 authenticated principal
        if (principal instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) principal;
            System.out.println(oauthUser.getAttributes());
            return ResponseEntity.ok(oauthUser.getAttributes());
        }

        // If standard UserDetails principal
        if (principal instanceof UserDetails) {
            UserDetails user = (UserDetails) principal;
            Map<String, Object> body = new HashMap<>();
            body.put("username", user.getUsername());
            body.put("role", user.getAuthorities());
            body.put("enabled", user.isEnabled());
            body.put("accountNonExpired", user.isAccountNonExpired());
            body.put("accountNonLocked", user.isAccountNonLocked());
            body.put("credentialsNonExpired", user.isCredentialsNonExpired());
            return ResponseEntity.ok(body);
        }

        // Fallback: return principal.toString()
        return ResponseEntity.ok(Map.of("principal", principal != null ? principal.toString() : null));
    }

    public String prepareJwtToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
        return jwtToken;
    }
}
