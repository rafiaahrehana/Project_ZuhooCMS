package com.businessos.modules.servicedesk.servicereview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * BUG-FIX SUMMARY
 * ───────────────
 * TENANT ISOLATION BUG — Two new scoped queries added:
 *
 * 1. findByCompanyId(companyId, pageable) — ServiceReviewServiceImpl.listAll() called
 *    findAll(pageable) which returns reviews from ALL tenants. Company A admin could
 *    see Company B's reviews. Replaced with companyId-scoped version.
 *
 * 2. findByIdAndCompanyId(id, companyId) — ServiceReviewServiceImpl.getById() called
 *    findById(id) with no tenant check. Any authenticated platformuser knowing an ID could
 *    read another tenant's review. Replaced with tenant-scoped lookup.
 *
 * Existing findByHubServiceId, findAverageRatingByServiceId are intentionally public-facing
 * (published reviews for a service) and don't need company scoping per the controller.
 */
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {

    Page<ServiceReview> findByCompanyId(Long companyId, Pageable pageable); // added

    Optional<ServiceReview> findByIdAndCompanyId(Long id, Long companyId);  // added

    Page<ServiceReview> findByHubServiceId(Long hubServiceId, Pageable pageable);

    Optional<ServiceReview> findByServiceRequestIdAndClientId(Long serviceRequestId, Long clientId);

    @Query("SELECT AVG(r.rating) FROM ServiceReview r " +
            "WHERE r.hubService.id = :serviceId " +
            "AND r.published = true AND r.deleted = false")
    Optional<Double> findAverageRatingByServiceId(@Param("serviceId") Long serviceId);

    @Query("SELECT AVG(r.rating) FROM ServiceReview r " +
            "WHERE r.company.id = :companyId " +
            "AND r.published = true AND r.deleted = false")
    Optional<Double> findAverageRatingByCompanyId(@Param("companyId") Long companyId);

    /**
     * Average client rating of the work a given staff member delivered in a window.
     * Feeds the customer-satisfaction KPI on a performance review. Unpublished
     * reviews are excluded, matching the two queries above.
     */
    @Query("""
        SELECT AVG(r.rating) FROM ServiceReview r
        WHERE r.company.id = :companyId
          AND r.staff.id = :staffId
          AND r.createdAt >= :from AND r.createdAt < :to
          AND r.published = true AND r.deleted = false
        """)
    Optional<Double> findAverageRatingByStaffInRange(
        @Param("companyId") Long companyId,
        @Param("staffId") Long staffId,
        @Param("from") java.time.LocalDateTime from,
        @Param("to") java.time.LocalDateTime to);
}
