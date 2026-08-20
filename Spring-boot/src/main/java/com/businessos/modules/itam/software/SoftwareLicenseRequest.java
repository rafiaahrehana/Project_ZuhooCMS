package com.businessos.modules.itam.software;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// AllArgsConstructor access is package-private (not public): Jackson picks a visible
// all-args constructor as its deserialization "creator" over the no-args+setters path,
// and a creator must supply every parameter - a JSON body omitting a primitive
// int/boolean field (e.g. autoRenew, never sent by the frontend form) then fails with
// "Cannot map null into type int/boolean" before @Valid even runs. Restricting
// visibility stops Jackson from seeing it, so it falls back to no-args+setters, which
// correctly leaves missing primitives at their Java default (0/false).
@Data @NoArgsConstructor @AllArgsConstructor(access = AccessLevel.PACKAGE) @Builder
public class SoftwareLicenseRequest {

    @NotBlank(message = "License key is required")
    private String licenseKey;

    @NotBlank(message = "Software name is required")
    private String softwareName;

    @NotBlank(message = "Publisher is required")
    private String publisher;

    private String version;

    @NotNull(message = "License type is required")
    private LicenseType licenseType;

    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeatsLicensed;

    @NotNull(message = "License purchase date is required")
    private LocalDate licensePurchaseDate;

    @NotNull(message = "License cost is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal licenseCost;

    @NotNull(message = "License expiry date is required")
    private LocalDate licenseExpiryDate;

    @NotNull(message = "Renewal type is required")
    private LicenseRenewalType renewalType;

    private LocalDate nextRenewalDate;
    private BigDecimal renewalCost;

    private String vendor;
    private String accountEmail;
    private String licenseUrl;
    private String username;
    private String passwordHash;

    private String installationLocation;
    private int estimatedUserCount;
    private String complianceNotes;
    private String notes;
    private String renewalNotes;

    // Boolean (not boolean) - the frontend "Add License" form has no auto-renew
    // control, so this key is simply absent from the JSON body. Jackson binds
    // SoftwareLicenseRequest via its all-args constructor (Lombok @AllArgsConstructor
    // + parameter-names), and for a *primitive* constructor parameter that's missing
    // from the payload, it fails with "Cannot map `null` into type `boolean`" instead
    // of defaulting - only wrapper types tolerate a missing/null value here.
    private Boolean autoRenew;

    public boolean isAutoRenewOrDefault() {
        return autoRenew == null || autoRenew;
    }
}