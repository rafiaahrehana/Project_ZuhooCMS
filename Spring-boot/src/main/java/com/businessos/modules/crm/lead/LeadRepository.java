package com.businessos.modules.crm.lead;

import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByIdAndCompanyId(Long id, Long companyId);

    Page<Lead> findByCompanyId(Long companyId, Pageable pageable);

    Page<Lead> findByCompanyIdAndStatus(Long companyId, LeadStatus status, Pageable pageable);

    Page<Lead> findByCompanyIdAndAssignedToId(Long companyId, Long employeeId, Pageable pageable);


    @Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND " +
           "(LOWER(l.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(l.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           "LOWER(l.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') AND " +
           "l.deleted = false")
    Page<Lead> searchLeads(@Param("companyId") Long companyId, @Param("keyword") String keyword, Pageable pageable);

    Page<Lead> findByCompanyIdAndSource(Long companyId, LeadSource source, Pageable pageable);

    Page<Lead> findByCompanyIdAndPriority(Long companyId, Priority priority, Pageable pageable);


    @Query("SELECT DISTINCT l FROM Lead l LEFT JOIN l.tags t WHERE l.company.id = :companyId " +
           "AND (:status IS NULL OR l.status = :status) " +
           "AND (:source IS NULL OR l.source = :source) " +
           "AND (:priority IS NULL OR l.priority = :priority) " +
           "AND (:assignedToId IS NULL OR l.assignedTo.id = :assignedToId) " +
           "AND (:tagId IS NULL OR t.id = :tagId) " +
           "AND l.deleted = false " +
           "ORDER BY l.createdAt DESC")
    Page<Lead> filterLeads(@Param("companyId") Long companyId,
                           @Param("status") LeadStatus status,
                           @Param("source") LeadSource source,
                           @Param("priority") Priority priority,
                           @Param("assignedToId") Long assignedToId,
                           @Param("tagId") Long tagId,
                           Pageable pageable);

    // Unlike every sibling count query in this file, this one previously had no
    // converted=false filter. LeadStatus has no CONVERTED value - a QUALIFIED
    // lead that converts to an Opportunity keeps that status forever, so any
    // dashboard counting "Qualified Leads" by status alone only ever grew and
    // never reflected the real, still-open backlog.
    long countByCompanyIdAndStatusAndConvertedFalse(Long companyId, LeadStatus status);

    long countByCompanyIdAndSource(Long companyId, com.businessos.enums.LeadSource source);

    long countByCompanyIdAndStatusAndCreatedAtBetween(
            Long companyId, LeadStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.company.id = :companyId AND " +
           "l.status NOT IN :closedStatuses AND l.converted = false AND l.deleted = false")
    long countActiveByCompanyId(@Param("companyId") Long companyId,
                                @Param("closedStatuses") List<LeadStatus> closedStatuses);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.company.id = :companyId AND " +
           "l.assignedTo.id = :assignedToId AND l.status NOT IN :closedStatuses AND l.converted = false AND l.deleted = false")
    long countActiveByAssignee(@Param("companyId") Long companyId,
                               @Param("assignedToId") Long assignedToId,
                               @Param("closedStatuses") List<LeadStatus> closedStatuses);


    @Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND " +
           "l.expectedCloseDate BETWEEN :startDate AND :endDate AND " +
           "l.deleted = false")
    List<Lead> findLeadsCloseExpectedBetween(@Param("companyId") Long companyId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND " +
           "l.lastContactDate IS NULL AND l.status != 'DISQUALIFIED' AND l.converted = false AND " +
           "l.deleted = false")
    Page<Lead> findNeverContactedLeads(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND " +
           "l.lastContactDate < :beforeDate AND l.status NOT IN :closedStatuses AND l.converted = false AND " +
           "l.deleted = false")
    // LocalDate, matching Lead.lastContactDate - Hibernate 7 rejects comparing
    // a DATE column against a LocalDateTime parameter (the old silent coercion
    // is now an InvalidDataAccessApiUsageException).
    Page<Lead> findStalLeads(@Param("companyId") Long companyId,
                             @Param("beforeDate") java.time.LocalDate beforeDate,
                             @Param("closedStatuses") List<LeadStatus> closedStatuses,
                             Pageable pageable);

    // Cross-company (runs outside an HTTP request context - scheduler), matching
    // the convention used by LicenseExpiryScheduler. staleNotifiedAt IS NULL is
    // what makes this fire once per staleness period, not every scheduler run.
    @Query("SELECT l FROM Lead l WHERE " +
           "l.lastContactDate < :beforeDate AND l.status NOT IN :closedStatuses AND l.converted = false AND " +
           "l.deleted = false AND l.staleNotifiedAt IS NULL AND l.assignedTo IS NOT NULL")
    List<Lead> findNewlyStaleLeads(@Param("beforeDate") java.time.LocalDate beforeDate,
                                    @Param("closedStatuses") List<LeadStatus> closedStatuses);

    @Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND " +
           "l.assignedTo IS NULL AND l.status NOT IN :closedStatuses AND l.converted = false AND " +
           "l.deleted = false")
    Page<Lead> findUnassignedLeads(@Param("companyId") Long companyId,
                                   @Param("closedStatuses") List<LeadStatus> closedStatuses,
                                   Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND " +
           "l.priority = 'HIGH' AND l.status NOT IN :closedStatuses AND l.converted = false AND " +
           "l.deleted = false " +
           "ORDER BY l.expectedCloseDate ASC")
    Page<Lead> findHighPriorityOpenLeads(@Param("companyId") Long companyId,
                                         @Param("closedStatuses") List<LeadStatus> closedStatuses,
                                         Pageable pageable);

    boolean existsByEmailAndCompanyIdAndDeletedFalse(String email, Long companyId);

    boolean existsByPhoneAndCompanyIdAndDeletedFalse(String phone, Long companyId);
    long countByCompanyId(Long companyId);

    long countByCompanyIdAndConvertedTrue(Long companyId);
}
