package com.businessos.auth.platformuser;

import com.businessos.auth.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("authPlatformUserController")
@RequiredArgsConstructor
@RequestMapping("/api/platform/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody PlatformUserRequest request) {
        return new ResponseEntity<>(platformUserService.createPlatformUser(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(platformUserService.list(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(platformUserService.getById(id));
    }

    // NOTE: not @Valid here - PlatformUserRequest.password is @NotBlank for create,
    // but on update the password should be optional (only changed if provided).
    // Other required fields are still checked in PlatformUserService.update().
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody PlatformUserRequest request) {
        return ResponseEntity.ok(platformUserService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deactivate(@PathVariable Long id) {
        platformUserService.deactivate(id);
        return ResponseEntity.ok("Platform user deactivated successfully");
    }
}
