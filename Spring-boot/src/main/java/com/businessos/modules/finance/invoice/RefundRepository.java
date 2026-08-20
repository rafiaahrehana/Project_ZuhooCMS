package com.businessos.modules.finance.invoice;

import com.businessos.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Page<Refund> findByCompanyIdAndStatus(Long companyId, RefundStatus status, Pageable pageable);

    Page<Refund> findByCompanyId(Long companyId, Pageable pageable);

    Optional<Refund> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByClientInvoiceIdAndCompanyIdAndStatus(Long clientInvoiceId, Long companyId, RefundStatus status);

    // Ordered newest-first so the caller can take the first match per invoice as "latest".
    List<Refund> findByClientInvoiceIdInAndCompanyIdOrderByCreatedAtDesc(List<Long> clientInvoiceIds, Long companyId);

    // Refunds have no reference number of their own - match on the linked invoice
    // number, the client's name, or the refund reason instead.
    @Query("SELECT r FROM Refund r " +
           "LEFT JOIN r.clientInvoice ci " +
           "LEFT JOIN ci.client c " +
           "LEFT JOIN c.user u " +
           "WHERE r.companyId = :companyId AND (" +
           "LOWER(ci.invoiceNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(c.clientCompanyName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(r.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') AND " +
           "r.deleted = false")
    Page<Refund> searchRefunds(@Param("companyId") Long companyId, @Param("keyword") String keyword, Pageable pageable);
}
