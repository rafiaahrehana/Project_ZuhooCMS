package com.businessos.modules.servicedesk.requeststatus;

import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.enums.ServiceRequestStatus;
import com.businessos.auth.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Enumerated(EnumType.STRING)
    private ServiceRequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceRequestStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // Denormalised for fast audit queries
    private Long companyId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}
