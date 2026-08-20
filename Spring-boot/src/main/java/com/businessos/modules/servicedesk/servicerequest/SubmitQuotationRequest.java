package com.businessos.modules.servicedesk.servicerequest;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubmitQuotationRequest {
    private BigDecimal amount;
    private String currency;
    private String notes;
    private LocalDateTime validUntil;
}
