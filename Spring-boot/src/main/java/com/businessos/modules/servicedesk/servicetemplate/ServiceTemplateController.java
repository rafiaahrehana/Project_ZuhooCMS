package com.businessos.modules.servicedesk.servicetemplate;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-templates")
@RequiredArgsConstructor
public class ServiceTemplateController {

    private final ServiceTemplateService templateService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ServiceTemplateResponse> create(@Valid @RequestBody ServiceTemplateRequest request) {
        return ResponseEntity.ok(templateService.create(request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceTemplateResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceTemplateRequest request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceTemplateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ServiceTemplateResponse>> listAll(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            Pageable pageable) {
        return ResponseEntity.ok(templateService.listAll(activeOnly, pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ServiceTemplateResponse>> listByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(templateService.listByCategory(categoryId));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.ok().build();
    }
}
