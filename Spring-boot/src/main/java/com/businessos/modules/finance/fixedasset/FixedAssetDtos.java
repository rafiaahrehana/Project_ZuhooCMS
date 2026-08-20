package com.businessos.modules.finance.fixedasset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FixedAssetDtos {

    @Data
    public static class FixedAssetRequest {
        @NotBlank(message = "Asset name is required")
        private String name;
        private String assetTag;
        private String category;
        @NotNull(message = "Cost is required")
        @DecimalMin(value = "0.01", message = "Cost must be greater than zero")
        private BigDecimal cost;
        @DecimalMin(value = "0.0")
        private BigDecimal salvageValue;
        @NotNull(message = "Useful life (months) is required")
        @Min(value = 1, message = "Useful life must be at least 1 month")
        private Integer usefulLifeMonths;
        @NotNull(message = "Acquisition date is required")
        private LocalDate acquisitionDate;
        private String notes;
        // When true (default) registering the asset posts Dr Fixed Assets / Cr Cash.
        // Set false for assets bought before this system existed (opening balances).
        private Boolean postPurchaseToLedger;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FixedAssetResponse {
        private Long id;
        private String name;
        private String assetTag;
        private String category;
        private BigDecimal cost;
        private BigDecimal salvageValue;
        private int usefulLifeMonths;
        private LocalDate acquisitionDate;
        private BigDecimal accumulatedDepreciation;
        private BigDecimal bookValue;
        private BigDecimal monthlyDepreciation;
        private FixedAssetStatus status;
        private String notes;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepreciationRunResponse {
        private Long id;
        private int year;
        private int month;
        private BigDecimal totalAmount;
        private int assetsDepreciated;
        private String runBy;
        private LocalDateTime runAt;
    }

    public static FixedAssetResponse toResponse(FixedAsset a) {
        if (a == null) return null;
        return FixedAssetResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .assetTag(a.getAssetTag())
                .category(a.getCategory())
                .cost(a.getCost())
                .salvageValue(a.getSalvageValue())
                .usefulLifeMonths(a.getUsefulLifeMonths())
                .acquisitionDate(a.getAcquisitionDate())
                .accumulatedDepreciation(a.getAccumulatedDepreciation())
                .bookValue(a.bookValue())
                .monthlyDepreciation(a.monthlyDepreciation())
                .status(a.getStatus())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .build();
    }

    public static DepreciationRunResponse toResponse(DepreciationRun r) {
        if (r == null) return null;
        return DepreciationRunResponse.builder()
                .id(r.getId())
                .year(r.getYear())
                .month(r.getMonth())
                .totalAmount(r.getTotalAmount())
                .assetsDepreciated(r.getAssetsDepreciated())
                .runBy(r.getRunBy())
                .runAt(r.getRunAt())
                .build();
    }
}
