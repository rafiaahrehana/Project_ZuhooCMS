package com.businessos.modules.hrm.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetRequest {
    @NotBlank(message = "Asset name is required")
    @Size(max = 150)
    private String name;
    @Size(max = 100)
    private String category;
    @Size(max = 100)
    private String serialNumber;
    private String description;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private Long assignedToId;
    private String notes;

    // IT Hardware Specific Fields - already on the Asset entity, now settable via the API
    @Size(max = 100)
    private String assetTag;
    @Size(max = 100)
    private String brand;
    @Size(max = 100)
    private String model;
    private String ipAddress;
    private String macAddress;
    private String processorModel;
    private String ramSize;
    private String storageSize;
    private String operatingSystem;
    private LocalDate warrantyExpiry;
}
