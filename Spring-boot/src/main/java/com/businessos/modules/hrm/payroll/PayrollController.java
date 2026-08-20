package com.businessos.modules.hrm.payroll;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.security.SecurityUtil;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.enums.PaymentMethod;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/payroll")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class PayrollController {

    private final PayrollService payrollService;
    private final AuthorizationService authorizationService;
    private final SecurityUtil securityUtil;
    private final EmployeeRepository employeeRepository;

    @PostMapping
    public ResponseEntity<PayrollResponse> create(@Valid @RequestBody CreatePayrollRequest request) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        return new ResponseEntity<>(payrollService.create(request), HttpStatus.CREATED);
    }

    /**
     * Generates a DRAFT payroll for every active employee who has a salary
     * structure and doesn't already have one for this period - the batch
     * "run payroll" action every real company needs monthly.
     */
    @PostMapping("/generate")
    public ResponseEntity<BulkPayrollResult> generateForAllEmployees(
            @RequestParam int month,
            @RequestParam int year) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        return ResponseEntity.ok(payrollService.generateForAllEmployees(month, year));
    }

    @GetMapping
    public ResponseEntity<Page<PayrollResponse>> listByPeriod(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        return ResponseEntity.ok(payrollService.listByPeriod(month, year,
                PageRequest.of(page, size, Sort.by("createdAt"))));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<PayrollResponse>> listForEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        
        // Allow if user has PAYROLL_VIEW permission
        if (!authorizationService.hasPermission(PermissionCode.PAYROLL_VIEW)) {
            // Otherwise, check if they are requesting their own payroll records
            com.businessos.auth.user.User currentUser = securityUtil.getCurrentUser();
            if (currentUser == null) {
                throw new ForbiddenException("Access denied");
            }
            Employee currentEmp = employeeRepository.findByUserId(currentUser.getId()).orElse(null);
            if (currentEmp == null || !currentEmp.getId().equals(employeeId)) {
                throw new ForbiddenException("Access denied: You can only view your own payroll history");
            }
        }

        return ResponseEntity.ok(payrollService.listForEmployee(employeeId,
                PageRequest.of(page, size, Sort.by("payYear").descending()
                        .and(Sort.by("payMonth").descending()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollResponse> getById(@PathVariable Long id) {
        // Fetch the payroll to check permissions
        PayrollResponse res = payrollService.getById(id);
        
        if (!authorizationService.hasPermission(PermissionCode.PAYROLL_VIEW)) {
            com.businessos.auth.user.User currentUser = securityUtil.getCurrentUser();
            if (currentUser == null) {
                throw new ForbiddenException("Access denied");
            }
            Employee currentEmp = employeeRepository.findByUserId(currentUser.getId()).orElse(null);
            if (currentEmp == null || !currentEmp.getId().equals(res.getEmployeeId())) {
                throw new ForbiddenException("Access denied: You can only view your own payroll records");
            }
        }
        
        return ResponseEntity.ok(res);
    }

    /**
     * Payslip PDF for one payroll record.
     *
     * No permission check here on purpose: the service enforces "PAYROLL_VIEW
     * sees anyone's, everyone else only their own", so an employee can download
     * their own payslip without being granted a company-wide payroll
     * permission.
     */
    @GetMapping(value = "/{id}/payslip", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> payslipPdf(@PathVariable Long id) {
        PayslipDocument doc = payrollService.generatePayslipPdf(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + doc.fileName() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(doc.content());
    }

    /**
     * Bank disbursement sheet for a pay period - APPROVED payrolls only.
     * Finance uploads this to the bank's corporate portal for bulk salary
     * transfer, then returns here to mark each row paid. Downloading it does
     * not move any money and does not change payroll status.
     */
    @GetMapping(value = "/disbursement", produces = "text/csv")
    public ResponseEntity<String> disbursementCsv(
            @RequestParam int month,
            @RequestParam int year) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        String csv = payrollService.buildDisbursementCsv(month, year);
        String filename = String.format("salary-disbursement-%04d-%02d.csv", year, month);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<PayrollResponse> approve(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_APPROVE);
        return ResponseEntity.ok(payrollService.approve(id));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<PayrollResponse> markPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String paymentReference,
            @RequestParam(required = false) PaymentMethod paymentMethod) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_APPROVE);
        return ResponseEntity.ok(payrollService.markPaid(id, paymentReference, paymentMethod));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        payrollService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


