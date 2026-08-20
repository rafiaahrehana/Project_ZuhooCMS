package com.businessos.shared.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

public interface SslCommerzService {

    /** Starts a checkout and returns the SSLCommerz GatewayPageURL to redirect the payer to. */
    String initiate(GatewayPurpose purpose, Long targetId, BigDecimal amount);

    /**
     * Handles the success callback / IPN: validates val_id server-side against
     * the SSLCommerz validation API, then applies the payment to the domain
     * object. Idempotent - a transaction is only ever applied once.
     * Returns the final status for the redirect.
     */
    GatewayTransactionStatus handleSuccess(Map<String, String> params);

    void markFailed(String tranId);

    void markCancelled(String tranId);
}
