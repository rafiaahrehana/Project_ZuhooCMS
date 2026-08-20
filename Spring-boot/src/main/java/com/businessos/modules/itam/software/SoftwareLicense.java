package com.businessos.modules.itam.software;

import com.businessos.core.base.BaseEntity;
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
@Table(name = "software_licenses", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "license_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SoftwareLicense extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(name = "license_key", nullable = false)
    private String licenseKey; // Unique license identifier

    private String softwareName; // e.g., "JetBrains IntelliJ IDEA"
    private String publisher; // e.g., "JetBrains"
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LicenseType licenseType; // PERPETUAL, SUBSCRIPTION, TRIAL, OPEN_SOURCE

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LicenseStatus licenseStatus = LicenseStatus.ACTIVE;

    // License Count
    private int totalSeatsLicensed; // How many users can use
    private int seatsUsed; // Currently assigned
    private int seatsAvailable; // totalSeats - seatsUsed

    // Purchase Info
    private LocalDate licensePurchaseDate;
    private BigDecimal licenseCost; // Cost per seat or total

    // Expiry & Renewal
    private LocalDate licenseExpiryDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LicenseRenewalType renewalType = LicenseRenewalType.ANNUAL;

    private LocalDate nextRenewalDate;
    private BigDecimal renewalCost;

    // Vendor Info
    private String vendor; // Where purchased (AWS, Azure, etc.)
    private String accountEmail; // Licensed account
    private String licenseUrl; // Portal URL for management

    // Credentials (encrypted in production)
    private String username;
    private String passwordHash; // Never store plain text!

    // Usage & Compliance
    private String installationLocation; // Cloud, On-premise, etc.
    private int estimatedUserCount; // Expected users
    private String complianceNotes;

    // Notes
    private String notes;
    private String renewalNotes;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean autoRenew = true;

    public boolean isExpiringSoon() {
        if (licenseExpiryDate == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        return licenseExpiryDate.isBefore(thirtyDaysFromNow) && licenseExpiryDate.isAfter(today);
    }

    public boolean isExpired() {
        if (licenseExpiryDate == null) return false;
        return licenseExpiryDate.isBefore(LocalDate.now());
    }

    public long getDaysUntilExpiry() {
        if (licenseExpiryDate == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), licenseExpiryDate);
    }

    public void assignSeat() {
        if (seatsUsed < totalSeatsLicensed) {
            seatsUsed++;
            seatsAvailable = totalSeatsLicensed - seatsUsed;
        }
    }

    public void releaseSeat() {
        if (seatsUsed > 0) {
            seatsUsed--;
            seatsAvailable = totalSeatsLicensed - seatsUsed;
        }
    }
}
