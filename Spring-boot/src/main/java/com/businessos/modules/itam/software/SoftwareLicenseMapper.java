package com.businessos.modules.itam.software;

public class SoftwareLicenseMapper {

    public static SoftwareLicenseResponse toResponse(SoftwareLicense entity) {
        if (entity == null) return null;

        return SoftwareLicenseResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .licenseKey(entity.getLicenseKey())
                .softwareName(entity.getSoftwareName())
                .publisher(entity.getPublisher())
                .version(entity.getVersion())
                .licenseType(entity.getLicenseType())
                .totalSeatsLicensed(entity.getTotalSeatsLicensed())
                .seatsUsed(entity.getSeatsUsed())
                .seatsAvailable(entity.getSeatsAvailable())
                .licensePurchaseDate(entity.getLicensePurchaseDate())
                .licenseCost(entity.getLicenseCost())
                .licenseExpiryDate(entity.getLicenseExpiryDate())
                .expiringSoon(entity.isExpiringSoon())
                .expired(entity.isExpired())
                .daysUntilExpiry(entity.getDaysUntilExpiry())
                .licenseStatus(entity.getLicenseStatus())
                .renewalType(entity.getRenewalType())
                .nextRenewalDate(entity.getNextRenewalDate())
                .renewalCost(entity.getRenewalCost())
                .vendor(entity.getVendor())
                .accountEmail(entity.getAccountEmail())
                .licenseUrl(entity.getLicenseUrl())
                .installationLocation(entity.getInstallationLocation())
                .estimatedUserCount(entity.getEstimatedUserCount())
                .complianceNotes(entity.getComplianceNotes())
                .active(entity.isActive())
                .autoRenew(entity.isAutoRenew())
                .notes(entity.getNotes())
                .renewalNotes(entity.getRenewalNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static SoftwareLicense toEntity(SoftwareLicenseRequest request) {
        if (request == null) return null;

        return SoftwareLicense.builder()
                .licenseKey(request.getLicenseKey())
                .softwareName(request.getSoftwareName())
                .publisher(request.getPublisher())
                .version(request.getVersion())
                .licenseType(request.getLicenseType())
                .totalSeatsLicensed(request.getTotalSeatsLicensed())
                .licensePurchaseDate(request.getLicensePurchaseDate())
                .licenseCost(request.getLicenseCost())
                .licenseExpiryDate(request.getLicenseExpiryDate())
                .renewalType(request.getRenewalType())
                .nextRenewalDate(request.getNextRenewalDate())
                .renewalCost(request.getRenewalCost())
                .vendor(request.getVendor())
                .accountEmail(request.getAccountEmail())
                .licenseUrl(request.getLicenseUrl())
                .username(request.getUsername())
                .installationLocation(request.getInstallationLocation())
                .estimatedUserCount(request.getEstimatedUserCount())
                .complianceNotes(request.getComplianceNotes())
                .autoRenew(request.isAutoRenewOrDefault())
                .notes(request.getNotes())
                .renewalNotes(request.getRenewalNotes())
                .build();
    }
}