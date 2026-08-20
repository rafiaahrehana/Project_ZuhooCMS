package com.businessos.modules.finance.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {

    Optional<VendorBill> findByIdAndCompanyId(Long id, Long companyId);

    Page<VendorBill> findByCompanyId(Long companyId, Pageable pageable);

    Page<VendorBill> findByCompanyIdAndStatus(Long companyId, VendorBillStatus status, Pageable pageable);

    Page<VendorBill> findByCompanyIdAndVendorId(Long companyId, Long vendorId, Pageable pageable);

    java.util.List<VendorBill> findByCompanyIdAndBalanceAmountGreaterThan(Long companyId, java.math.BigDecimal min);

    boolean existsByVendorIdAndCompanyId(Long vendorId, Long companyId);

    /** APPROVED/PARTIALLY_PAID bills = money the company still owes. */
    @Query("SELECT b FROM VendorBill b WHERE b.companyId = :companyId AND b.status IN :statuses")
    List<VendorBill> findOutstandingByCompanyId(@Param("companyId") Long companyId,
                                                 @Param("statuses") List<VendorBillStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.balanceAmount), 0) FROM VendorBill b " +
           "WHERE b.companyId = :companyId AND b.vendor.id = :vendorId " +
           "AND b.status IN (com.businessos.modules.finance.vendor.VendorBillStatus.APPROVED, " +
           "com.businessos.modules.finance.vendor.VendorBillStatus.PARTIALLY_PAID, " +
           "com.businessos.modules.finance.vendor.VendorBillStatus.OVERDUE)")
    BigDecimal sumOutstandingByVendor(@Param("companyId") Long companyId, @Param("vendorId") Long vendorId);

    @Modifying
    @Query("UPDATE VendorBill b SET b.status = :newStatus WHERE b.dueDate < :currentDate AND b.status IN :oldStatuses")
    int markOverdueBills(
        @Param("currentDate") java.time.LocalDate currentDate,
        @Param("newStatus") VendorBillStatus newStatus,
        @Param("oldStatuses") List<VendorBillStatus> oldStatuses
    );

    @Query("SELECT b FROM VendorBill b WHERE b.dueDate < :currentDate AND b.status IN :oldStatuses AND b.deleted = false")
    List<VendorBill> findNewlyOverdueBills(
        @Param("currentDate") java.time.LocalDate currentDate,
        @Param("oldStatuses") List<VendorBillStatus> oldStatuses
    );

    @Query("SELECT MAX(b.billNumber) FROM VendorBill b WHERE b.companyId = :companyId AND b.billNumber LIKE :prefix%")
    Optional<String> findMaxBillNumberByCompanyAndPrefix(@Param("companyId") Long companyId, @Param("prefix") String prefix);

    /**
     * Approved vendor-bill spend against one expense account, for budget
     * tracking. totalAmount (not balanceAmount) - the expense is recognized
     * at approval per postToLedger, not at payment, so a budget should count
     * it the same moment the GL does. DRAFT excluded (not yet recognized),
     * CANCELLED excluded (reversed).
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM VendorBill b " +
           "WHERE b.companyId = :companyId AND b.expenseAccount.accountName = :accountName " +
           "AND b.billDate BETWEEN :start AND :end " +
           "AND b.status IN (com.businessos.modules.finance.vendor.VendorBillStatus.APPROVED, " +
           "com.businessos.modules.finance.vendor.VendorBillStatus.PARTIALLY_PAID, " +
           "com.businessos.modules.finance.vendor.VendorBillStatus.OVERDUE, " +
           "com.businessos.modules.finance.vendor.VendorBillStatus.PAID) " +
           "AND b.deleted = false")
    BigDecimal sumByExpenseAccountNameAndDateRange(@Param("companyId") Long companyId,
                                                    @Param("accountName") String accountName,
                                                    @Param("start") java.time.LocalDate start,
                                                    @Param("end") java.time.LocalDate end);
}
