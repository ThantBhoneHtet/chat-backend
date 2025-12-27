package tbh.chat_app.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FirebaseEnvProcessor implements EnvironmentPostProcessor {
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, 
                                     SpringApplication application) {
        
        // Check if FIREBASE_CREDENTIALS_JSON is set
        String json = System.getenv("FIREBASE_CREDENTIALS_JSON");
        if (json != null && !json.trim().isEmpty()) {
            try {
                System.out.println("🚀 FirebaseEnvProcessor: Creating credentials file...");
                
                // Create directory
                Path configDir = Paths.get("/app/config");
                Files.createDirectories(configDir);
                
                // Create file
                Path configFile = configDir.resolve("firebase-key.json");
                String processedJson = json.replace("\\n", "\n");
                Files.writeString(configFile, processedJson);
                
                System.out.println("✅ Created Firebase config at: " + configFile);
                
                // Set the property for Spring Cloud GCP
                Map<String, Object> props = new HashMap<>();
                props.put("firebase.credentials.path", configFile.toString());
                props.put("spring.cloud.gcp.credentials.location", "file:" + configFile.toString());
                props.put("spring.cloud.gcp.firestore.credentials.location", "file:" + configFile.toString());
                
                environment.getPropertySources().addFirst(
                    new MapPropertySource("firebaseProperties", props)
                );
                
            } catch (IOException e) {
                System.err.println("❌ Failed to create Firebase config: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ FirebaseEnvProcessor: No FIREBASE_CREDENTIALS_JSON, using defaults");
        }
    }
}