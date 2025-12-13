package com.kobe.warehouse.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase configuration for push notifications.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String FIREBASE_CONFIG_PATH = "firebase-service-account.json";

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(FIREBASE_CONFIG_PATH);

            if (!resource.exists()) {
                log.error("Firebase service account file not found: {}", FIREBASE_CONFIG_PATH);
                log.error("Please place the Firebase service account JSON file in src/main/resources/");
                throw new IOException("Firebase service account file not found");
            }

            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            FirebaseApp app;
            if (FirebaseApp.getApps().isEmpty()) {
                app = FirebaseApp.initializeApp(options);
                log.info("Firebase app initialized successfully");
            } else {
                app = FirebaseApp.getInstance();
                log.info("Firebase app already initialized, using existing instance");
            }

            return app;
        } catch (IOException e) {
            log.error("Failed to initialize Firebase app", e);
            throw e;
        }
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
        log.info("FirebaseMessaging bean created successfully");
        return messaging;
    }
}
