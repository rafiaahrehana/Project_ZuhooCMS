package com.businessos.modules.servicedesk.companyservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    Optional<ServicePackage> findByIdAndCompanyId(Long id, Long companyId);

    Page<ServicePackage> findByCompanyId(Long companyId, Pageable pageable);

    List<ServicePackage> findByCompanyIdAndActiveTrue(Long companyId);

    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long excludeId);

    boolean existsByCompanyIdAndName(Long companyId, String name);

    /**
     * Returns packages that include a given service — used to check
     * whether deactivating a service breaks any active package.
     */
    @Query("""
        SELECT p FROM ServicePackage p
        JOIN p.services s
        WHERE p.company.id = :companyId
          AND s.id = :serviceId
          AND p.active = true
          AND p.deleted = false
        """)
    List<ServicePackage> findActivePackagesContainingService(
        @Param("companyId") Long companyId,
        @Param("serviceId") Long serviceId
    );
}
