package com.businessos.modules.hrm.asset;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "assets")
@Getter @Setter @NoArgsConstructor
public class Asset extends BaseEntity {

    @Column(nullable = false) private String name;
    private String assetTag; // e.g. LPT-001
    private String brand;
    private String model;
    private String serialNumber;
    private String category; // Laptop, Monitor, Phone, Furniture
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT") private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private Employee assignedTo;

    private LocalDate assignedAt;
    private LocalDate returnedAt;

    // IT Hardware Specific Fields (Nullable)
    private String ipAddress;
    private String macAddress;
    private String processorModel;
    private String ramSize;
    private String storageSize;
    private String operatingSystem;

    // Disposal / Lifecycle
    private LocalDate disposalDate;
    private String disposalReason;

    // Warranty-expiry scheduler bookkeeping: set once each alert has been sent
    // so WarrantyExpiryScheduler notifies exactly once per state, not daily.
    // Null again would mean "never alerted" - there's no reset path today, same
    // as SoftwareLicense's expiry status doesn't reset without a new expiry date.
    private LocalDate warrantyExpiringSoonAlertedAt;
    private LocalDate warrantyExpiredAlertedAt;
}
