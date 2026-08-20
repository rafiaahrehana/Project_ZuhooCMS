package com.businessos.modules.hrm.payroll.loan;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/loans")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class LoanAdvanceController {

    private final LoanAdvanceService service;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<LoanAdvanceResponse> create(@Valid @RequestBody CreateLoanRequest request) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LoanAdvanceResponse>> list() {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LoanAdvanceResponse>> listForEmployee(@PathVariable Long employeeId) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        return ResponseEntity.ok(service.listForEmployee(employeeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanAdvanceResponse> getById(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/{id}/repayments")
    public ResponseEntity<List<LoanRepaymentResponse>> repaymentHistory(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        return ResponseEntity.ok(service.repaymentHistory(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<LoanAdvanceResponse> cancel(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        return ResponseEntity.ok(service.cancel(id));
    }
}
