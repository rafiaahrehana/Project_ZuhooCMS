package com.businessos.modules.hrm.education;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EducationQualificationRepository extends JpaRepository<EducationQualification, Long> {

    Optional<EducationQualification> findByIdAndCompanyId(Long id, Long companyId);

    List<EducationQualification> findByCompanyIdAndEmployeeIdOrderByPassingYearDesc(Long companyId, Long employeeId);
}
