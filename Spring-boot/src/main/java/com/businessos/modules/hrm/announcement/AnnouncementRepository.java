package com.businessos.modules.hrm.announcement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Optional<Announcement> findByIdAndCompanyId(Long id, Long companyId);

    Page<Announcement> findByCompanyId(Long companyId, Pageable pageable);

    @Query("""
        SELECT a FROM Announcement a
        WHERE a.company.id = :companyId
          AND a.published = true
          AND (a.expiresAt IS NULL OR a.expiresAt > :now)
          AND a.deleted = false
        ORDER BY a.publishedAt DESC
        """)
    List<Announcement> findActiveByCompanyId(
        @Param("companyId") Long companyId,
        @Param("now") LocalDateTime now);

    // Cross-company (runs outside an HTTP request context - scheduler), matching
    // the convention used by LicenseExpiryScheduler.
    List<Announcement> findByPublishedFalseAndDeletedFalseAndScheduledAtLessThanEqual(LocalDateTime now);
}
