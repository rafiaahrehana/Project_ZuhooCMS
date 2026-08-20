package com.businessos.modules.hrm.leave.companyleavePolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/leave-policies")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class CompanyLeavePolicyController {

    private final CompanyLeavePolicyService policyService;

    @PostMapping
    public ResponseEntity<CompanyLeavePolicyResponse> create(@RequestBody CompanyLeavePolicyRequest request) {
        return new ResponseEntity<>(policyService.create(request), HttpStatus.CREATED);
    }

    @PostMapping("/draft")
    public ResponseEntity<LeavePolicyDraftResponse> draftWithAi(@RequestBody LeavePolicyDraftRequest request) {
        return ResponseEntity.ok(policyService.draftWithAi(request));
    }

    @GetMapping
    public ResponseEntity<Page<CompanyLeavePolicyResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(policyService.listAll(PageRequest.of(page, size)));
    }

    @GetMapping("/active")
    public ResponseEntity<List<CompanyLeavePolicyResponse>> listActive() {
        return ResponseEntity.ok(policyService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyLeavePolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyLeavePolicyResponse> update(
            @PathVariable Long id,
            @RequestBody CompanyLeavePolicyRequest request) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


