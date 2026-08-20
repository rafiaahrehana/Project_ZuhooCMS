package com.businessos.modules.hrm.education;

import com.businessos.modules.hrm.employee.Employee;

public class EducationQualificationMapper {

    public static EducationQualificationResponse toResponse(EducationQualification q) {
        Employee emp = q.getEmployee();
        EducationQualificationResponse r = new EducationQualificationResponse();
        r.setId(q.getId());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(emp != null ? emp.getFullName() : null);
        r.setDegree(q.getDegree());
        r.setInstitution(q.getInstitution());
        r.setFieldOfStudy(q.getFieldOfStudy());
        r.setPassingYear(q.getPassingYear());
        r.setResult(q.getResult());
        r.setNotes(q.getNotes());
        r.setCreatedAt(q.getCreatedAt());
        return r;
    }
}
