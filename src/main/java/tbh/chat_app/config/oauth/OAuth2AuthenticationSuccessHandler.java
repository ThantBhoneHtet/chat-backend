package tbh.chat_app.config.oauth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tbh.chat_app.config.jwt.JwtUtils;
import tbh.chat_app.config.jwt.UserPrincipal;
import tbh.chat_app.service.MobileOAuthCodeService;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final MobileOAuthCodeService mobileOAuthCodeService;

    public OAuth2AuthenticationSuccessHandler(
            JwtUtils jwtUtils,
            MobileOAuthCodeService mobileOAuthCodeService) {
        this.jwtUtils = jwtUtils;
        this.mobileOAuthCodeService = mobileOAuthCodeService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            Object principal = oauthToken.getPrincipal();
            
            String jwtToken = null;
            
            if (principal instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) principal;
                jwtToken = jwtUtils.generateTokenFromUsername(userPrincipal);
            } else if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                String email = oauth2User.getAttribute("email");
                if (email != null) {
                    jwtToken = jwtUtils.generateTokenFromOAuth2User(oauth2User);
                }
            }
            
            if (jwtToken != null) {
                String redirectUrl = buildMobileRedirectUrl(request, jwtToken);
                if (redirectUrl == null) {
                    redirectUrl = buildRedirectUrl(jwtToken);
                }
                response.sendRedirect(redirectUrl);
            } else {
                String redirectUrl = buildMobileFailureRedirect(request);
                response.sendRedirect(
                        redirectUrl == null ? "/api/auth/oauth2/failure" : redirectUrl);
            }
        }
    }
    
    private String buildRedirectUrl(String jwtToken) {
        return UriComponentsBuilder.fromUriString("https://chat-with-me-123.lovable.app/oauth2/success")
            .queryParam("token", jwtToken)
            .queryParam("message", "OAuth2 login successful")
            .build().toUriString();
    }

    private String buildMobileRedirectUrl(HttpServletRequest request, String jwtToken) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object challenge = session.getAttribute(MobileOAuthSession.CODE_CHALLENGE_ATTRIBUTE);
        Object state = session.getAttribute(MobileOAuthSession.STATE_ATTRIBUTE);
        if (!(challenge instanceof String codeChallenge) || !(state instanceof String oauthState)) {
            return null;
        }

        session.removeAttribute(MobileOAuthSession.CODE_CHALLENGE_ATTRIBUTE);
        session.removeAttribute(MobileOAuthSession.STATE_ATTRIBUTE);
        String code = mobileOAuthCodeService.issue(jwtToken, codeChallenge);
        return MobileOAuthSession.successRedirect(code, oauthState);
    }

    private String buildMobileFailureRedirect(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object state = session.getAttribute(MobileOAuthSession.STATE_ATTRIBUTE);
        if (!(state instanceof String oauthState)) {
            return null;
        }

        session.removeAttribute(MobileOAuthSession.CODE_CHALLENGE_ATTRIBUTE);
        session.removeAttribute(MobileOAuthSession.STATE_ATTRIBUTE);
        return MobileOAuthSession.failureRedirect(oauthState);
    }
}
