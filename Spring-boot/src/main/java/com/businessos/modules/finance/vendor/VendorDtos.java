package com.businessos.modules.finance.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VendorDtos {

    @Data
    public static class VendorRequest {
        @NotBlank(message = "Vendor name is required")
        @Size(max = 200)
        private String name;
        @Size(max = 150) private String contactPerson;
        @Size(max = 150) private String email;
        @Size(max = 50) private String phone;
        @Size(max = 100) private String taxId;
        @Size(max = 500) private String address;
        @Size(max = 100) private String paymentTerms;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorResponse {
        private Long id;
        private String name;
        private String contactPerson;
        private String email;
        private String phone;
        private String taxId;
        private String address;
        private String paymentTerms;
        private String notes;
        private boolean active;
        // Rolled up from this vendor's bills - what we still owe them.
        private BigDecimal outstandingBalance;
        private LocalDateTime createdAt;
    }

    public static VendorResponse toResponse(Vendor v, BigDecimal outstandingBalance) {
        if (v == null) return null;
        return VendorResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .contactPerson(v.getContactPerson())
                .email(v.getEmail())
                .phone(v.getPhone())
                .taxId(v.getTaxId())
                .address(v.getAddress())
                .paymentTerms(v.getPaymentTerms())
                .notes(v.getNotes())
                .active(v.isActive())
                .outstandingBalance(outstandingBalance != null ? outstandingBalance : BigDecimal.ZERO)
                .createdAt(v.getCreatedAt())
                .build();
    }
}
