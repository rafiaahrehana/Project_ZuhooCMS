package com.businessos.modules.ai.repository;

import com.businessos.modules.ai.entity.AiUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    @Query("SELECT COUNT(l) FROM AiUsageLog l WHERE ((:companyId IS NULL AND l.company IS NULL) OR (l.company.id = :companyId)) AND l.logDate = :date")
    long countByCompanyAndDate(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(l) FROM AiUsageLog l WHERE l.user.id = :userId AND l.logDate = :date")
    long countByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * Requests by one user since a point in time - backs the rolling hourly
     * per-user quota (ai.hourly-user-limit). Uses createdAt, not logDate, so the
     * window slides continuously instead of resetting at midnight.
     */
    @Query("SELECT COUNT(l) FROM AiUsageLog l WHERE l.user.id = :userId AND l.createdAt >= :since")
    long countByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("""
        SELECT l.aiFeature, COUNT(l), SUM(l.inputTokens + l.outputTokens)
        FROM AiUsageLog l
        WHERE ((:companyId IS NULL AND l.company IS NULL) OR (l.company.id = :companyId)) AND l.logDate BETWEEN :from AND :to
        GROUP BY l.aiFeature
        ORDER BY COUNT(l) DESC
        """)
    List<Object[]> aggregateByFeature(
        @Param("companyId") Long companyId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(l.inputTokens + l.outputTokens), 0)
        FROM AiUsageLog l
        WHERE ((:companyId IS NULL AND l.company IS NULL) OR (l.company.id = :companyId)) AND l.logDate BETWEEN :from AND :to
        """)
    Long totalTokensForPeriod(
        @Param("companyId") Long companyId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("SELECT COALESCE(AVG(l.executionTimeMs), 0) FROM AiUsageLog l WHERE ((:companyId IS NULL AND l.company IS NULL) OR (l.company.id = :companyId)) AND l.logDate = :date")
    Double avgResponseTimeMs(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    Page<AiUsageLog> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
}
