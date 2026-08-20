package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.enums.ServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    @EntityGraph(attributePaths = {"client", "client.user", "assignedEmployee", "assignedEmployee.user", "subscription", "subscription.servicePackage", "company", "companyService"})
    Optional<ServiceRequest> findByIdAndCompanyId(Long id, Long companyId);

    @EntityGraph(attributePaths = {"client", "client.user", "assignedEmployee", "assignedEmployee.user", "subscription", "subscription.servicePackage", "company", "companyService"})
    Page<ServiceRequest> findByCompanyId(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "client.user", "assignedEmployee", "assignedEmployee.user", "subscription", "subscription.servicePackage", "company", "companyService"})
    Page<ServiceRequest> findByCompanyIdAndStatus(
        Long companyId, ServiceRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "client.user", "assignedEmployee", "assignedEmployee.user", "subscription", "subscription.servicePackage", "company", "companyService"})
    Page<ServiceRequest> findByCompanyIdAndClientId(
        Long companyId, Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "client.user", "assignedEmployee", "assignedEmployee.user", "subscription", "subscription.servicePackage", "company", "companyService"})
    Page<ServiceRequest> findByCompanyIdAndAssignedEmployeeId(
        Long companyId, Long employeeId, Pageable pageable);

    long countByCompanyIdAndStatus(Long companyId, ServiceRequestStatus status);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndClientIdAndStatus(Long companyId, Long clientId, ServiceRequestStatus status);

    /** Requests an employee saw through to completion in a window - the "projects completed" KPI. */
    long countByCompanyIdAndAssignedEmployeeIdAndStatusAndCompletedAtBetween(
        Long companyId, Long employeeId, ServiceRequestStatus status,
        java.time.LocalDateTime from, java.time.LocalDateTime to);

    boolean existsByCompanyIdAndStatusNotIn(Long companyId, List<ServiceRequestStatus> closedStatuses);

    /** Prerequisite check: has this client ever completed this service before? */
    boolean existsByCompanyIdAndClientIdAndCompanyServiceIdAndStatus(
        Long companyId, Long clientId, Long companyServiceId, ServiceRequestStatus status);

    @Query("""
        SELECT r FROM ServiceRequest r
        WHERE r.company.id = :companyId
          AND r.slaDeadline < :now
          AND r.slaBreach = false
          AND r.status NOT IN :closedStatuses
          AND r.deleted = false
        """)
    List<ServiceRequest> findUnmarkedSlaBreaches(
        @Param("companyId") Long companyId,
        @Param("now") LocalDateTime now,
        @Param("closedStatuses") List<ServiceRequestStatus> closedStatuses
    );

    @Query("""
        SELECT r FROM ServiceRequest r
        WHERE r.slaDeadline < :now
          AND r.slaBreach = false
          AND r.status NOT IN :closedStatuses
          AND r.deleted = false
        """)
    List<ServiceRequest> findAllUnmarkedSlaBreaches(
        @Param("now") LocalDateTime now,
        @Param("closedStatuses") List<ServiceRequestStatus> closedStatuses
    );

    @Modifying
    @Query("""
        UPDATE ServiceRequest r SET r.slaBreach = true
        WHERE r.slaDeadline < :now
          AND r.slaBreach = false
          AND r.status NOT IN :closedStatuses
          AND r.deleted = false
        """)
    int bulkMarkSlaBreaches(
        @Param("now") LocalDateTime now,
        @Param("closedStatuses") List<ServiceRequestStatus> closedStatuses
    );

    // Newly breached open requests — read before bulkMarkSlaBreaches to notify assignees
    @Query("""
        SELECT r FROM ServiceRequest r
        WHERE r.slaDeadline < :now
          AND r.slaBreach = false
          AND r.status NOT IN :closedStatuses
          AND r.deleted = false
        """)
    List<ServiceRequest> findNewlyBreached(
        @Param("now") LocalDateTime now,
        @Param("closedStatuses") List<ServiceRequestStatus> closedStatuses
    );

    long countByCompanyIdAndSlaBreachTrueAndStatusNotIn(Long companyId, List<ServiceRequestStatus> statuses);

    List<ServiceRequest> findAllByStatusInAndCreatedAtBetween(
        List<ServiceRequestStatus> statuses, LocalDateTime start, LocalDateTime end);

    List<ServiceRequest> findAllByStatusInAndCreatedAtBefore(
        List<ServiceRequestStatus> statuses, LocalDateTime cutoff);

    Page<ServiceRequest> findByCompanyIdAndTitleContainingIgnoreCaseAndDeletedFalse(
        Long companyId, String keyword, Pageable pageable);
}
