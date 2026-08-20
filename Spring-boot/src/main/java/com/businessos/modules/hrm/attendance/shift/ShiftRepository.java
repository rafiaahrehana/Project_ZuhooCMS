package com.businessos.modules.hrm.attendance.shift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByIdAndCompanyId(Long id, Long companyId);

    Page<Shift> findByCompanyId(Long companyId, Pageable pageable);

    List<Shift> findByCompanyIdAndActiveTrue(Long companyId);

    boolean existsByCompanyIdAndName(Long companyId, String name);
}
