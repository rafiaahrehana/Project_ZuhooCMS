package com.businessos.shared.payment.wallet;

import com.businessos.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletService {

    WalletResponse getOrCreateWallet();

    Page<WalletTransactionResponse> getTransactions(WalletTransactionType type, Pageable pageable);

    /**
     * Internal method used by Invoice/Payment services.
     * Debits the wallet and records a ledger entry.
     */
    Wallet debit(String contextType, Long contextId, BigDecimal amount, String reference, String notes);

    /**
     * Internal method used by Refund servicereview.
     * Credits the wallet balance.
     */
    Wallet credit(String contextType, Long contextId, BigDecimal amount, WalletTransactionType type,
                  String reference, String notes);
}
