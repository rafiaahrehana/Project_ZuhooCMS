package com.businessos.modules.hrm.attendance.biometric.device;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company/biometric/devices")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class BiometricDeviceController {

    private final BiometricDeviceService deviceService;

    @PostMapping
    public ResponseEntity<BiometricDeviceResponse> create(@Valid @RequestBody BiometricDeviceRequest request) {
        return new ResponseEntity<>(deviceService.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BiometricDeviceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getById(id));
    }

    @GetMapping("/device-id/{deviceId}")
    public ResponseEntity<BiometricDeviceResponse> getByDeviceId(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getByDeviceId(deviceId));
    }

    @GetMapping
    public ResponseEntity<Page<BiometricDeviceResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(deviceService.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<BiometricDeviceResponse>> getByStatus(
            @PathVariable BiometricDeviceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(deviceService.getByStatus(status, PageRequest.of(page, size)));
    }

    @GetMapping("/online")
    public ResponseEntity<List<BiometricDeviceResponse>> getOnlineDevices() {
        return ResponseEntity.ok(deviceService.getOnlineDevices());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BiometricDeviceResponse> update(@PathVariable Long id, @Valid @RequestBody BiometricDeviceRequest request) {
        return ResponseEntity.ok(deviceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam BiometricDeviceStatus status) {
        deviceService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/online-status")
    public ResponseEntity<Void> updateOnlineStatus(@PathVariable Long id, @RequestParam boolean online) {
        deviceService.updateOnlineStatus(id, online);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Void> recordSync(@PathVariable Long id) {
        deviceService.recordSync(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BiometricDeviceResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.delete(id));
    }
}
