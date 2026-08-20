package com.businessos.modules.hrm.payroll.run;

import com.businessos.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/payroll-runs")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class PayrollRunController {

    private final PayrollRunService service;

    public record CreateRunRequest(int month, int year, String remarks) {}
    public record RejectRequest(String reason) {}
    public record PayRequest(PaymentMethod paymentMethod, String referencePrefix, LocalDate paymentDate) {}

    @GetMapping
    public ResponseEntity<List<PayrollRun>> list() {
        return ResponseEntity.ok(service.list());
    }

    /** The run for one period, or 204 if none exists yet. */
    @GetMapping("/period")
    public ResponseEntity<PayrollRun> forPeriod(@RequestParam int month, @RequestParam int year) {
        PayrollRun run = service.getForPeriod(month, year);
        return run == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(run);
    }

    @PostMapping
    public ResponseEntity<PayrollRun> create(@RequestBody CreateRunRequest request) {
        return new ResponseEntity<>(service.create(request.month(), request.year(), request.remarks()), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/recalculate")
    public ResponseEntity<PayrollRun> recalculate(@PathVariable Long id) {
        return ResponseEntity.ok(service.recalculate(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<PayrollRun> submit(@PathVariable Long id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PayrollRun> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PayrollRun> reject(@PathVariable Long id, @RequestBody RejectRequest request) {
        return ResponseEntity.ok(service.reject(id, request.reason()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PayrollRun> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PayrollRun> pay(@PathVariable Long id, @RequestBody PayRequest request) {
        return ResponseEntity.ok(service.pay(id, request.paymentMethod(), request.referencePrefix(), request.paymentDate()));
    }
}
