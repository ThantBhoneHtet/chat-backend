package tbh.chat_app.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@EnableReactiveFirestoreRepositories(basePackages = "tbh.chat_app.repository")
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Value("${firebase.project.id}")
    private String projectId;

    private InputStream getCredentialsStream() throws IOException {

        // 1. Railway / Production: Base64 credentials
        String base64Credentials = System.getenv("FIREBASE_CREDENTIALS_BASE64");

        if (base64Credentials != null && !base64Credentials.isBlank()) {
            System.out.println("✓ Loading Firebase credentials from FIREBASE_CREDENTIALS_BASE64");
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            return new ByteArrayInputStream(decoded);
        }

        // 2. Local fallback
        System.out.println("ℹ Loading Firebase credentials from: " + credentialsPath);

        if (credentialsPath.startsWith("classpath:")) {
            String resourcePath = credentialsPath.substring("classpath:".length());
            InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IllegalStateException(
                    "Firebase credentials not found in classpath: " + resourcePath
                );
            }
            return stream;
        }

        File file = new File(credentialsPath);
        if (!file.exists()) {
            throw new IllegalStateException(
                "Firebase credentials file not found: " + credentialsPath
            );
        }
        return new FileInputStream(file);
    }


    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream serviceAccountStream = getCredentialsStream();
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
        
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build();

        if (FirebaseApp.getApps().isEmpty()) {
            System.out.println("✓ Firebase initialized successfully with project: " + projectId);
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore() throws IOException {
        // Create a new stream (Firestore needs its own)
        InputStream serviceAccountStream = getCredentialsStream();
        FirestoreOptions options = FirestoreOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
            .setProjectId(projectId)
            .build();
        return options.getService();
    }
}

// package tbh.chat_app.config;

// import com.google.auth.oauth2.GoogleCredentials;
// import com.google.cloud.firestore.Firestore;
// import com.google.cloud.firestore.FirestoreOptions;
// import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
// import com.google.firebase.FirebaseApp;
// import com.google.firebase.FirebaseOptions;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.io.Resource;

// import java.io.IOException;
// import java.io.InputStream;

// @Configuration
// @EnableReactiveFirestoreRepositories(basePackages = "tbh.chat_app.repository")
// public class FirebaseConfig {

//     @Value("${firebase.credentials.path}")
//     private Resource serviceAccount;

//     @Value("${firebase.project.id}")
//     private String projectId;

//     @Bean
//     public FirebaseApp firebaseApp() throws IOException {
//         // Verify credentials are loaded properly
//         InputStream serviceAccountStream = serviceAccount.getInputStream();
//         GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
        
//         FirebaseOptions options = FirebaseOptions.builder()
//             .setCredentials(credentials)
//             .setProjectId(projectId)  // Explicitly set project ID
//             .build();

//         if (FirebaseApp.getApps().isEmpty()) {
//             return FirebaseApp.initializeApp(options);
//         }
//         return FirebaseApp.getInstance();
//     }

//     @Bean
//     public Firestore firestore() throws IOException {
//         // Initialize with explicit project ID
//         FirestoreOptions options = FirestoreOptions.newBuilder()
//             .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
//             .setProjectId(projectId)
//             .build();
//         return options.getService();
//     }
// }