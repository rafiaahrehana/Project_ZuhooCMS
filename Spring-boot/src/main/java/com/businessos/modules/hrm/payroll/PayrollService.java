package com.businessos.modules.hrm.payroll;

import com.businessos.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PayrollService {
    PayrollResponse create(CreatePayrollRequest request);
    PayrollResponse getById(Long id);
    Page<PayrollResponse> listByPeriod(int month, int year, Pageable pageable);
    Page<PayrollResponse> listForEmployee(Long employeeId, Pageable pageable);
    PayrollResponse approve(Long id);
    /** CSV of APPROVED payrolls for a period, for bank bulk-salary upload. */
    String buildDisbursementCsv(int month, int year);

    PayrollResponse markPaid(Long id, String paymentReference, PaymentMethod paymentMethod);
    void delete(Long id);

    /** Creates a DRAFT payroll for every active employee with a salary structure who doesn't already have one for this period. */
    BulkPayrollResult generateForAllEmployees(int month, int year);

    /**
     * Payslip PDF for one payroll record.
     *
     * Enforces the same rule as getById: PAYROLL_VIEW sees anyone's, everyone
     * else only their own. The check lives here rather than in the controller
     * so it cannot be bypassed by a second caller.
     */
    PayslipDocument generatePayslipPdf(Long id);
}
