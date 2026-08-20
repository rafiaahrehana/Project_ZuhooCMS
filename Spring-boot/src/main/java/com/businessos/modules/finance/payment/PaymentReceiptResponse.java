package com.businessos.modules.finance.payment;

import com.businessos.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentReceiptResponse {
    private Long id;
    private Long companyId;
    private String receiptNumber;
    private Long invoiceId;
    private String invoiceNumber;
    private Long clientId;
    private String clientName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String transactionReference;
    private PaymentStatus status;
    private String depositedToBank;
    private LocalDate reversedDate;
    private String reversalReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
