package tbh.chat_app.config.oauth;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import tbh.chat_app.config.oauth.user.OAuth2UserInfo;
import tbh.chat_app.config.oauth.user.OAuth2UserInfoFactory;
import tbh.chat_app.model.User;
import tbh.chat_app.repository.UserRepository;
import tbh.chat_app.utils.const_variables.AuthProvider;
import tbh.chat_app.utils.const_variables.Role;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // Normalize attributes via OAuth2UserInfoFactory so OIDC and OAuth2 flows use same pipeline
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        System.out.println("Oauth2 User Attributes: ========================================\\n" + oidcUser.getClaims());
        OAuth2UserInfo userInfo =
            OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oidcUser.getClaims());

        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not found from OIDC provider");
        }

        try {
            User user = userRepository.findByEmail(email).block();
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setProvider(AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase()));
                user.setProviderId(userInfo.getId() != null ? userInfo.getId() : oidcUser.getSubject());
                user.setRoles(List.of(Role.ROLE_USER));
                user.setPassword(UUID.randomUUID().toString());
                user.setAvatarUrl(userInfo.getImageUrl() != null ? userInfo.getImageUrl() : oidcUser.getPicture());
                user.setFirstName(userInfo.getFirstName() != null ? userInfo.getFirstName() : oidcUser.getGivenName());
                user.setLastName(userInfo.getLastName() != null ? userInfo.getLastName() : oidcUser.getFamilyName());
                user = userRepository.save(user).block();
                System.out.println("Created new user from OIDC: " + user.getEmail());
            } else {
                user.setProvider(AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase()));
                user.setProviderId(userInfo.getId() != null ? userInfo.getId() : oidcUser.getSubject());

                user.setAvatarUrl(userInfo.getImageUrl() != null ? userInfo.getImageUrl() : oidcUser.getPicture());
                user.setFirstName(userInfo.getFirstName() != null ? userInfo.getFirstName() : oidcUser.getGivenName());
                user.setLastName(userInfo.getLastName() != null ? userInfo.getLastName() : oidcUser.getFamilyName());
                
                user = userRepository.save(user).block();
                System.out.println("Updated existing OIDC user: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Error while creating/updating OIDC user: " + e.getMessage());
            e.printStackTrace();
        }

        return oidcUser;
    }
}
