package com.businessos.modules.hrm.payroll;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class PayrollMapper {

    /**
     * getUser() forces Hibernate to initialize the Employee proxy, which
     * re-runs the entity's own @SQLRestriction("deleted = false") - a
     * terminated (soft-deleted) employee's proxy throws EntityNotFoundException
     * on any field access beyond its id. Payroll history must stay readable
     * for someone who has since left, so this falls back to a blank name
     * rather than 500ing the whole payroll list.
     */
    private static User safeUser(Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    public static PayrollResponse toPayrollResponse(Payroll p) {
        Employee emp = p.getEmployee();
        User empUser = safeUser(emp);
        Employee approver = p.getApprovedBy();
        User approverUser = safeUser(approver);
        PayrollResponse r = new PayrollResponse();
        r.setId(p.getId());
        r.setPayMonth(p.getPayMonth());
        r.setPayYear(p.getPayYear());
        r.setBasicSalary(p.getBasicSalary());
        r.setHouseRent(p.getHouseRent());
        r.setMedicalAllowance(p.getMedicalAllowance());
        r.setTransportAllowance(p.getTransportAllowance());
        r.setFoodAllowance(p.getFoodAllowance());
        r.setSpecialAllowance(p.getSpecialAllowance());
        r.setBonus(p.getBonus());
        r.setBillableHours(p.getBillableHours());
        r.setBillableRate(p.getBillableRate());
        r.setBillablePay(p.getBillablePay());
        r.setOvertimeHours(p.getOvertimeHours());
        r.setOvertimeRate(p.getOvertimeRate());
        r.setOvertimePay(p.getOvertimePay());
        r.setDeductions(p.getDeductions());
        r.setTaxDeduction(p.getTaxDeduction());
        r.setInsuranceDeduction(p.getInsuranceDeduction());
        r.setProvidentFundDeduction(p.getProvidentFundDeduction());
        r.setAttendanceDeduction(p.getAttendanceDeduction());
        r.setOtherEarnings(p.getOtherEarnings());
        r.setOtherDeductions(p.getOtherDeductions());
        r.setLoanAdvanceId(p.getLoanAdvance() != null ? p.getLoanAdvance().getId() : null);
        r.setLoanDeductionAmount(p.getLoanDeductionAmount());
        r.setAbsentDays(p.getAbsentDays());
        r.setNetSalary(p.getNetSalary());
        r.setStatus(p.getStatus());
        r.setPaymentReference(p.getPaymentReference());
        r.setPaymentMethod(p.getPaymentMethod());
        r.setPaidAt(p.getPaidAt());
        r.setNotes(p.getNotes());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setApprovedById(approver != null ? approver.getId() : null);
        r.setApprovedByName(approverUser != null ? approverUser.getFullName() : null);
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
