package com.businessos.modules.hrm.payroll.loan;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class LoanAdvanceMapper {

    /** A terminated employee's lazy proxy throws on any field access beyond its id - loan history must still display. */
    private static User safeUser(Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    public static LoanAdvanceResponse toResponse(LoanAdvance loan) {
        Employee emp = loan.getEmployee();
        User empUser = safeUser(emp);
        LoanAdvanceResponse r = new LoanAdvanceResponse();
        r.setId(loan.getId());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setType(loan.getType());
        r.setPrincipalAmount(loan.getPrincipalAmount());
        r.setDisbursedDate(loan.getDisbursedDate());
        r.setMonthlyInstallment(loan.getMonthlyInstallment());
        r.setRemainingBalance(loan.getRemainingBalance());
        r.setStatus(loan.getStatus());
        r.setReason(loan.getReason());
        r.setNotes(loan.getNotes());
        r.setCreatedAt(loan.getCreatedAt());
        return r;
    }

    public static LoanRepaymentResponse toResponse(LoanRepayment repayment) {
        LoanRepaymentResponse r = new LoanRepaymentResponse();
        r.setId(repayment.getId());
        r.setPayrollId(repayment.getPayroll() != null ? repayment.getPayroll().getId() : null);
        if (repayment.getPayroll() != null) {
            r.setPayMonth(repayment.getPayroll().getPayMonth());
            r.setPayYear(repayment.getPayroll().getPayYear());
        }
        r.setAmount(repayment.getAmount());
        r.setPaidDate(repayment.getPaidDate());
        r.setBalanceAfter(repayment.getBalanceAfter());
        return r;
    }
}
