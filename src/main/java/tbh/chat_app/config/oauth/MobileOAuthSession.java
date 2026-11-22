package tbh.chat_app.config.oauth;

import org.springframework.web.util.UriComponentsBuilder;

public final class MobileOAuthSession {

    public static final String CODE_CHALLENGE_ATTRIBUTE = "mobileOAuth.codeChallenge";
    public static final String STATE_ATTRIBUTE = "mobileOAuth.state";
    public static final String CALLBACK_URI = "chatwithme://oauth/callback";

    private MobileOAuthSession() {}

    public static String successRedirect(String code, String state) {
        return UriComponentsBuilder.fromUriString(CALLBACK_URI)
                .queryParam("code", code)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    public static String failureRedirect(String state) {
        return UriComponentsBuilder.fromUriString(CALLBACK_URI)
                .queryParam("error", "oauth_failed")
                .queryParam("state", state)
                .build()
                .toUriString();
    }
}
