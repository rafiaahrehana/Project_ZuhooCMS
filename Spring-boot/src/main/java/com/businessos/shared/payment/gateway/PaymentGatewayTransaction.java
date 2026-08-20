package com.businessos.shared.payment.gateway;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row per gateway checkout attempt. tranId is what we send to SSLCommerz
 * and what its callbacks/IPN echo back; companyId is captured at initiate time
 * (authenticated) because callbacks arrive with no security context.
 */
@Entity
@Table(name = "payment_gateway_transactions",
       indexes = @Index(name = "idx_pgt_tran_id", columnList = "tranId", unique = true))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentGatewayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String tranId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GatewayPurpose purpose;

    /** Invoice id / subscription id; null for wallet top-ups */
    private Long targetId;

    @Column(nullable = false)
    private Long companyId;

    private Long initiatedByUserId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 5)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GatewayTransactionStatus status;

    private String valId;
    private String bankTranId;
    private String cardType;

    @Column(nullable = false)
    private LocalDateTime initiatedAt;

    private LocalDateTime completedAt;
}
