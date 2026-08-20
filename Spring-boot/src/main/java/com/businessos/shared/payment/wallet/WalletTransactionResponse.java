package com.businessos.shared.payment.wallet;

import com.businessos.enums.WalletTransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class WalletTransactionResponse {
    private Long id;
    private WalletTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String reference;
    private String notes;
    private LocalDateTime transactedAt;
}
