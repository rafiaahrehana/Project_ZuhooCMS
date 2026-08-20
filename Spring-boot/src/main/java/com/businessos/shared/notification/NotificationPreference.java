package com.businessos.shared.notification;

import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Email preferences
    @Builder.Default private boolean emailOnServiceRequest  = true;
    @Builder.Default private boolean emailOnStatusChange    = true;
    @Builder.Default private boolean emailOnInvoice         = true;
    @Builder.Default private boolean emailOnPayment         = true;
    @Builder.Default private boolean emailOnTaskAssigned    = true;
    @Builder.Default private boolean emailOnLeaveUpdate     = true;

    // In-app preferences
    @Builder.Default private boolean inAppOnServiceRequest  = true;
    @Builder.Default private boolean inAppOnStatusChange    = true;

    @Builder.Default private boolean emailMarketing         = false;
}
