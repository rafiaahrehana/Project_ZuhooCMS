package com.businessos.modules.hrm.attendance.attendance;

public enum AttendanceMethod {
    MANUAL("Manual - Admin entry"),
    FINGERPRINT("Fingerprint - Biometric"),
    FACIAL("Facial - Face recognition"),
    RFID("RFID - Card based"),
    IRIS("Iris - Iris recognition"),
    GPS("GPS - Location based"),
    NFC("NFC - Phone tap"),
    QR_CODE("QR Code - QR scan"),
    OTHER("Other - Custom method");

    private final String description;

    AttendanceMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}