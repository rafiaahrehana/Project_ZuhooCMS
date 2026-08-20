export type AttendanceStatus =
  | 'PRESENT' | 'LATE' | 'ABSENT' | 'ON_LEAVE' | 'HALF_DAY'
  | 'WORK_FROM_HOME' | 'WEEKEND' | 'HOLIDAY' | 'PARTIAL_DAY' | 'UNMARKED';

export const ATTENDANCE_STATUSES: AttendanceStatus[] = [
  'PRESENT', 'LATE', 'ABSENT', 'ON_LEAVE', 'HALF_DAY',
  'WORK_FROM_HOME', 'WEEKEND', 'HOLIDAY', 'PARTIAL_DAY', 'UNMARKED',
];

export type AttendanceMethod =
  | 'MANUAL' | 'FINGERPRINT' | 'FACIAL' | 'RFID' | 'IRIS'
  | 'GPS' | 'NFC' | 'QR_CODE' | 'OTHER';

export const ATTENDANCE_METHODS: AttendanceMethod[] = [
  'MANUAL', 'FINGERPRINT', 'FACIAL', 'RFID', 'IRIS',
  'GPS', 'NFC', 'QR_CODE', 'OTHER',
];

export interface AttendanceRecord {
  id: number;
  companyId: number;
  employeeId: number;
  employeeName: string;
  employeeNumber?: string;
  attendanceDate: string;
  checkInTime?: string;
  checkOutTime?: string;
  checkInDateTime?: string;
  checkOutDateTime?: string;
  checkInMethod?: string;
  checkInLocation?: string;
  checkInLatitude?: string;
  checkInLongitude?: string;
  checkInReason?: string;
  checkOutMethod?: string;
  checkOutLocation?: string;
  shiftType?: string;
  status: string;
  isLate: boolean;
  lateMinutes: number;
  lateReason?: string;
  isOvertime: boolean;
  overtimeHours?: number;
  leftEarly?: boolean;
  earlyMinutes?: number;
  earlyDepartureReason?: string;
  totalWorkingHours?: number;
  approved: boolean;
  approvedBy?: string;
  approvedDateTime?: string;
  isVerified: boolean;
  verificationScore?: number;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface MyAttendanceMonthlySummary {
  year: number;
  month: number;
  presentDays: number;
  absentDays: number;
  halfDays: number;
  onLeaveDays: number;
  holidayDays: number;
  weekOffDays: number;
  workedHours: number;
}

// For HR manually recording/backdating an employee's attendance (POST /manual)
export interface ManualAttendanceRequest {
  employeeId: number;
  attendanceDate: string;
  checkInTime?: string;
  checkOutTime?: string;
  checkInMethod?: string;
  checkOutMethod?: string;
  shiftType?: string;
  status: AttendanceStatus;
  isLate?: boolean;
  lateMinutes?: number;
  lateReason?: string;
  isOvertime?: boolean;
  overtimeHours?: number;
  leftEarly?: boolean;
  earlyMinutes?: number;
  earlyDepartureReason?: string;
  adminNotes?: string;
}

export interface AttendanceLeave {
  id: number;
  companyId: number;
  employeeId: number;
  employeeName: string;
  leaveDate: string;
  leaveType: string;
  leaveReason?: string;
  halfDay: boolean;
  approved: boolean;
  approvedBy?: string;
  rejectionReason?: string;
  notes?: string;
  createdAt: string;
  approvedDate?: string;
  updatedAt?: string;
}

export interface Timesheet {
  id: number;
  workDate: string;
  startTime?: string;
  endTime?: string;
  hoursWorked: number;
  billableHours: number;
  projectName?: string;
  taskDescription?: string;
  description?: string;
  submitted: boolean;
  submittedAt?: string;
  approved: boolean;
  approvedAt?: string;
  status: 'NOT_SUBMITTED' | 'SUBMITTED' | 'APPROVED';
  employeeId: number;
  employeeName?: string;
  approvedById?: number;
  approvedByName?: string;
  taskId?: number;
  taskTitle?: string;
  createdAt: string;
}

export interface TimesheetRequest {
  workDate: string;
  startTime?: string;
  endTime?: string;
  hoursWorked: number;
  billableHours?: number;
  projectName?: string;
  taskDescription?: string;
  description?: string;
  taskId?: number;
}

export type BiometricDeviceType = 'FINGERPRINT_TERMINAL' | 'FACIAL_RECOGNITION' | 'RFID_READER' | 'IRIS_SCANNER' | 'HYBRID' | 'GPS_TRACKING' | 'NFC_READER' | 'QR_SCANNER';
export const BIOMETRIC_DEVICE_TYPES: BiometricDeviceType[] = ['FINGERPRINT_TERMINAL', 'FACIAL_RECOGNITION', 'RFID_READER', 'IRIS_SCANNER', 'HYBRID', 'GPS_TRACKING', 'NFC_READER', 'QR_SCANNER'];

export type BiometricDeviceStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE' | 'OFFLINE';
export const BIOMETRIC_DEVICE_STATUSES: BiometricDeviceStatus[] = ['ACTIVE', 'INACTIVE', 'MAINTENANCE', 'OFFLINE'];

export interface BiometricDevice {
  id: number;
  deviceName: string;
  deviceType: BiometricDeviceType;
  deviceId: string;
  ipAddress?: string;
  portNumber: number;
  location?: string;
  department?: string;
  status: BiometricDeviceStatus;
  matchThreshold: number;
  enabledForCheckIn: boolean;
  enabledForCheckOut: boolean;
  lastSyncTime?: string;
  lastHealthCheckTime?: string;
  isOnline: boolean;
  manufacturer?: string;
  model?: string;
  firmwareVersion?: string;
  totalEnrollments: number;
  maxEnrollments: number;
  notes?: string;
  companyId?: number;
}

export interface BiometricDeviceRequest {
  deviceName: string;
  deviceType: BiometricDeviceType;
  deviceId: string;
  ipAddress?: string;
  portNumber?: number;
  location?: string;
  department?: string;
  matchThreshold?: number;
  enabledForCheckIn?: boolean;
  enabledForCheckOut?: boolean;
  notes?: string;
}

export interface BiometricEnrollment {
  id: number;
  employeeId: number;
  employeeName?: string;
  deviceId: number;
  deviceName?: string;
  biometricType: string;
  biometricTemplate: string;
  templateFormat?: string;
  enrollmentDate?: string;
  enrolledBy?: string;
  enrollmentAttempts: number;
  enrollmentQualityScore: number;
  enrolled: boolean;
  active: boolean;
  lastVerifiedTime?: string;
  successfulMatches: number;
  failedMatches: number;
  notes?: string;
  securityNotes?: string;
}

export interface BiometricEnrollmentRequest {
  employeeId: number;
  deviceId: number;
  biometricType: string;
  biometricTemplate: string;
  templateFormat?: string;
  qualityScore?: number;
}
