package com.businessos.modules.finance.invoice;

import com.businessos.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ClientInvoiceRepository extends JpaRepository<ClientInvoice, Long> {

    Optional<ClientInvoice> findByInvoiceNumber(String invoiceNumber);

    Optional<ClientInvoice> findByCompanyIdAndInvoiceNumber(Long companyId, String invoiceNumber);

    Optional<ClientInvoice> findByIdAndCompanyId(Long id, Long companyId);

    @EntityGraph(attributePaths = {"client"})
    Page<ClientInvoice> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * Invoices for one company in a date window, with the client joined in.
     *
     * The entity graph matters here: the finance dashboard reads a client name
     * per row, and without it each row triggers its own select.
     */
    @EntityGraph(attributePaths = {"client"})
    List<ClientInvoice> findByCompanyIdAndInvoiceDateBetween(Long companyId, java.time.LocalDate start, java.time.LocalDate end);

    @EntityGraph(attributePaths = {"client"})
    Page<ClientInvoice> findByCompanyIdAndStatus(Long companyId, InvoiceStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"client"})
    Page<ClientInvoice> findByCompanyIdAndClientId(Long companyId, Long clientId, Pageable pageable);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, InvoiceStatus status);

    Page<ClientInvoice> findByCompanyIdAndInvoiceNumberContainingIgnoreCase(Long companyId, String keyword, Pageable pageable);

    /**
     * Used for per-company sequential invoice number generation.
     * Returns the highest invoice number for a given company and year prefix.
     */
    @Query("SELECT MAX(i.invoiceNumber) FROM ClientInvoice i WHERE i.companyId = :companyId AND i.invoiceNumber LIKE :prefix%")
    Optional<String> findMaxInvoiceNumberByCompanyAndPrefix(
        @Param("companyId") Long companyId,
        @Param("prefix") String prefix
    );

    /**
     * Returns all overdue invoices for a company — due date passed, not yet paid/cancelled.
     */
    @Query("""
        SELECT i FROM ClientInvoice i
        WHERE i.companyId = :companyId
          AND i.dueDate < CURRENT_DATE
          AND i.status NOT IN :excludedStatuses
          AND i.deleted = false
        """)
    List<ClientInvoice> findOverdueInvoices(
        @Param("companyId") Long companyId,
        @Param("excludedStatuses") List<InvoiceStatus> excludedStatuses
    );

    @Query("SELECT SUM(i.balanceAmount) FROM ClientInvoice i WHERE i.companyId = :companyId AND i.status IN :statuses AND i.deleted = false")
    Optional<BigDecimal> sumOutstandingByCompanyId(
        @Param("companyId") Long companyId,
        @Param("statuses") List<InvoiceStatus> statuses
    );

    @Query("SELECT SUM(i.balanceAmount) FROM ClientInvoice i WHERE i.companyId = :companyId AND i.client.id = :clientId AND i.status IN :statuses AND i.deleted = false")
    Optional<BigDecimal> sumOutstandingByCompanyIdAndClientId(
        @Param("companyId") Long companyId,
        @Param("clientId") Long clientId,
        @Param("statuses") List<InvoiceStatus> statuses
    );

    long countByCompanyIdAndClientIdAndStatusIn(Long companyId, Long clientId, List<InvoiceStatus> statuses);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ClientInvoice i SET i.status = :newStatus WHERE i.dueDate < :currentDate AND i.status IN :oldStatuses")
    int markOverdueInvoices(
        @Param("currentDate") java.time.LocalDate currentDate,
        @Param("newStatus") InvoiceStatus newStatus,
        @Param("oldStatuses") List<InvoiceStatus> oldStatuses
    );

    /**
     * Used by InvoiceOverdueScheduler to know exactly which invoices are about to
     * flip to OVERDUE - queried BEFORE markOverdueInvoices() runs, so each one can
     * be notified once rather than every day it stays overdue.
     */
    @Query("SELECT i FROM ClientInvoice i WHERE i.dueDate < :currentDate AND i.status IN :oldStatuses AND i.deleted = false")
    List<ClientInvoice> findNewlyOverdue(
        @Param("currentDate") java.time.LocalDate currentDate,
        @Param("oldStatuses") List<InvoiceStatus> oldStatuses
    );

    /** Used by the AR Aging report - anything still owed, regardless of how overdue. */
    @EntityGraph(attributePaths = {"client"})
    @Query("""
        SELECT i FROM ClientInvoice i
        WHERE i.companyId = :companyId
          AND i.status IN :outstandingStatuses
          AND i.balanceAmount > 0
          AND i.deleted = false
        """)
    List<ClientInvoice> findOutstandingByCompanyId(
        @Param("companyId") Long companyId,
        @Param("outstandingStatuses") List<InvoiceStatus> outstandingStatuses
    );
}
