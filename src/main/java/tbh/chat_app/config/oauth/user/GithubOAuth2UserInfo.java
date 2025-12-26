package tbh.chat_app.config.oauth.user;

import java.util.Map;

public class GithubOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        // GitHub exposes id as a number
        Object v = attributes.get("id");
        return v == null ? null : String.valueOf(v);
    }

    @Override
    public String getEmail() {
        // GitHub may not expose email in primary profile - check attribute email
        String email = (String) attributes.get("email");
        return email;
    }

    @Override
    public String getName() {
        // GitHub may leave name null — fall back to login if name not provided
        String name = (String) attributes.get("name");
        if (name == null || name.trim().isEmpty()) {
            return (String) attributes.get("login");
        }
        return name;
    }

    @Override
    public String getFirstName() {
        // GitHub doesn't separate first/last; derive from name
        String full = getName();
        if (full == null) return null;
        String[] parts = full.split(" ");
        return parts.length > 0 ? parts[0] : full;
    }

    @Override
    public String getLastName() {
        String full = getName();
        if (full == null) return null;
        String[] parts = full.split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : null;
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
