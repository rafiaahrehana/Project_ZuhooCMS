package com.businessos.modules.hrm.attendance.biometric.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BiometricDeviceService {

    BiometricDeviceResponse create(BiometricDeviceRequest request);

    BiometricDeviceResponse getById(Long id);

    BiometricDeviceResponse getByDeviceId(String deviceId);

    Page<BiometricDeviceResponse> getAll(Pageable pageable);

    Page<BiometricDeviceResponse> getByStatus(BiometricDeviceStatus status, Pageable pageable);

    List<BiometricDeviceResponse> getOnlineDevices();

    BiometricDeviceResponse update(Long id, BiometricDeviceRequest request);

    void updateStatus(Long id, BiometricDeviceStatus status);

    void updateOnlineStatus(Long id, boolean online);

    void recordSync(Long id);

    BiometricDeviceResponse delete(Long id);
}
