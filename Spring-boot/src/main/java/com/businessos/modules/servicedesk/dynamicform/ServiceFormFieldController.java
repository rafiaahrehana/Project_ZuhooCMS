package com.businessos.modules.servicedesk.dynamicform;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services/{serviceId}/form-fields")
@RequiredArgsConstructor
public class ServiceFormFieldController {

    private final ServiceFormFieldService formFieldService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ServiceFormFieldResponse> create(@PathVariable Long serviceId, @Valid @RequestBody ServiceFormFieldRequest request) {
        return ResponseEntity.ok(formFieldService.create(serviceId, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceFormFieldResponse> update(@PathVariable Long serviceId, @PathVariable Long id, @Valid @RequestBody ServiceFormFieldRequest request) {
        return ResponseEntity.ok(formFieldService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<ServiceFormFieldResponse>> listByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(formFieldService.listByService(serviceId));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long serviceId, @PathVariable Long id) {
        formFieldService.delete(id);
        return ResponseEntity.ok().build();
    }
}
