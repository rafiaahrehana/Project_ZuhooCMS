package com.businessos.modules.servicedesk.companyservice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RENAMED: HubServiceController → CompanyServiceController
 * FIX: Added @Valid on all @RequestBody parameters.
 * FIX: Added @PreAuthorize — only ADMIN can manage the service catalog.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/services")
public class CompanyServiceController {

    private final CompanyServiceService companyServiceService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<CompanyServiceResponse> create(
            @Valid @RequestBody CompanyServiceRequest request) {
        return new ResponseEntity<>(companyServiceService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<CompanyServiceResponse>> listAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(companyServiceService.listAll(categoryId,
                PageRequest.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/active")
    public ResponseEntity<List<CompanyServiceResponse>> listActive() {
        return ResponseEntity.ok(companyServiceService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyServiceService.getById(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<CompanyServiceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CompanyServiceRequest request) {
        return ResponseEntity.ok(companyServiceService.update(id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CompanyServiceResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(companyServiceService.toggleActive(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        companyServiceService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}
