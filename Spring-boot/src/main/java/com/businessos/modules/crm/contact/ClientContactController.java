package com.businessos.modules.crm.contact;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clients/{clientId}/contacts")
public class ClientContactController {

    private final ClientContactService clientContactService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ClientContactResponse> create(
            @PathVariable Long clientId,
            @Valid @RequestBody ClientContactRequest request) {
        return new ResponseEntity<>(clientContactService.create(clientId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<ClientContactResponse>> listByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientContactService.listByClient(clientId));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<ClientContactResponse> getById(@PathVariable Long clientId, @PathVariable Long id) {
        return ResponseEntity.ok(clientContactService.getById(clientId, id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}")
    public ResponseEntity<ClientContactResponse> update(
            @PathVariable Long clientId,
            @PathVariable Long id,
            @Valid @RequestBody ClientContactRequest request) {
        return ResponseEntity.ok(clientContactService.update(clientId, id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/primary")
    public ResponseEntity<ClientContactResponse> markPrimary(@PathVariable Long clientId, @PathVariable Long id) {
        return ResponseEntity.ok(clientContactService.markPrimary(clientId, id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long clientId, @PathVariable Long id) {
        clientContactService.delete(clientId, id);
        return ResponseEntity.noContent().build();
    }
}
