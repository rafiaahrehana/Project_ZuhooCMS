package com.businessos.modules.company;

import com.businessos.enums.CompanyStatus;
import com.businessos.shared.exception.UnauthorizedException;
import com.businessos.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CompanyService companyService;
    private final SecurityUtil   securityUtil;

    @GetMapping("/public/{subdomain}")
    public ResponseEntity<CompanyPublicResponse> getPublic(@PathVariable String subdomain) {
        return ResponseEntity.ok(companyService.getBySubdomain(subdomain));
    }

    /** Companies a prospective client can pick from on the public client registration page. */
    @GetMapping("/public/list")
    public ResponseEntity<java.util.List<CompanyPublicResponse>> getPublicList() {
        return ResponseEntity.ok(companyService.getPublicList());
    }

    /** Active services of a company for its public portal page. */
    @GetMapping("/public/{subdomain}/services")
    public ResponseEntity<java.util.List<com.businessos.modules.servicedesk.companyservice.CompanyServiceResponse>> getPublicServices(@PathVariable String subdomain) {
        return ResponseEntity.ok(companyService.getPublicServices(subdomain));
    }

    @GetMapping("/me")
    public ResponseEntity<CompanyResponse> getMyCompany() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null)
            throw new UnauthorizedException("No company associated with this account");
        return ResponseEntity.ok(companyService.getById(companyId));
    }

    @PatchMapping("/me")
    public ResponseEntity<CompanyResponse> updateMyCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null)
            throw new UnauthorizedException("No company associated with this account");
        return ResponseEntity.ok(companyService.update(companyId, request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'PLATFORM_ACCOUNTANT', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'MARKETING_MANAGER')")
    @PostMapping("/admin")
    public ResponseEntity<CompanyResponse> registerByAdmin(@Valid @RequestBody RegisterCompanyRequest request) {
        return new ResponseEntity<>(companyService.registerByAdmin(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'PLATFORM_ACCOUNTANT', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'MARKETING_MANAGER')")
    @GetMapping
    public ResponseEntity<Page<CompanyResponse>> listAll(
            @RequestParam(required = false) CompanyStatus status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(companyService.listAll(status, plan, keyword,
                PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("createdAt").descending())));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'PLATFORM_ACCOUNTANT', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'MARKETING_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SALES_MANAGER', 'PLATFORM_ACCOUNTANT')")
    @PatchMapping("/{id}/plan")
    public ResponseEntity<CompanyResponse> changePlan(
            @PathVariable Long id,
            @RequestParam String plan,
            @RequestParam(required = false) java.math.BigDecimal amountPaid,
            @RequestParam(required = false) String transactionRef) {
        return ResponseEntity.ok(companyService.changePlan(id, plan, amountPaid, transactionRef));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'PLATFORM_ACCOUNTANT')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<CompanyResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam CompanyStatus status) {
        return ResponseEntity.ok(companyService.changeStatus(id, status));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deactivate(@PathVariable Long id) {
        companyService.deactivate(id);
        return ResponseEntity.ok("Company deactivated successfully");
    }
}
