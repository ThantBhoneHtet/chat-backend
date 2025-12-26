package tbh.chat_app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.util.UriComponentsBuilder;

import tbh.chat_app.config.jwt.AuthEntryPointJwt;
import tbh.chat_app.config.jwt.AuthTokenFilter;
import tbh.chat_app.config.jwt.JwtUtils;
import tbh.chat_app.config.oauth.CustomOidcUserService;
import tbh.chat_app.config.oauth.OAuth2AuthenticationSuccessHandler;
import tbh.chat_app.config.oauth.OAuth2UserService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private OAuth2UserService oAuth2UserService;

    @Autowired
    private CustomOidcUserService customOidcUserService;

    @Autowired 
    private JwtUtils jwtUtils;

    @Bean
    public AuthTokenFilter jwtFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(request -> 
                request.requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**", "/chat/**").permitAll()
                        .anyRequest().authenticated()
            )
            // .formLogin(form -> form.defaultSuccessUrl("/hello", true));
            // .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("http://localhost:8081/chat", true));
            .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
                    userInfo -> userInfo
                            .userService(oAuth2UserService)
                            .oidcUserService(customOidcUserService)
                )
                .successHandler(oauth2AuthenticationSuccessHandler())
                .failureHandler(oauth2AuthenticationFailureHandler())
            );


        http.sessionManagement(
                session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS)
        );

        http.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler));
        //http.httpBasic(withDefaults());
        http.headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions
                        .sameOrigin()
                )
        );

        http.addFilterBefore(jwtFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    } 

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration builder) throws Exception {
        return builder.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // OAuth2 handler methods
    private AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler(jwtUtils);
    }

    private AuthenticationFailureHandler oauth2AuthenticationFailureHandler() {
        return (request, response, exception) -> {
            System.err.println("OAuth2 FAILURE: " + exception.getMessage());
            response.sendRedirect("/api/auth/oauth2/failure");
        };
    }
}
