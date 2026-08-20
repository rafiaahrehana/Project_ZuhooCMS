
package com.businessos.modules.crm.client;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.enums.ClientStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client extends BaseEntity {

    private String billingAddress;
    private String clientCompanyName;
    private String industry;
    private String website;
    private String taxId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientStatus status = ClientStatus.ACTIVE;

    private LocalDate onboardedAt;
    @Builder.Default
    private BigDecimal lifetimeValue = BigDecimal.ZERO;
    @Builder.Default
    private Integer totalRequests = 0;
    @Builder.Default
    private boolean portalAccessEnabled = false; // was missing

    // ==================== Account-level fields (Salesforce-style Account) ====================
    private String shippingAddress;

    // Comma-separated tags, e.g. "vip,enterprise,renewal-risk"
    @Column(length = 500)
    private String tags;

    private Integer employeeCount;

    @Column(precision = 14, scale = 2)
    private BigDecimal annualRevenue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_manager_id")
    private Employee accountManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Null when the Client was created without provisioning a portal login
    // (see CreateClientRequest.provisionPortalLogin).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // Normalized shared-taxonomy tags. Named distinctly from the legacy free-text
    // `tags` field above (kept for backward compatibility, not replaced).
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "client_tags",
        joinColumns = @JoinColumn(name = "client_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private java.util.List<com.businessos.modules.crm.tag.Tag> tagEntities = new java.util.ArrayList<>();
}