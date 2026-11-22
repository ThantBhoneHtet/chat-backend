package tbh.chat_app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

@Service
public class MobileOAuthCodeService {

    private static final String COLLECTION = "mobile_oauth_codes";
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(2);

    private final Firestore firestore;
    private final SecureRandom secureRandom = new SecureRandom();

    public MobileOAuthCodeService(Firestore firestore) {
        this.firestore = firestore;
    }

    public String issue(String jwt, String codeChallenge) {
        byte[] randomCode = new byte[32];
        secureRandom.nextBytes(randomCode);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomCode);

        Map<String, Object> record = Map.of(
                "token", jwt,
                "codeChallenge", codeChallenge,
                "expiresAt", Date.from(java.time.Instant.now().plus(CODE_LIFETIME)));
        DocumentReference document = firestore.collection(COLLECTION).document(code);
        await(document.create(record), "Could not save mobile OAuth code.");
        return code;
    }

    public String consume(String code, String codeVerifier) {
        DocumentReference document = firestore.collection(COLLECTION).document(code);
        ApiFuture<String> transaction = firestore.runTransaction(tx -> {
            DocumentSnapshot snapshot = tx.get(document).get();
            if (!snapshot.exists()) {
                return null;
            }

            String token = snapshot.getString("token");
            String expectedChallenge = snapshot.getString("codeChallenge");
            Date expiresAt = snapshot.getDate("expiresAt");
            tx.delete(document);

            boolean valid = token != null
                    && expectedChallenge != null
                    && expiresAt != null
                    && expiresAt.after(new Date())
                    && MessageDigest.isEqual(
                            expectedChallenge.getBytes(StandardCharsets.US_ASCII),
                            challengeFor(codeVerifier).getBytes(StandardCharsets.US_ASCII));
            return valid ? token : null;
        });
        return await(transaction, "Could not exchange mobile OAuth code.");
    }

    private String challengeFor(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private <T> T await(ApiFuture<T> future, String failureMessage) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failureMessage, exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException(failureMessage, exception.getCause());
        }
    }
}
