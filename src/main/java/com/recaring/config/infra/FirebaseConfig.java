package com.recaring.config.infra;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;
    private static final int WRITE_TIMEOUT_MILLIS = 5_000;

    @Value("${firebase.service-account-json-base64}")
    private String serviceAccountJsonBase64;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (serviceAccountJsonBase64 == null || serviceAccountJsonBase64.isBlank()) {
            throw new IllegalStateException("firebase.service-account-json-base64 is required when firebase.enabled=true");
        }

        byte[] decoded = decodeServiceAccount();
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
                .setReadTimeout(READ_TIMEOUT_MILLIS)
                .setWriteTimeout(WRITE_TIMEOUT_MILLIS)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private byte[] decodeServiceAccount() {
        try {
            return Base64.getDecoder().decode(serviceAccountJsonBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "firebase.service-account-json-base64 must be valid Base64 encoded JSON.",
                    exception
            );
        }
    }
}
