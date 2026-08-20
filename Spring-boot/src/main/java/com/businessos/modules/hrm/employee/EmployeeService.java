package com.businessos.modules.hrm.employee;

import com.businessos.enums.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    /** ADMIN / OWNER: onboard a new employee — creates User + Employee in one transaction */
    EmployeeResponse create(CreateEmployeeRequest request);

    /** ADMIN / OWNER: get full employee details by id */
    EmployeeResponse getById(Long id);

    /** EMPLOYEE: get own profile */
    EmployeeResponse getMyProfile();

    /** EMPLOYEE: self-edit a narrow subset of own profile fields - see SelfUpdateEmployeeRequest */
    EmployeeResponse updateMyProfile(SelfUpdateEmployeeRequest request);

    /**
     * ADMIN / OWNER: list all employees, optionally filtered by department, status, or search term.
     * excludeOwner=true drops the company owner's own auto-created Employee record -
     * used by the HRM Employees admin page; other callers (asset assignment, payroll,
     * offboarding pickers) pass false to keep including the owner as before.
     */
    Page<EmployeeResponse> listAll(Long departmentId, EmploymentStatus status, String search, boolean excludeOwner, Pageable pageable);

    default Page<EmployeeResponse> listAll(Long departmentId, boolean excludeOwner, Pageable pageable) {
        return listAll(departmentId, null, null, excludeOwner, pageable);
    }

    /** ADMIN / OWNER: update employee profile fields and relationships */
    EmployeeResponse update(Long id, UpdateEmployeeRequest request);

    /** ADMIN / OWNER: terminate employment — sets requeststatus TERMINATED and soft-deletes platformuser */
    void terminate(Long id);

    /** ADMIN / OWNER: get total number of employees for the current company */
    long getEmployeeCount();

    /** Checks if a platformuser is an employee of the current company */
    boolean isEmployee(Long userId);
}
