package com.businessos.modules.hrm.leave.leavebalance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/leave-balances")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @PostMapping
    public ResponseEntity<LeaveBalanceResponse> create(@Valid @RequestBody LeaveBalanceRequest request) {
        return new ResponseEntity<>(leaveBalanceService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveBalanceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LeaveBalanceRequest request) {
        return ResponseEntity.ok(leaveBalanceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        leaveBalanceService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }

    /**
     * The caller's own balances - what the employee dashboard renders.
     * Separate from GET / because that returns every employee's balances.
     */
    @GetMapping("/my")
    public ResponseEntity<java.util.List<LeaveBalanceResponse>> listMine(
            @RequestParam(required = false) Integer year) {
        int resolvedYear = year != null ? year : Year.now().getValue();
        return ResponseEntity.ok(leaveBalanceService.listMine(resolvedYear));
    }

    @GetMapping
    public ResponseEntity<Page<LeaveBalanceResponse>> listAll(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int resolvedYear = year != null ? year : Year.now().getValue();
        return ResponseEntity.ok(leaveBalanceService.listAll(resolvedYear,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}
