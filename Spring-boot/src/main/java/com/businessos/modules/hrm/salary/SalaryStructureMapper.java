package com.businessos.modules.hrm.salary;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class SalaryStructureMapper {

    public static SalaryStructureResponse toSalaryStructureResponse(SalaryStructure s) {
        Employee emp = s.getEmployee();
        User empUser = emp != null ? emp.getUser() : null;
        User approver = s.getApprovedBy();
        SalaryStructureResponse r = new SalaryStructureResponse();
        r.setId(s.getId());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setEffectiveFrom(s.getEffectiveFrom());
        r.setEffectiveTo(s.getEffectiveTo());
        r.setGrossSalary(s.getGrossSalary());
        r.setBasicSalary(s.getBasicSalary());
        r.setHouseRent(s.getHouseRent());
        r.setMedicalAllowance(s.getMedicalAllowance());
        r.setTransportAllowance(s.getTransportAllowance());
        r.setFoodAllowance(s.getFoodAllowance());
        r.setSpecialAllowance(s.getSpecialAllowance());
        r.setProvidentFund(s.getProvidentFund());
        r.setTaxDeduction(s.getTaxDeduction());
        r.setNetSalary(s.computeNetSalary());
        r.setNotes(s.getNotes());
        r.setApprovedById(approver != null ? approver.getId() : null);
        r.setApprovedByName(approver != null ? approver.getFullName() : null);
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }
}
