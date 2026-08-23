package com.zeylex.denguesense.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

@Component
public class FirebaseMessagingHolder {

    private static final Logger log = LoggerFactory.getLogger(FirebaseMessagingHolder.class);

    private final FirebaseMessaging firebaseMessaging;

    public FirebaseMessagingHolder(ResourceLoader resourceLoader,
                                   @Value("${firebase.credentials-file:}") String credentialsFile) {
        this.firebaseMessaging = initialize(resourceLoader, credentialsFile);
    }

    public Optional<FirebaseMessaging> get() {
        return Optional.ofNullable(firebaseMessaging);
    }

    private static FirebaseMessaging initialize(ResourceLoader resourceLoader, String credentialsFile) {
        if (credentialsFile == null || credentialsFile.isBlank()) {
            log.warn("firebase.credentials-file is not set; FCM pushes will be recorded as FAILED until configured");
            return null;
        }
        try {
            Resource resource = resourceLoader.getResource(credentialsFile);
            if (!resource.exists()) {
                log.warn("Firebase credentials file not found at {}; FCM pushes will be recorded as FAILED", credentialsFile);
                return null;
            }
            try (InputStream stream = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp app = FirebaseApp.getApps().isEmpty()
                        ? FirebaseApp.initializeApp(options)
                        : FirebaseApp.getInstance();
                log.info("Firebase Admin SDK initialized");
                return FirebaseMessaging.getInstance(app);
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize Firebase Admin SDK: {}", ex.getMessage());
            return null;
        }
    }
}
