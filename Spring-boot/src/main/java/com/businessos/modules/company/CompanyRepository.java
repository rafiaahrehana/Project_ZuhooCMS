package com.businessos.modules.company;

import com.businessos.enums.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySubdomain(String subdomain);

    Optional<Company> findByOwnerId(Long userId);

    boolean existsBySubdomain(String subdomain);

    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    /** Companies a prospective client can register under (public registration picker). */
    List<Company> findByStatusInOrderByCompanyNameAsc(List<CompanyStatus> statuses);

    Page<Company> findBySubscriptionPlan(String plan, Pageable pageable);

    /**
     * Combined, null-safe filtering for the platform companies list:
     * any of status / plan / keyword may be omitted. Keyword matches
     * company name, email or subdomain, case-insensitively.
     */
    @Query("""
        SELECT c FROM Company c
        WHERE (:status IS NULL OR c.status = :status)
          AND (:plan IS NULL OR c.subscriptionPlan = :plan)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.companyEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.subdomain) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Company> findFiltered(
        @Param("status") CompanyStatus status,
        @Param("plan") String plan,
        @Param("keyword") String keyword,
        Pageable pageable);

    /**
     * Fix from Phase 2: enum values are now passed as typed parameters,
     * not string literals. JPQL string literals ('TRIAL') do not reliably
     * compare against @Enumerated(STRING) columns.
     */
    @Query("""
        SELECT c FROM Company c
        WHERE c.subscriptionEnd < :today
          AND c.status IN :statuses
          AND c.deleted = false
          AND c.isPlatformTenant = false
        """)
    List<Company> findExpiredSubscriptions(
        @Param("today") LocalDate today,
        @Param("statuses") List<CompanyStatus> statuses
    );

    @Query("""
        SELECT c FROM Company c
        WHERE c.subscriptionEnd <= :cutoffDate
          AND c.subscriptionEnd >= :today
          AND c.status = :status
          AND c.trialReminderSentAt IS NULL
          AND c.deleted = false
          AND c.isPlatformTenant = false
        """)
    List<Company> findTrialExpiringBetween(
        @Param("today") LocalDate today,
        @Param("cutoffDate") LocalDate cutoffDate,
        @Param("status") CompanyStatus status
    );

    long countByStatus(CompanyStatus status);

    long countBySubscriptionPlan(String plan);

    long countByStatusAndSubscriptionEndBetween(CompanyStatus status, LocalDate from, LocalDate to);
}
