package com.businessos.modules.hrm.attendance.biometric.device;

public enum BiometricDeviceType {
    FINGERPRINT_TERMINAL("Fingerprint Terminal - Fingerprint scanner"),
    FACIAL_RECOGNITION("Facial Recognition - Face scanner"),
    RFID_READER("RFID Reader - Card reader"),
    IRIS_SCANNER("Iris Scanner - Iris reader"),
    HYBRID("Hybrid - Multiple methods"),
    GPS_TRACKING("GPS Tracking - Location based"),
    NFC_READER("NFC Reader - NFC scanner"),
    QR_SCANNER("QR Scanner - QR code reader");

    private final String description;

    BiometricDeviceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
