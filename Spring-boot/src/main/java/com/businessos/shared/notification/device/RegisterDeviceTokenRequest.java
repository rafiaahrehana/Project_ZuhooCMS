package com.businessos.shared.notification.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Note there is deliberately no userId field — the owner is always taken from the JWT. Accepting
 * one from the client would let any authenticated caller register a device against someone else's
 * account and receive their notifications (the same class of hole that was closed in the support
 * module's sentByUserId).
 */
@Getter
@Setter
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    @Size(max = 512, message = "Device token is too long")
    private String token;

    @NotNull(message = "Platform is required")
    private DevicePlatform platform;
}
