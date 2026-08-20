package com.businessos.modules.finance.payment;

import com.businessos.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentReceiptRequest {
    @NotNull(message = "Client ID is required")
    private Long clientId;
    private Long invoiceId;
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    private String transactionReference;
    private String notes;
}
