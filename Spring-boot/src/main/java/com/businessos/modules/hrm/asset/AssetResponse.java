package com.businessos.modules.hrm.asset;

import com.businessos.enums.AssetStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssetResponse {
    private Long id;
    private String name;
    private String category;
    private String serialNumber;
    private String description;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private AssetStatus status;
    private LocalDate assignedAt;
    private LocalDate returnDate;
    private String notes;
    private Long assignedToId;
    private String assignedToName;
    private LocalDateTime createdAt;

    // IT Hardware Specific Fields - already on the Asset entity, now exposed via the API
    private String assetTag;
    private String brand;
    private String model;
    private String ipAddress;
    private String macAddress;
    private String processorModel;
    private String ramSize;
    private String storageSize;
    private String operatingSystem;
    private LocalDate warrantyExpiry;
}
