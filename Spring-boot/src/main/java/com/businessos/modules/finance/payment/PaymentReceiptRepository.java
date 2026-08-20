package com.businessos.modules.finance.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {
    Optional<PaymentReceipt> findByIdAndCompanyId(Long id, Long companyId);
    Page<PaymentReceipt> findByCompanyId(Long companyId, Pageable pageable);
    Page<PaymentReceipt> findByCompanyIdAndClientId(Long companyId, Long clientId, Pageable pageable);

    /** Used by PaymentReceiptServiceImpl.generateReceiptNumber - MAX-based, scoped per company. */
    @Query("SELECT MAX(r.receiptNumber) FROM PaymentReceipt r WHERE r.companyId = :companyId AND r.receiptNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxReceiptNumberByCompanyAndPrefix(@Param("companyId") Long companyId, @Param("prefix") String prefix);
}
