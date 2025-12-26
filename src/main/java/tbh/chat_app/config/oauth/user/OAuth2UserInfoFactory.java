package tbh.chat_app.config.oauth.user;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId == null) return null;

        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        }

        if (registrationId.equalsIgnoreCase("github")) {
            return new GithubOAuth2UserInfo(attributes);
        }

        // Add more providers here
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }
}
