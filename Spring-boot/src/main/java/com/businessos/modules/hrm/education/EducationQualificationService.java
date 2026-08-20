package com.businessos.modules.hrm.education;

import java.util.List;

public interface EducationQualificationService {
    EducationQualificationResponse create(EducationQualificationRequest request);
    EducationQualificationResponse update(Long id, EducationQualificationRequest request);
    List<EducationQualificationResponse> listForEmployee(Long employeeId);
    void delete(Long id);
}
