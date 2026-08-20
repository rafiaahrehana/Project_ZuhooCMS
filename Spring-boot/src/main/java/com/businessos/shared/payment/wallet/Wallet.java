package com.businessos.shared.payment.wallet;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// No tenant @Filter here on purpose - unlike its sibling entities, Wallet has no
// company_id column. Scoping is done via contextType/contextId instead (see
// WalletServiceImpl). Do not uncomment a commented-out version of this - it
// will break startup (no such column) or silently mis-scope queries.
@Entity
@Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet extends BaseEntity {

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // Promotional / trial credit balance — separate from real cash balance.
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "BDT";

    @Column(name = "context_type", nullable = false)
    private String contextType; // PLATFORM, COMPANY, CLIENT

    @Column(name = "context_id", nullable = false)
    private Long contextId;

    // Optional constraint: UNIQUE(context_type, context_id)
    // Total spendable amount = cash balance + credit balance.
    public BigDecimal getTotalAvailable() {
        BigDecimal b = balance      != null ? balance      : BigDecimal.ZERO;
        BigDecimal c = creditBalance != null ? creditBalance : BigDecimal.ZERO;
        return b.add(c);
    }
}
