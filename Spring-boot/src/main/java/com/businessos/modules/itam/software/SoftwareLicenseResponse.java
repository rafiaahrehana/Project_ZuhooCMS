package com.businessos.modules.itam.software;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SoftwareLicenseResponse {
    private Long id;
    private Long companyId;
    private String licenseKey;
    private String softwareName;
    private String publisher;
    private String version;

    private LicenseType licenseType;
    private LicenseStatus licenseStatus;

    private int totalSeatsLicensed;
    private int seatsUsed;
    private int seatsAvailable;

    private LocalDate licensePurchaseDate;
    private BigDecimal licenseCost;

    private LocalDate licenseExpiryDate;
    private boolean expiringSoon;
    private boolean expired;
    private long daysUntilExpiry;

    private LicenseRenewalType renewalType;
    private LocalDate nextRenewalDate;
    private BigDecimal renewalCost;

    private String vendor;
    private String accountEmail;
    private String licenseUrl;
    private String installationLocation;
    private int estimatedUserCount;
    private String complianceNotes;

    private boolean active;
    private boolean autoRenew;
    private String notes;
    private String renewalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}