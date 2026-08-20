package com.businessos.shared.subscription;

import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "subscription_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // References SubscriptionPlanDefinition.code, same as Company.subscriptionPlan.
    @Column(nullable = false)
    private String fromPlan;

    @Column(nullable = false)
    private String toPlan;

    private LocalDate subscriptionStart;
    private LocalDate subscriptionEnd;
    private BigDecimal amountPaid;
    private String transactionRef;
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;
}
