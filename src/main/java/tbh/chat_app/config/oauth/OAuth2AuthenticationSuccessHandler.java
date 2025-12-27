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
import tbh.chat_app.config.jwt.JwtUtils;
import tbh.chat_app.config.jwt.UserPrincipal;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;

     public OAuth2AuthenticationSuccessHandler(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {

        System.out.println("=== OAuth2 SUCCESS HANDLER STARTED ===");
        
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
                     System.out.println("Generated JWT token: " + jwtToken);
                }
            }
            
            if (jwtToken != null) {
                // Redirect to frontend with token or return JSON response
                String redirectUrl = buildRedirectUrl(jwtToken);
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect("/api/auth/oauth2/failure");
            }
        }
    }
    
    private String buildRedirectUrl(String jwtToken) {
        return UriComponentsBuilder.fromUriString("https://chat-with-me-123.lovable.app/oauth2/success")
            .queryParam("token", jwtToken)
            .queryParam("message", "OAuth2 login successful")
            .build().toUriString();
    }
}
