package com.businessos.modules.hrm.asset;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssetAssignmentHistoryResponse {
    private Long id;
    private Long assetId;
    private String assetName;
    private Long employeeId;
    private String employeeName;
    private LocalDate assignedAt;
    private LocalDate returnedAt;
    private String condition;
    private String conditionOnReturn;
    private String notes;
    private Long assignedById;
    private String assignedByName;
    private LocalDateTime createdAt;
}
