package com.businessos.modules.hrm.salary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SalaryStructureService {
    SalaryStructureResponse create(SalaryStructureRequest request);
    SalaryStructureResponse update(Long id, SalaryStructureRequest request);
    SalaryStructureResponse getById(Long id);
    SalaryStructureResponse getActiveForEmployee(Long employeeId);
    Page<SalaryStructureResponse> listAll(Pageable pageable);
    Page<SalaryStructureResponse> listForEmployee(Long employeeId, Pageable pageable);
    List<SalaryStructureResponse> historyForEmployee(Long employeeId);
    void delete(Long id);
}
