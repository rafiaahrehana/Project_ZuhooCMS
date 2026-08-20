package com.businessos.modules.finance.invoice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientInvoiceItemRequest {
    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    private String notes;
}
