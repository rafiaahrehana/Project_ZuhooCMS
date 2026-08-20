package com.businessos.shared.payment.gateway;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentGatewayTransactionRepository
        extends JpaRepository<PaymentGatewayTransaction, Long> {

    Optional<PaymentGatewayTransaction> findByTranId(String tranId);

    /**
     * SSLCommerz fires both the browser success redirect and the server-to-server
     * IPN for the same tran_id, near-concurrently. Locking the row here closes the
     * check-then-act gap in handleSuccess()'s idempotency check, so a second caller
     * blocks until the first has committed SUCCESS and then sees it - preventing a
     * double credit/double invoice-payment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentGatewayTransaction t WHERE t.tranId = :tranId")
    Optional<PaymentGatewayTransaction> findByTranIdForUpdate(@Param("tranId") String tranId);
}
