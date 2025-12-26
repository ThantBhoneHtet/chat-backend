package tbh.chat_app.config.oauth;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import tbh.chat_app.config.jwt.UserPrincipal;
import tbh.chat_app.config.oauth.user.OAuth2UserInfo;
import tbh.chat_app.config.oauth.user.OAuth2UserInfoFactory;
import tbh.chat_app.model.User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;
import tbh.chat_app.repository.UserRepository;
import tbh.chat_app.utils.const_variables.AuthProvider;
import tbh.chat_app.utils.const_variables.Role;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        System.out.println("=== OAuth2UserService.loadUser() called ===");
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            System.err.println("Error in OAuth2UserService: " + ex.getMessage());
            ex.printStackTrace();
            throw new OAuth2AuthenticationException("OAuth2 processing failed");
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        // Extract user info from OAuth2 provider and normalize attributes using factory
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        // use factory to normalize provider attributes
        System.out.println("Oauth2 User Attributes: ========================================\n" + oAuth2User.getAttributes());
        OAuth2UserInfo userInfo =
            OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        String email = userInfo.getEmail();

        // If provider is GitHub and email is missing, try to fetch from GitHub API /user/emails
        if ((email == null || email.trim().isEmpty()) && registrationId.equalsIgnoreCase("github")) {
            String githubEmail = fetchGithubEmail(oAuth2UserRequest);
            if (githubEmail != null && !githubEmail.trim().isEmpty()) {
                email = githubEmail;
            }
        }
        // name intentionally unused; attributes available via userInfo

        System.out.println("Processing OAuth2 user with email: " + email);
        if (email == null || email.trim().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        
        // Check if user exists
        User user = userRepository.findByEmail(email).block();
        System.out.println("Found user in DB: " + (user != null));

        
        if (user == null) {
            // Register new OAuth2 user
            user = registerNewOAuth2User(registrationId, userInfo);
            System.out.println("Created new user: " + user.getEmail());
        } else {
            // Update existing user if needed
            user = updateExistingOAuth2User(user, registrationId, userInfo);
            System.out.println("Updated existing user: " + user.getEmail());
        }
        
        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        User user = new User();
        user.setEmail(userInfo.getEmail());
        // user.setName(oAuth2User.getAttribute("name"));
        user.setProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
        user.setProviderId(userInfo.getId());
        user.setAvatarUrl(userInfo.getImageUrl());
        user.setFirstName(userInfo.getFirstName());
        user.setLastName(userInfo.getLastName());
        user.setRoles(List.of(Role.ROLE_USER)); // Default role
        
        // Set a random password for OAuth2 users (they won't use it)
        user.setPassword(UUID.randomUUID().toString());
        
        return userRepository.save(user).block();
    }

    private User updateExistingOAuth2User(User existingUser, String registrationId, OAuth2UserInfo userInfo) {
        // Update user details if needed
        // existingUser.setName(oAuth2User.getAttribute("name"));
        existingUser.setProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
        existingUser.setProviderId(userInfo.getId());

        existingUser.setAvatarUrl(userInfo.getImageUrl());
        existingUser.setFirstName(userInfo.getFirstName());
        existingUser.setLastName(userInfo.getLastName());
        
        return userRepository.save(existingUser).block();
    }

    // Fetch emails from GitHub when the email is not present on main profile
    private String fetchGithubEmail(OAuth2UserRequest request) {
        try {
            String token = request.getAccessToken().getTokenValue();
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            // GitHub accepts 'token ' or 'Bearer '
            // GitHub prefers 'Bearer' for OAuth tokens and requires a User-Agent header
            headers.set("Authorization", "Bearer " + token);
            headers.set("User-Agent", "chat-app");
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> resp = rest.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                for (Map<String, Object> e : resp.getBody()) {
                    Boolean primary = Boolean.valueOf(String.valueOf(e.getOrDefault("primary", "false")));
                    Boolean verified = Boolean.valueOf(String.valueOf(e.getOrDefault("verified", "false")));
                    String email = (String) e.get("email");
                    if (email != null && (primary || verified)) {
                        return email;
                    }
                }
                // fallback to first available email
                Map<String, Object> first = resp.getBody().stream().findFirst().orElse(null);
                if (first != null) return (String) first.get("email");
            }
        } catch (Exception ex) {
            System.err.println("could not fetch github emails: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }
}