package com.businessos.modules.crm.client;

import com.businessos.enums.ClientStatus;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        return new ResponseEntity<>(clientService.create(request), HttpStatus.CREATED);
    }

    /** Public, unauthenticated self-registration - see SecurityConfig for the permitAll rule. */
    @PostMapping("/public/register")
    public ResponseEntity<ClientResponse> registerPublic(@Valid @RequestBody PublicClientRegisterRequest request) {
        return new ResponseEntity<>(clientService.registerPublic(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<ClientResponse>> listAll(
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(clientService.listAll(status, tagId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/active")
    public ResponseEntity<List<ClientResponse>> listActive() {
        return ResponseEntity.ok(clientService.listActive());
    }

    @GetMapping("/me")
    public ResponseEntity<ClientResponse> getMyProfile() {
        return ResponseEntity.ok(clientService.getMyProfile());
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/me")
    public ResponseEntity<ClientResponse> updateMyProfile(@Valid @RequestBody UpdateMyClientProfileRequest request) {
        return ResponseEntity.ok(clientService.updateMyProfile(request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getById(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}")
    public ResponseEntity<ClientResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    /**
     * Creates a portal login for this client and emails a one-time
     * set-password link. Safe to call again to re-send an expired invite.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/{id}/invite-portal")
    public ResponseEntity<ClientResponse> inviteToPortal(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.inviteToPortal(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}