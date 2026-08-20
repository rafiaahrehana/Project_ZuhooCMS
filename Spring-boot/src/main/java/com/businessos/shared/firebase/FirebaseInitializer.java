package com.businessos.shared.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Single place where the Firebase Admin SDK is started.
 *
 * Two features now need it — push notifications (FcmPushService) and Google sign-in
 * (GoogleAuthService). Both used to be able to race to initialise it; making them depend on this
 * bean instead means Spring guarantees the ordering, and there is one answer to "is Firebase
 * configured?" rather than two.
 *
 * Firebase stays optional: with no service-account path the app starts normally, push is skipped
 * and Google sign-in returns a clear error instead of failing at startup. A developer without
 * credentials must still be able to run everything else.
 */
@Component
@Slf4j
public class FirebaseInitializer {

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    private boolean available;

    @PostConstruct
    void init() {

        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("Firebase disabled — set fcm.credentials-path (or FIREBASE_CREDENTIALS_PATH) "
                    + "to a service-account JSON to enable push notifications and Google sign-in.");
            return;
        }

        try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build());
            }

            available = true;
            log.info("Firebase enabled — push notifications and Google sign-in are available");

        } catch (Exception e) {
            // A bad path must not stop the application from starting.
            log.warn("Firebase disabled — could not initialise from {}: {}",
                    credentialsPath, e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
