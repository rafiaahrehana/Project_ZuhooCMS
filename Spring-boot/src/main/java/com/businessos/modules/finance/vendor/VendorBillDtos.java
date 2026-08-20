package com.businessos.modules.finance.vendor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class VendorBillDtos {

    @Data
    public static class VendorBillRequest {
        @NotNull(message = "Vendor is required")
        private Long vendorId;
        @NotNull(message = "Bill date is required")
        private LocalDate billDate;
        @NotNull(message = "Due date is required")
        private LocalDate dueDate;
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        private BigDecimal subtotal;
        @DecimalMin(value = "0.0")
        private BigDecimal taxAmount;
        private String vendorReference;
        private String description;
        // Optional COA account (EXPENSE type) the bill's cost posts to on approval.
        private Long expenseAccountId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorBillResponse {
        private Long id;
        private String billNumber;
        private String vendorReference;
        private Long vendorId;
        private String vendorName;
        private LocalDate billDate;
        private LocalDate dueDate;
        private BigDecimal subtotal;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal balanceAmount;
        private VendorBillStatus status;
        private String description;
        private Long expenseAccountId;
        private String expenseAccountName;
        private String createdBy;
        private String approvedBy;
        private LocalDate approvedDate;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApAgeingLine {
        private Long billId;
        private String billNumber;
        private Long vendorId;
        private String vendorName;
        private LocalDate dueDate;
        private BigDecimal balanceAmount;
        private long daysOverdue;
        private String bucket;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApAgeingReport {
        private LocalDate asOfDate;
        private BigDecimal current;
        private BigDecimal days1to30;
        private BigDecimal days31to60;
        private BigDecimal days61to90;
        private BigDecimal over90;
        private BigDecimal totalOutstanding;
        private List<ApAgeingLine> lines;
    }

    public static VendorBillResponse toResponse(VendorBill b) {
        if (b == null) return null;
        return VendorBillResponse.builder()
                .id(b.getId())
                .billNumber(b.getBillNumber())
                .vendorReference(b.getVendorReference())
                .vendorId(b.getVendor() != null ? b.getVendor().getId() : null)
                .vendorName(b.getVendor() != null ? b.getVendor().getName() : null)
                .billDate(b.getBillDate())
                .dueDate(b.getDueDate())
                .subtotal(b.getSubtotal())
                .taxAmount(b.getTaxAmount())
                .totalAmount(b.getTotalAmount())
                .paidAmount(b.getPaidAmount())
                .balanceAmount(b.getBalanceAmount())
                .status(b.getStatus())
                .description(b.getDescription())
                .expenseAccountId(b.getExpenseAccount() != null ? b.getExpenseAccount().getId() : null)
                .expenseAccountName(b.getExpenseAccount() != null ? b.getExpenseAccount().getAccountName() : null)
                .createdBy(b.getCreatedBy())
                .approvedBy(b.getApprovedBy())
                .approvedDate(b.getApprovedDate())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
