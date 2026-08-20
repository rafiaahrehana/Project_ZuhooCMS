package com.businessos.modules.hrm.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Page<Department> findByCompanyId(Long companyId, Pageable pageable);

    List<Department> findByCompanyIdAndActiveTrue(Long companyId);

    boolean existsByCompanyIdAndName(Long companyId, String name);

    Optional<Department> findByIdAndCompanyId(Long id, Long companyId);
}
