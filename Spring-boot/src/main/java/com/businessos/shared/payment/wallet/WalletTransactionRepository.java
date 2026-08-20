package com.businessos.shared.payment.wallet;

import com.businessos.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletContextTypeAndWalletContextIdOrderByTransactedAtDesc(String contextType, Long contextId, Pageable pageable);

    Page<WalletTransaction> findByWalletContextTypeAndWalletContextIdAndTypeOrderByTransactedAtDesc(
        String contextType, Long contextId, WalletTransactionType type, Pageable pageable);

    /**
     * Last transaction for a wallet — used to derive current running balance.
     */
    Optional<WalletTransaction> findTopByWalletIdOrderByTransactedAtDesc(Long walletId);

    @Query("SELECT SUM(t.amount) FROM WalletTransaction t WHERE t.wallet.contextType = :contextType AND t.wallet.contextId = :contextId AND t.type = :type AND t.transactedAt >= :from")
    Optional<BigDecimal> sumByWalletContextTypeAndWalletContextIdAndTypeAfter(String contextType, Long contextId, WalletTransactionType type, LocalDateTime from);
}
