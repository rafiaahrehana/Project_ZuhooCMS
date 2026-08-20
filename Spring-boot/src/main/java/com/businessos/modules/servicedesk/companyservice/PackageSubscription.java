package com.businessos.modules.servicedesk.companyservice;

import com.businessos.modules.crm.client.Client;
import com.businessos.modules.company.Company;
import com.businessos.enums.BillingCycle;
import com.businessos.enums.SubscriptionStatus;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PackageSubscription represents a single client's active subscription
 * to a ServicePackage for a specific billing period.
 *
 * One client can have multiple historical subscriptions to the same package
 * (previous cycles), but only ONE may be ACTIVE at a time per package.
 * This is enforced in ServicePackageServiceImpl.subscribe().
 *
 * requestsUsed is incremented every time the client raises a ServiceRequest
 * under this subscription (via ServiceRequestServiceImpl.create()).
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(
    name = "package_subscriptions",
    indexes = {
        @Index(name = "idx_sub_client_package", columnList = "client_id, package_id"),
        @Index(name = "idx_sub_status",         columnList = "status"),
        @Index(name = "idx_sub_end_date",        columnList = "end_date")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PackageSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private ServicePackage servicePackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    @Column(nullable = false)
    private LocalDate startDate;

    /** Calculated from startDate + billingCycle. NULL for indefinite ONE_TIME packages. */
    private LocalDate endDate;

    /** Date the next billing charge is due. */
    private LocalDate nextBillingDate;

    /**
     * Price locked at the time of subscription — immune to future package price changes.
     */
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePaid;

    /**
     * Request quota copied from package at subscription time.
     * NULL = unlimited.
     */
    private Integer requestQuota;

    /**
     * How many service requests have been raised under this subscription
     * in the current billing period.
     */
    @Builder.Default
    @Column(nullable = false)
    private int requestsUsed = 0;

    /** Whether this subscription renews automatically at period end. */
    @Builder.Default
    private boolean autoRenew = true;

    /** Timestamp when the subscription was activated (payment confirmed). */
    private LocalDateTime activatedAt;

    /** Timestamp when the subscription was cancelled or suspended. */
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    // ── Computed helpers ──────────────────────────────────────────

    /** True if this subscription is currently usable. */
    public boolean isUsable() {
        return status == SubscriptionStatus.ACTIVE
            && (endDate == null || !LocalDate.now().isAfter(endDate));
    }

    /**
     * True if the client can still raise a service request under this subscription.
     * NULL quota = unlimited.
     */
    public boolean hasRemainingQuota() {
        return requestQuota == null || requestsUsed < requestQuota;
    }

    /** Remaining requests this period. Returns Integer.MAX_VALUE for unlimited. */
    public int getRemainingRequests() {
        if (requestQuota == null) return Integer.MAX_VALUE;
        return Math.max(0, requestQuota - requestsUsed);
    }
}
