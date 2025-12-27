package tbh.chat_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatAppApplication {

// Static block runs BEFORE Spring starts
    static {
        createFirebaseConfigFile();
    }
    
    private static void createFirebaseConfigFile() {
        String json = System.getenv("FIREBASE_CREDENTIALS_JSON");
        if (json != null && !json.trim().isEmpty()) {
            try {
                System.out.println("=== CREATING FIREBASE CONFIG FILE (BEFORE SPRING) ===");
                
                // Create directory
                java.nio.file.Path configDir = java.nio.file.Paths.get("/app/config");
                java.nio.file.Files.createDirectories(configDir);
                
                // Create file (replace \n with actual newlines)
                java.nio.file.Path configFile = configDir.resolve("firebase-key.json");
                String processedJson = json.replace("\\n", "\n");
                java.nio.file.Files.writeString(configFile, processedJson);
                
                System.out.println("✅ File created: " + configFile);
                System.out.println("✅ File size: " + java.nio.file.Files.size(configFile) + " bytes");
                
                // Set system property with file: prefix
                System.setProperty("spring.cloud.gcp.credentials.location", "file:" + configFile);
                
            } catch (Exception e) {
                System.err.println("❌ Failed to create Firebase config: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠ FIREBASE_CREDENTIALS_JSON not set - will use classpath/default");
        }
    }
    
    public static void main(String[] args) {
        // Debug: Check if file exists
        java.io.File file = new java.io.File("/app/config/firebase-key.json");
        System.out.println("Before Spring starts - File exists: " + file.exists());
        System.out.println("Before Spring starts - File size: " + (file.exists() ? file.length() : 0));
        
        SpringApplication.run(ChatAppApplication.class, args);
    }

}
