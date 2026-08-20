package com.businessos.modules.hrm.designation;

import com.businessos.modules.hrm.department.Department;

public class DesignationMapper {

    public static DesignationResponse toDesignationResponse(Designation d) {
        Department dept = d.getDepartment();

        DesignationResponse r = new DesignationResponse();
        r.setId(d.getId());
        r.setName(d.getName());
        r.setCode(d.getCode());
        r.setLevel(d.getLevel());
        r.setDescription(d.getDescription());
        r.setActive(d.isActive());
        r.setEmploymentCategory(d.getEmploymentCategory());
        r.setDepartmentId(dept != null ? dept.getId() : null);
        r.setDepartmentName(dept != null ? dept.getName() : null);
        r.setCreatedAt(d.getCreatedAt());
        return r;
    }
}
