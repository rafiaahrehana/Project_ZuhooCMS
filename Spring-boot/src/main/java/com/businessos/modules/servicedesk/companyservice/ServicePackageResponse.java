package com.businessos.modules.servicedesk.companyservice;

import lombok.Getter;
import lombok.Setter;
import com.businessos.enums.BillingCycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ServicePackageResponse {
    private Long id;
    private String name;
    private String nameBn;
    private String description;
    private String descriptionBn;
    private String iconUrl;
    private BigDecimal packagePrice;
    private BigDecimal discountPercent;
    private BillingCycle billingCycle;
    private Integer requestQuota;
    private boolean autoRenew;
    private boolean active;
    private Integer deliveryDays;
    private String terms;
    private boolean featured;
    private boolean popular;

    // Computed
    private BigDecimal effectivePrice;
    private BigDecimal totalServicePrice;

    // Included services summary
    private List<CompanyServiceResponse> services;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
