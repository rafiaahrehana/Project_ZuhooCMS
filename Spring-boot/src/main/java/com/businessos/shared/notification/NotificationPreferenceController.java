package com.businessos.shared.notification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<NotificationPreferenceResponse> get() {
        return ResponseEntity.ok(preferenceService.getForCurrentUser());
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceResponse> update(
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.update(request));
    }

    @DeleteMapping
    public ResponseEntity<NotificationPreferenceResponse> reset() {
        return ResponseEntity.ok(preferenceService.resetToDefaults());
    }
}
