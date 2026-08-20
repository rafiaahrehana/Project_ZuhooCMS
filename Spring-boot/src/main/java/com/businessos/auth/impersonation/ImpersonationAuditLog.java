package com.businessos.auth.impersonation;

import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Not a BaseEntity - like AuditLog, this is an append-only compliance record and must
// never be soft-deletable or hidden by the standard @SQLRestriction("deleted = false").
@Entity
@Table(name = "impersonation_audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImpersonationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "impersonation_session_id", nullable = false, unique = true)
    private String impersonationSessionId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
