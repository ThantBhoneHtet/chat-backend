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
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@EnableReactiveFirestoreRepositories(basePackages = "tbh.chat_app.repository")
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private Resource serviceAccount;

    @Value("${firebase.project.id}")
    private String projectId;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Verify credentials are loaded properly
        InputStream serviceAccountStream = serviceAccount.getInputStream();
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
        
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(projectId)  // Explicitly set project ID
            .build();

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore() throws IOException {
        // Initialize with explicit project ID
        FirestoreOptions options = FirestoreOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
            .setProjectId(projectId)
            .build();
        return options.getService();
    }
}