package tbh.chat_app.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import tbh.chat_app.config.oauth.MobileOAuthSession;
import tbh.chat_app.dto.user.JwtResponse;
import tbh.chat_app.dto.user.MobileOAuthCodeExchangeRequest;
import tbh.chat_app.service.MobileOAuthCodeService;

@RestController
@RequestMapping("/api/auth/mobile/oauth2")
public class MobileOAuthController {

    private static final Set<String> PROVIDERS = Set.of("google", "github");
    private static final Pattern PKCE_VALUE = Pattern.compile("[A-Za-z0-9_-]{43,128}");

    private final MobileOAuthCodeService codeService;

    public MobileOAuthController(MobileOAuthCodeService codeService) {
        this.codeService = codeService;
    }

    @GetMapping("/start/{provider}")
    public void start(
            @PathVariable String provider,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!PROVIDERS.contains(provider)
                || !isValidPkceValue(codeChallenge)
                || !isValidPkceValue(state)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid mobile OAuth request.");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(MobileOAuthSession.CODE_CHALLENGE_ATTRIBUTE, codeChallenge);
        session.setAttribute(MobileOAuthSession.STATE_ATTRIBUTE, state);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(
            @Valid @RequestBody MobileOAuthCodeExchangeRequest request) {
        String token = codeService.consume(request.code(), request.codeVerifier());
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "The OAuth code is invalid or expired.", "status", false));
        }
        return ResponseEntity.ok(new JwtResponse(token, "OAuth login successful."));
    }

    private boolean isValidPkceValue(String value) {
        return PKCE_VALUE.matcher(value).matches();
    }
}
