package com.businessos.modules.hrm.asset;

import com.businessos.enums.AssetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByIdAndCompanyId(Long id, Long companyId);

    /** Locked lookup for assign() - prevents two admins assigning the same asset
     *  to two different new hires at once, matching
     *  SoftwareLicenseRepository.findByIdAndCompanyIdForUpdate(). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Asset a WHERE a.id = :id AND a.company.id = :companyId")
    Optional<Asset> findByIdAndCompanyIdForUpdate(@Param("id") Long id, @Param("companyId") Long companyId);

    Page<Asset> findByCompanyId(Long companyId, Pageable pageable);

    Page<Asset> findByCompanyIdAndStatus(Long companyId, AssetStatus status, Pageable pageable);

    List<Asset> findByCompanyIdAndAssignedToId(Long companyId, Long employeeId);

    boolean existsByCompanyIdAndSerialNumber(Long companyId, String serialNumber);

    boolean existsByCompanyIdAndAssetTag(Long companyId, String assetTag);

    @Query("""
        SELECT a FROM Asset a
        WHERE a.status <> com.businessos.enums.AssetStatus.DISPOSED
          AND a.warrantyExpiringSoonAlertedAt IS NULL
          AND a.warrantyExpiry BETWEEN :today AND :cutoff
          AND a.deleted = false
        """)
    List<Asset> findNewlyWarrantyExpiringSoon(@Param("today") LocalDate today, @Param("cutoff") LocalDate cutoff);

    @Modifying
    @Query("""
        UPDATE Asset a SET a.warrantyExpiringSoonAlertedAt = :today
        WHERE a.status <> com.businessos.enums.AssetStatus.DISPOSED
          AND a.warrantyExpiringSoonAlertedAt IS NULL
          AND a.warrantyExpiry BETWEEN :today AND :cutoff
        """)
    void bulkMarkWarrantyExpiringSoonAlerted(@Param("today") LocalDate today, @Param("cutoff") LocalDate cutoff);

    @Query("""
        SELECT a FROM Asset a
        WHERE a.status <> com.businessos.enums.AssetStatus.DISPOSED
          AND a.warrantyExpiredAlertedAt IS NULL
          AND a.warrantyExpiry < :today
          AND a.deleted = false
        """)
    List<Asset> findNewlyWarrantyExpired(@Param("today") LocalDate today);

    @Modifying
    @Query("""
        UPDATE Asset a SET a.warrantyExpiredAlertedAt = :today
        WHERE a.status <> com.businessos.enums.AssetStatus.DISPOSED
          AND a.warrantyExpiredAlertedAt IS NULL
          AND a.warrantyExpiry < :today
        """)
    void bulkMarkWarrantyExpiredAlerted(@Param("today") LocalDate today);
}
