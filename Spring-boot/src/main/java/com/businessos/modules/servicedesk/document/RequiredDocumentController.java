package com.businessos.modules.servicedesk.document;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services/{serviceId}/required-documents")
@RequiredArgsConstructor
public class RequiredDocumentController {

    private final RequiredDocumentService requiredDocumentService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<RequiredDocumentResponse> create(@PathVariable Long serviceId, @Valid @RequestBody RequiredDocumentRequest request) {
        return ResponseEntity.ok(requiredDocumentService.create(serviceId, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<RequiredDocumentResponse> update(@PathVariable Long serviceId, @PathVariable Long id, @Valid @RequestBody RequiredDocumentRequest request) {
        return ResponseEntity.ok(requiredDocumentService.update(id, request));
    }

    // No role gate - a client filling out a request needs to see the checklist
    // to know what to upload (mirrors ServiceFormFieldController.listByService).
    @GetMapping
    public ResponseEntity<List<RequiredDocumentResponse>> listByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(requiredDocumentService.listByService(serviceId));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long serviceId, @PathVariable Long id) {
        requiredDocumentService.delete(id);
        return ResponseEntity.ok().build();
    }
}
