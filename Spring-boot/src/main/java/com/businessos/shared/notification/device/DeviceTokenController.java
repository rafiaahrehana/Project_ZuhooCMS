package com.businessos.shared.notification.device;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/device-tokens")
@RequiredArgsConstructor
@Tag(name = "Device Tokens", description = "Push notification device registration")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register this device for push notifications (upserts by token)")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        deviceTokenService.register(request);
        return ResponseEntity.ok("Device registered");
    }

    @DeleteMapping("/{token}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unregister a device token (call on sign-out)")
    public ResponseEntity<String> unregister(@PathVariable String token) {
        deviceTokenService.unregister(token);
        return ResponseEntity.ok("Device unregistered");
    }
}
