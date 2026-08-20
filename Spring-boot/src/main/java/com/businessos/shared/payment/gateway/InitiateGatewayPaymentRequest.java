package com.businessos.shared.payment.gateway;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiateGatewayPaymentRequest {
    @NotNull(message = "Purpose is required")
    private GatewayPurpose purpose;
    /** Invoice id / subscription id; null for wallet top-ups */
    private Long targetId;
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}
