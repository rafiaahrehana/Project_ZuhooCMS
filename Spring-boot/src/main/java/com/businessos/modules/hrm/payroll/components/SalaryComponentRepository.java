package com.businessos.modules.hrm.payroll.components;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, Long> {
    List<SalaryComponent> findByCompanyIdOrderBySortOrderAscNameAsc(Long companyId);
    boolean existsByCompanyId(Long companyId);
    List<SalaryComponent> findByCompanyIdAndActiveTrueOrderBySortOrderAscNameAsc(Long companyId);
}
