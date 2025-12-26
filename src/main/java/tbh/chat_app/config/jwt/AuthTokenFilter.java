package tbh.chat_app.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Enumeration;

// JWT Filter
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());

        try {
            String jwt = parseJwt(request);
            System.out.println("Jwt token from AuthTokenFilter.java : " + jwt);
            if (jwt != null) {
                try {
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String username = jwtUtils.getUserNameFromJwtToken(jwt);
                        System.out.println("Username: " + username);

                        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                        System.out.println("UserDetails name:" + userDetails.getUsername());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails,
                                        null,
                                        userDetails.getAuthorities());

                        logger.debug("Roles from JWT: {}", userDetails.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        System.out.println("Authentication : " + authentication.getDetails().toString());

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        System.out.println("SecurityContext authentication: " + SecurityContextHolder.getContext().getAuthentication());
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                    logger.warn("JWT token expired: {}", ex.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"JWT token expired\"}");
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        logger.debug("AuthTokenFilter.java: {}", jwt);
        return jwt;
    }
}