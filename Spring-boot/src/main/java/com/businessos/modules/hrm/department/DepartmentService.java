package com.businessos.modules.hrm.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DepartmentService {

    /** ADMIN / OWNER: create a department with optional parent and budget */
    DepartmentResponse create(DepartmentRequest request);

    /** ADMIN / OWNER / EMPLOYEE: get department by id */
    DepartmentResponse getById(Long id);

    /** ADMIN / OWNER / EMPLOYEE: list all departments with pagination */
    Page<DepartmentResponse> listAll(Pageable pageable);

    /** ALL: list active departments — used for dropdowns */
    List<DepartmentResponse> listActive();

    /** ADMIN / OWNER: update department including parent and budget */
    DepartmentResponse update(Long id, DepartmentRequest request);

    /** ADMIN / OWNER: toggle department active / inactive */
    DepartmentResponse toggleActive(Long id);

    /** ADMIN / OWNER: soft-delete department (only if no employees) */
    void delete(Long id);

}
