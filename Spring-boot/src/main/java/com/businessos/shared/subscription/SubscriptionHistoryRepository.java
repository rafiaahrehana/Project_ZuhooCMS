package com.businessos.shared.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    Page<SubscriptionHistory> findByCompanyId(Long companyId, Pageable pageable);

    // Bucketed by day in Java (DashboardServiceImpl) rather than a DB-side GROUP BY -
    // keeps this portable across JPA providers instead of relying on dialect-specific
    // date-truncation functions.
    java.util.List<SubscriptionHistory> findByChangedAtGreaterThanEqualOrderByChangedAtAsc(LocalDateTime from);

    @Query("SELECT COALESCE(SUM(h.amountPaid), 0) FROM SubscriptionHistory h")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(h.amountPaid), 0) FROM SubscriptionHistory h WHERE h.changedAt >= :from")
    BigDecimal sumRevenueSince(@Param("from") LocalDateTime from);
}
