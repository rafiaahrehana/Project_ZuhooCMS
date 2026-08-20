package com.businessos.modules.hrm.attendance.biometric.device;

public class BiometricDeviceMapper {

    public static BiometricDeviceResponse toResponse(BiometricDevice entity) {
        if (entity == null) {
            return null;
        }

        return BiometricDeviceResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .deviceName(entity.getDeviceName())
                .deviceType(entity.getDeviceType())
                .deviceId(entity.getDeviceId())
                .ipAddress(entity.getIpAddress())
                .portNumber(entity.getPortNumber())
                .location(entity.getLocation())
                .department(entity.getDepartment())
                .status(entity.getStatus())
                .matchThreshold(entity.getMatchThreshold())
                .enabledForCheckIn(entity.isEnabledForCheckIn())
                .enabledForCheckOut(entity.isEnabledForCheckOut())
                .lastSyncTime(entity.getLastSyncTime())
                .lastHealthCheckTime(entity.getLastHealthCheckTime())
                .isOnline(entity.isOnline())
                .manufacturer(entity.getManufacturer())
                .model(entity.getModel())
                .firmwareVersion(entity.getFirmwareVersion())
                .totalEnrollments(entity.getTotalEnrollments())
                .maxEnrollments(entity.getMaxEnrollments())
                .notes(entity.getNotes())
                .build();
    }

    public static BiometricDevice toEntity(BiometricDeviceRequest request) {
        if (request == null) {
            return null;
        }

        BiometricDevice.BiometricDeviceBuilder builder = BiometricDevice.builder()
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .deviceId(request.getDeviceId())
                .ipAddress(request.getIpAddress())
                .portNumber(request.getPortNumber())
                .location(request.getLocation())
                .department(request.getDepartment())
                .notes(request.getNotes());

        if (request.getMatchThreshold() != null) {
            builder.matchThreshold(request.getMatchThreshold());
        }
        if (request.getEnabledForCheckIn() != null) {
            builder.enabledForCheckIn(request.getEnabledForCheckIn());
        }
        if (request.getEnabledForCheckOut() != null) {
            builder.enabledForCheckOut(request.getEnabledForCheckOut());
        }

        return builder.build();
    }
}
