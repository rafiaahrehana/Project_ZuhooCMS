package com.businessos.modules.finance.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentReceiptService {
    PaymentReceiptResponse create(PaymentReceiptRequest request);
    PaymentReceiptResponse getById(Long id);
    Page<PaymentReceiptResponse> getAll(Pageable pageable);

    /** The caller's own payment receipts - resolves their Client record from the security context. */
    Page<PaymentReceiptResponse> getMyReceipts(Pageable pageable);
    void confirmPayment(Long id);
    void markAsDeposited(Long id, String bank);

    /**
     * Unwinds a CONFIRMED/DEPOSITED payment that bounced (NSF cheque, chargeback):
     * posts Cr Cash / Dr AR, restores the linked invoice's paidAmount/status, and
     * marks the receipt REVERSED. Previously the REVERSED status existed in the enum
     * with no code path into it - a bounced payment simply couldn't be recorded.
     */
    void reverse(Long id, String reason);
    void delete(Long id);
}
