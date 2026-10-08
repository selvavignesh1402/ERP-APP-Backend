package com.riceerp.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private final boolean enabled;
    private final String credentialsPath;
    private final String projectId;

    public FirebaseConfig(@Value("${firebase.enabled:false}") boolean enabled,
                          @Value("${firebase.credentials-path:${FIREBASE_CREDENTIALS_PATH:}}") String credentialsPath,
                          @Value("${firebase.project-id:${FIREBASE_PROJECT_ID:}}") String projectId) {
        this.enabled = enabled;
        this.credentialsPath = credentialsPath;
        this.projectId = projectId;
    }

    GoogleCredentials credentials() throws IOException {
        if (credentialsPath.isBlank()) return GoogleCredentials.getApplicationDefault();
        try (InputStream stream = Files.newInputStream(Path.of(credentialsPath))) {
            return GoogleCredentials.fromStream(stream);
        }
    }

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("Firebase Admin is disabled. Set FIREBASE_ENABLED=true and configure external credentials to enable it.");
            return;
        }
        try {
            if (FirebaseApp.getApps().stream().noneMatch(app -> FirebaseApp.DEFAULT_APP_NAME.equals(app.getName()))) {
                FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials());
                if (!projectId.isBlank()) options.setProjectId(projectId.trim());
                FirebaseApp.initializeApp(options.build());
                log.info("Firebase Admin SDK initialized successfully.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Firebase is enabled but could not initialize. Configure FIREBASE_CREDENTIALS_PATH "
                    + "or Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS), and FIREBASE_PROJECT_ID if needed.", e);
        }
    }
}
