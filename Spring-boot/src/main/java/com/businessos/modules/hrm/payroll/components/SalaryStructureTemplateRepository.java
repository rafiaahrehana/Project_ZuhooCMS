package com.businessos.modules.hrm.payroll.components;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryStructureTemplateRepository extends JpaRepository<SalaryStructureTemplate, Long> {
    List<SalaryStructureTemplate> findByCompanyIdOrderByStructureNameAsc(Long companyId);
}
