package com.businessos.shared.notification.device;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A push target: one row per app install that has registered for notifications.
 *
 * The token is unique because FCM can hand the same token to a different account on the same
 * device after a reinstall or account switch — re-registering must move the row to the new user
 * rather than leave the old one pointing at a device that no longer belongs to them.
 *
 * companyId is denormalised the same way Notification does it, so a tenant's tokens can be
 * scoped without joining through the user.
 */
@Entity
@Table(name = "device_tokens",
       uniqueConstraints = @UniqueConstraint(name = "uk_device_tokens_token", columnNames = "token"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceToken extends BaseEntity {

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DevicePlatform platform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_id")
    private Long companyId;

    /** Refreshed on every re-registration, so stale installs can be pruned later. */
    private LocalDateTime lastSeenAt;
}
