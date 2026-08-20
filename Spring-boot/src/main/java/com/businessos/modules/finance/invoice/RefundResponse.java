package com.businessos.modules.finance.invoice;

import com.businessos.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private Long id;
    private Long clientInvoiceId;
    private String invoiceNumber;
    private Long clientId;
    private String clientName;
    private Long serviceRequestId;
    private String serviceRequestTitle;
    private BigDecimal requestedAmount;
    private RefundStatus status;
    private String reason;
    private LocalDateTime requestedAt;
    private String processedByName;
    private LocalDateTime processedAt;
    private String rejectionReason;
}
