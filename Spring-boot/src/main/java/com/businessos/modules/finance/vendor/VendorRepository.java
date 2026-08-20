package com.businessos.modules.finance.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByIdAndCompanyId(Long id, Long companyId);

    Page<Vendor> findByCompanyId(Long companyId, Pageable pageable);

    Page<Vendor> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String name, Pageable pageable);

    List<Vendor> findByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}
