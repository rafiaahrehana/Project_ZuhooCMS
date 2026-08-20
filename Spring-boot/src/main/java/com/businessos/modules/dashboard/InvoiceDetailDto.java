package com.businessos.modules.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailDto {
    private String invoiceNumber;
    private String clientName;
    private BigDecimal amount;
    private long daysOverdue;
}
