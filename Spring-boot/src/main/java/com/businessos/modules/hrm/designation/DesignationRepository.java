package com.businessos.modules.hrm.designation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignationRepository extends JpaRepository<Designation, Long> {

    Optional<Designation> findByIdAndCompanyId(Long id, Long companyId);

    Page<Designation> findByCompanyId(Long companyId, Pageable pageable);

    List<Designation> findByCompanyIdAndActiveTrueOrderByLevelAsc(Long companyId);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndCodeAndIdNot(Long companyId, String code, Long excludeId);
}
