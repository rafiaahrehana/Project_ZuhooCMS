package com.businessos.shared.payment.wallet;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletResponse {
    private Long id;
    private BigDecimal balance;
    private BigDecimal creditBalance;
    private BigDecimal totalAvailable;
    private String currency;
}
