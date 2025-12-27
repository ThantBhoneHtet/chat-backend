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
import java.io.*;
import java.nio.charset.StandardCharsets;


@Configuration
@EnableReactiveFirestoreRepositories(basePackages = "tbh.chat_app.repository")
public class FirebaseConfig {

    @Value("${firebase.credentials.path:classpath:chat-app-service-acc-key.json}")
    private String credentialsPath;

    @Value("${firebase.project.id:chat-app-817da}")
    private String projectId;

    private InputStream getCredentialsStream() throws IOException {
        System.out.println("=== Loading Firebase Credentials ===");
        
        // Priority 1: Check if file exists at FIREBASE_CREDENTIALS_FILE path
        String filePath = System.getenv("FIREBASE_CREDENTIALS_FILE");
        if (filePath != null && !filePath.trim().isEmpty()) {
            System.out.println("✓ Looking for credentials file at: " + filePath);
            File file = new File(filePath);
            if (file.exists() && file.length() > 0) {
                System.out.println("✓ Found credentials file: " + filePath + " (" + file.length() + " bytes)");
                return new FileInputStream(file);
            } else {
                System.out.println("✗ File not found or empty: " + filePath);
            }
        }
        
        // Priority 2: Check FIREBASE_CREDENTIALS_JSON environment variable
        String jsonEnv = System.getenv("FIREBASE_CREDENTIALS_JSON");
        if (jsonEnv != null && !jsonEnv.trim().isEmpty()) {
            System.out.println("✓ Using FIREBASE_CREDENTIALS_JSON environment variable");
            System.out.println("  JSON length: " + jsonEnv.length() + " chars");
            
            // Check if this looks like a file path instead of JSON
            if (jsonEnv.startsWith("{") && jsonEnv.contains("\"type\":\"service_account\"")) {
                // It's JSON - unescape \n in private_key
                String processedJson = jsonEnv.replace("\\n", "\n");
                return new ByteArrayInputStream(processedJson.getBytes(StandardCharsets.UTF_8));
            } else if (new File(jsonEnv).exists()) {
                // It's a file path
                System.out.println("  Treating as file path: " + jsonEnv);
                return new FileInputStream(jsonEnv);
            }
        }
        
        // Priority 3: Classpath (local development only)
        System.out.println("ℹ Falling back to classpath: " + credentialsPath);
        
        if (credentialsPath.startsWith("classpath:")) {
            String resourcePath = credentialsPath.replace("classpath:", "");
            InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(resourcePath);
            
            if (stream != null) {
                System.out.println("✓ Found in classpath: " + resourcePath);
                return stream;
            }
        }
        
        // Final fallback: try direct file
        File file = new File(credentialsPath);
        if (file.exists()) {
            System.out.println("✓ Found file: " + credentialsPath);
            return new FileInputStream(file);
        }
        
        throw new RuntimeException(
            "Firebase credentials not found.\n" +
            "Options:\n" +
            "1. Set FIREBASE_CREDENTIALS_JSON environment variable with JSON content\n" +
            "2. Set FIREBASE_CREDENTIALS_FILE environment variable with file path\n" +
            "3. Place chat-app-service-acc-key.json in src/main/resources/ for local dev"
        );
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
            System.out.println("✓ Firebase initialized successfully for project: " + projectId);
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore() throws IOException {
        // Note: getCredentialsStream() caches the bytes, so we can call it again
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