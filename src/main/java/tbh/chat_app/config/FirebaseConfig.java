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

@Configuration
@EnableReactiveFirestoreRepositories(basePackages = "tbh.chat_app.repository")
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Value("${firebase.project.id}")
    private String projectId;

    private InputStream getCredentialsStream() throws IOException {
        // 1. Try to get JSON from FIREBASE_CREDENTIALS_JSON environment variable
        String jsonCredentials = System.getenv("FIREBASE_CREDENTIALS_JSON");
        
        if (jsonCredentials != null && !jsonCredentials.trim().isEmpty()) {
            System.out.println("✓ Loading Firebase credentials from FIREBASE_CREDENTIALS_JSON");
            return new ByteArrayInputStream(jsonCredentials.getBytes(StandardCharsets.UTF_8));
        }
        
        // 2. Fallback to file (local development)
        System.out.println("ℹ FIREBASE_CREDENTIALS_JSON not found, loading from file: " + credentialsPath);
        
        if (credentialsPath.startsWith("classpath:")) {
            String resourcePath = credentialsPath.replace("classpath:", "");
            InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new RuntimeException("Firebase credentials not found in classpath: " + resourcePath);
            }
            return stream;
        } else {
            File file = new File(credentialsPath);
            if (!file.exists()) {
                throw new RuntimeException("Firebase credentials file not found: " + credentialsPath);
            }
            return new FileInputStream(file);
        }
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