package com.businessos.modules.dashboard;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * One row per calendar day - captured by PlatformMetricsScheduler so the platform
 * dashboard's KPI sparklines (Total/Active/Trial/Suspended companies) have real
 * history to plot instead of a single point. Upserted (see repository) so re-running
 * the snapshot for today never creates a duplicate row.
 */
@Entity
@Table(name = "platform_metrics_snapshots",
       indexes = @Index(name = "idx_pms_snapshot_date", columnList = "snapshotDate", unique = true))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PlatformMetricsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate snapshotDate;

    private long totalCompanies;
    private long activeCompanies;
    private long trialCompanies;
    private long suspendedCompanies;
    private long pendingVerificationCompanies;
}
