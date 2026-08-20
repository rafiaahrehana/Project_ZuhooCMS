package com.businessos.modules.finance.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    Page<CreditNote> findByCompanyId(Long companyId, Pageable pageable);

    Page<CreditNote> findByCompanyIdAndClientInvoiceId(Long companyId, Long clientInvoiceId, Pageable pageable);

    Optional<CreditNote> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Used for per-company sequential credit note number generation.
     * Returns the highest credit note number for a given company and year prefix.
     */
    @Query("SELECT MAX(cn.creditNoteNumber) FROM CreditNote cn WHERE cn.companyId = :companyId AND cn.creditNoteNumber LIKE :prefix%")
    Optional<String> findMaxCreditNoteNumberByCompanyAndPrefix(
        @Param("companyId") Long companyId,
        @Param("prefix") String prefix
    );
}
