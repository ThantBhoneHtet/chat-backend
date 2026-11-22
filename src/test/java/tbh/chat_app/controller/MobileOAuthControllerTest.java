package tbh.chat_app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tbh.chat_app.config.oauth.MobileOAuthSession;
import tbh.chat_app.dto.user.JwtResponse;
import tbh.chat_app.dto.user.MobileOAuthCodeExchangeRequest;
import tbh.chat_app.service.MobileOAuthCodeService;

class MobileOAuthControllerTest {

    private static final String PKCE_VALUE = "a".repeat(43);

    private final MobileOAuthCodeService codeService = mock(MobileOAuthCodeService.class);
    private final MobileOAuthController controller = new MobileOAuthController(codeService);

    @Test
    void startStoresPkceValuesAndRedirectsToSpringOAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.start("google", PKCE_VALUE, PKCE_VALUE, request, response);

        assertEquals("/oauth2/authorization/google", response.getRedirectedUrl());
        assertEquals(
                PKCE_VALUE,
                request.getSession(false).getAttribute(MobileOAuthSession.CODE_CHALLENGE_ATTRIBUTE));
        assertEquals(
                PKCE_VALUE,
                request.getSession(false).getAttribute(MobileOAuthSession.STATE_ATTRIBUTE));
    }

    @Test
    void startRejectsUnknownProvider() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.start("unknown", PKCE_VALUE, PKCE_VALUE, new MockHttpServletRequest(), response);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    void exchangeReturnsJwtOnlyForConsumedCode() {
        String code = "c".repeat(43);
        String verifier = "v".repeat(43);
        when(codeService.consume(code, verifier)).thenReturn("issued-jwt");

        var response = controller.exchange(new MobileOAuthCodeExchangeRequest(code, verifier));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("issued-jwt", ((JwtResponse) response.getBody()).token());
    }

    @Test
    void exchangeRejectsInvalidOrExpiredCode() {
        String code = "c".repeat(43);
        String verifier = "v".repeat(43);
        when(codeService.consume(code, verifier)).thenReturn(null);

        var response = controller.exchange(new MobileOAuthCodeExchangeRequest(code, verifier));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
