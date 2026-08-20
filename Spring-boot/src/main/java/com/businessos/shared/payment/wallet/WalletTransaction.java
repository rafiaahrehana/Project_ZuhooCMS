package com.businessos.shared.payment.wallet;

import com.businessos.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    private String reference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime transactedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
}
