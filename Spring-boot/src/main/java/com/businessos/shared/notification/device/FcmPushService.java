package com.businessos.shared.notification.device;

import com.businessos.shared.firebase.FirebaseInitializer;
import com.businessos.shared.notification.Notification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends a notification to a user's registered devices.
 *
 * Firebase is optional. If no service-account file is configured the service logs once at
 * startup and every send becomes a no-op — the app still works, it just falls back to the
 * existing in-app STOMP push and the notification list. That matters because a developer
 * checkout without Firebase credentials must still be able to run the whole backend.
 *
 * Nothing here is allowed to fail a caller: push is a best-effort side channel, and the
 * Notification row is already persisted by the time this runs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {

    private final DeviceTokenService deviceTokenService;

    // Injected rather than initialised here: Google sign-in needs Firebase too, so startup is
    // owned by FirebaseInitializer and this just asks whether it succeeded.
    private final FirebaseInitializer firebase;

    private boolean enabled() {
        return firebase.isAvailable();
    }

    public void push(Notification notification, Long userId) {

        if (!enabled()) {
            return;
        }

        try {
            List<DeviceToken> devices = deviceTokenService.tokensFor(userId);

            if (devices.isEmpty()) {
                return;
            }

            List<String> dead = new ArrayList<>();

            for (DeviceToken device : devices) {
                if (!send(device, notification)) {
                    dead.add(device.getToken());
                }
            }

            if (!dead.isEmpty()) {
                deviceTokenService.prune(dead);
            }

        } catch (Exception e) {
            log.debug("FCM push failed for user {}: {}", userId, e.getMessage());
        }
    }

    /** @return false when the token is dead and should be pruned. */
    private boolean send(DeviceToken device, Notification notification) {

        // Data-only message, no notification payload: it means the Android client builds the
        // system notification itself, so it can localise the type label and attach the deep link
        // — and it gets the same treatment whether the app is foregrounded or not.
        Message message = Message.builder()
                .setToken(device.getToken())
                .putData("type", notification.getType() != null ? notification.getType().name() : "GENERAL")
                .putData("title", nullSafe(notification.getTitle()))
                .putData("body", nullSafe(notification.getMessage()))
                .putData("actionUrl", nullSafe(notification.getActionUrl()))
                .putData("notificationId", notification.getId() != null
                        ? String.valueOf(notification.getId()) : "")
                .putData("serviceRequestId", notification.getServiceRequest() != null
                        ? String.valueOf(notification.getServiceRequest().getId()) : "")
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
            return true;

        } catch (FirebaseMessagingException e) {

            MessagingErrorCode code = e.getMessagingErrorCode();

            // The app was uninstalled, or the token was reissued — the row is now junk.
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                return false;
            }

            log.debug("FCM send failed ({}): {}", code, e.getMessage());
            return true;

        } catch (Exception e) {
            log.debug("FCM send failed: {}", e.getMessage());
            return true;
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
