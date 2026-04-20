package edu.ics499.VBeta.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * {@code FirebaseConfig} initializes Firebase SDK integration for non-test environments.
 * <p>
 * On application startup, this configuration loads service credentials and initializes
 * a singleton {@link FirebaseApp} when no app instance is already registered.
 */
@Configuration
@Profile("!test")
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    /**
     * Initializes Firebase application state after bean construction.
     *
     * @throws IOException when the Firebase credential file cannot be read
     */
    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()){
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                .build();
            FirebaseApp.initializeApp(options);
        }
    }
}