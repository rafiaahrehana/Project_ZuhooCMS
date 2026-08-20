package com.businessos.modules.hrm.department;

import com.businessos.modules.hrm.employee.Employee;

public  class DepartmentMapper {

    public static DepartmentResponse toResponse(Department d) {
        Employee employee = d.getEmployee();
        Department parent = d.getDepartment();
        DepartmentResponse r = new DepartmentResponse();
        r.setId(d.getId());
        r.setName(d.getName());
        r.setCode(d.getCode());
        r.setDescription(d.getDescription());
        r.setActive(d.isActive());
        r.setBudget(d.getBudget());
        r.setParentDepartmentId(parent != null ? parent.getId() : null);
        r.setParentDepartmentName(parent != null ? parent.getName() : null);
        r.setHeadEmployeeId(employee != null ? employee.getId() : null);
        r.setHeadEmployeeName(employee != null && employee.getUser() != null ? employee.getUser().getFullName() : null);
        r.setEmployeeCount(d.getEmployees() != null ? (long) d.getEmployees().size() : 0L);
        r.setCreatedAt(d.getCreatedAt());
        return r;
    }
}
