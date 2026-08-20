package com.businessos.modules.finance.invoice;

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
public class CreditNoteResponse {
    private Long id;
    private String creditNoteNumber;
    private Long clientInvoiceId;
    private String invoiceNumber;
    private Long clientId;
    private String clientName;
    private BigDecimal amount;
    private String reason;
    private String issuedByName;
    private LocalDateTime issuedAt;
}
