package com.businessos.modules.servicedesk.companyservice;

import lombok.Getter;
import lombok.Setter;
import com.businessos.enums.BillingCycle;
import com.businessos.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PackageSubscriptionResponse {
    private Long id;
    private Long packageId;
    private String packageName;
    private Long clientId;
    private String clientName;
    private SubscriptionStatus status;
    private BillingCycle billingCycle;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextBillingDate;
    private BigDecimal pricePaid;
    private Integer requestQuota;
    private int requestsUsed;
    private int remainingRequests;   // computed
    private boolean autoRenew;
    private LocalDateTime activatedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
}
