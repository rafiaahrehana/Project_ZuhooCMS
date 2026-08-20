package com.businessos.modules.servicedesk.companyservice;

import com.businessos.core.base.BaseEntity;
import com.businessos.enums.BillingCycle;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "service_packages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServicePackage extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String nameBn;

    @Column(columnDefinition = "TEXT")
    private String descriptionBn;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal packagePrice = BigDecimal.ZERO;


    @Builder.Default
    @Column(nullable = false)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    private Integer requestQuota;

    @Column(name = "overage_rate", precision = 10, scale = 2)
    private BigDecimal overageRate;

    @Builder.Default
    private boolean autoRenew = true;

    @Builder.Default
    private boolean active = true;

    private String iconUrl;

    private Integer deliveryDays;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Builder.Default
    private boolean featured = false;

    @Builder.Default
    private boolean popular = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "service_package_items",
        joinColumns = @JoinColumn(name = "package_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private List<CompanyService> services = new ArrayList<>();


    public BigDecimal getTotalServicePrice() {
        return services.stream()
            .filter(s -> s.getPrice() != null)
            .map(CompanyService::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getEffectivePrice() {
        if (packagePrice != null) return packagePrice;
        BigDecimal total = getTotalServicePrice();
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) return total;
        BigDecimal discount = total.multiply(discountPercent.divide(BigDecimal.valueOf(100)));
        return total.subtract(discount).max(BigDecimal.ZERO);
    }

    public boolean includesService(Long serviceId) {
        return services.stream().anyMatch(s -> s.getId().equals(serviceId));
    }
}
