package com.businessos.shared.email;

import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String template;

    @Column(nullable = false)
    private String status;

    private String failureReason;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    private LocalDateTime sentTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company; // Nullable, as platform emails might not be linked to a company directly if it's a generic email, or they might be.
}
