export type EmploymentStatus =
  | 'PROBATION' | 'CONFIRMED' | 'ACTIVE' | 'ON_LEAVE'
  | 'SUSPENDED' | 'RESIGNED' | 'TERMINATED' | 'RETIRED';

import { LocationRequest, LocationResponse } from '../../../shared/models/location.model';
import { PaymentMethod } from '../../finance/models/finance.model';

export type EmploymentType =
  | 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERN' | 'CONSULTANT';

export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';

export const EMPLOYMENT_STATUSES: EmploymentStatus[] =
  ['PROBATION', 'CONFIRMED', 'ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'RESIGNED', 'TERMINATED', 'RETIRED'];

export const EMPLOYMENT_TYPES: EmploymentType[] =
  ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN', 'CONSULTANT'];

export const GENDERS: Gender[] = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'];

// EmployeeResponse
export interface Employee {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  image?: string;
  employeeNumber?: string;
  officialEmail?: string;
  workPhone?: string;
  profileImageUrl?: string;
  nationalId?: string;
  taxId?: string;
  costCenter?: string;
  officeLocation?: string;
  jobTitle?: string;
  employmentType: EmploymentType;
  employmentStatus: EmploymentStatus;
  gender?: Gender;
  dateOfBirth?: string;
  fatherName?: string;
  motherName?: string;
  location?: LocationResponse;
  hireDate?: string;
  confirmationDate?: string;
  probationEndDate?: string;
  contractEndDate?: string;
  departmentId?: number;
  departmentName?: string;
  designationId?: number;
  designationName?: string;
  reportingManagerId?: number;
  reportingManagerName?: string;
  shiftId?: number;
  shiftName?: string;
  basicSalary?: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  billableRate?: number;
  bankName?: string;
  bankAccountNumber?: string;
  /** Bangladesh Bank routing number - 9 digits. Needed for BEFTN salary transfer. */
  bankRoutingNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  active: boolean;
  createdAt: string;
  customRoleId?: number;
  customRoleName?: string;
}

// CreateEmployeeRequest
export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  employmentType: EmploymentType;
  // employeeNumber is server-generated - not part of the create payload.
  officialEmail?: string;
  workPhone?: string;
  jobTitle?: string;
  designationId?: number;
  employmentStatus?: EmploymentStatus;
  departmentId?: number;
  reportingManagerId?: number;
  shiftId?: number;
  gender?: Gender;
  dateOfBirth?: string;
  fatherName?: string;
  motherName?: string;
  location?: LocationRequest;
  hireDate?: string;
  basicSalary?: number;
  nationalId?: string;
  taxId?: string;
  profileImageUrl?: string;
  costCenter?: string;
  officeLocation?: string;
  confirmationDate?: string;
  probationEndDate?: string;
  contractEndDate?: string;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  billableRate?: number;
  bankName?: string;
  bankAccountNumber?: string;
  /** Bangladesh Bank routing number - 9 digits (bank 3 + district 2 + branch 3 + check 1). */
  bankRoutingNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
}

// UpdateEmployeeRequest (all optional on backend)
export interface UpdateEmployeeRequest {
  jobTitle?: string;
  designationId?: number;
  employmentType?: EmploymentType;
  employmentStatus?: EmploymentStatus;
  gender?: Gender;
  dateOfBirth?: string;
  fatherName?: string;
  motherName?: string;
  location?: LocationRequest;
  hireDate?: string;
  confirmationDate?: string;
  probationEndDate?: string;
  contractEndDate?: string;
  departmentId?: number;
  reportingManagerId?: number;
  shiftId?: number;
  basicSalary?: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  billableRate?: number;
  bankName?: string;
  bankAccountNumber?: string;
  /** Bangladesh Bank routing number - 9 digits (bank 3 + district 2 + branch 3 + check 1). */
  bankRoutingNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  nationalId?: string;
  taxId?: string;
  costCenter?: string;
  officeLocation?: string;
  workPhone?: string;
  officialEmail?: string;
  profileImageUrl?: string;
}

// SelfUpdateEmployeeRequest - the narrow subset of UpdateEmployeeRequest an employee may
// self-edit via PATCH /employees/me (see backend SelfUpdateEmployeeRequest for why the
// rest - job title, department, salary, bank details, employment status, date of birth -
// stays HR-only).
export interface SelfUpdateEmployeeRequest {
  workPhone?: string;
  // Personal mobile number - lives on the User account (Employee.phone below),
  // distinct from workPhone which lives on the Employee record.
  phone?: string;
  profileImageUrl?: string;
  gender?: Gender;
  fatherName?: string;
  motherName?: string;
  nationalId?: string;
  taxId?: string;
  location?: LocationRequest;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
}

// DepartmentResponse
export interface Department {
  id: number;
  name: string;
  code?: string;
  description?: string;
  active: boolean;
  budget?: number;
  parentDepartmentId?: number;
  parentDepartmentName?: string;
  headEmployeeId?: number;
  headEmployeeName?: string;
  employeeCount: number;
  createdAt: string;
}

// DepartmentRequest
export interface DepartmentRequest {
  name: string;
  code?: string;
  description?: string;
  headEmployeeId?: number;
  parentDepartmentId?: number;
  budget?: number;
}

// DesignationResponse
export interface Designation {
  id: number;
  name: string;
  code: string;
  level: number;
  description?: string;
  active: boolean;
  employmentCategory?: string;
  departmentId?: number;
  departmentName?: string;
  createdAt: string;
}

// DesignationRequest
export interface DesignationRequest {
  name: string;
  code: string;
  level: number;
  description?: string;
  employmentCategory?: string;
  departmentId?: number;
  active?: boolean;
}

export const JOB_LEVELS: { value: number; label: string }[] = [
  { value: 1, label: 'L1 - Executive Leadership' },
  { value: 2, label: 'L2 - Director' },
  { value: 3, label: 'L3 - Senior Manager' },
  { value: 4, label: 'L4 - Manager' },
  { value: 5, label: 'L5 - Team Lead' },
  { value: 6, label: 'L6 - Senior Professional' },
  { value: 7, label: 'L7 - Professional' },
  { value: 8, label: 'L8 - Associate' },
  { value: 9, label: 'L9 - Trainee' },
];

export const DESIGNATION_EMPLOYMENT_CATEGORIES: { value: string; label: string }[] = [
  { value: 'FULL_TIME', label: 'Full Time' },
  { value: 'PART_TIME', label: 'Part Time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'TEMPORARY', label: 'Temporary' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'CONSULTANT', label: 'Consultant' },
];

export type PayrollStatus = 'DRAFT' | 'APPROVED' | 'PAID' | 'CANCELLED';

// PayrollResponse
export interface Payroll {
  id: number;
  payMonth: number;
  payYear: number;
  basicSalary: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  foodAllowance?: number;
  specialAllowance?: number;
  bonus?: number;
  // Server-computed: approved timesheet billable hours in this period * the
  // employee's billableRate - not a manual entry field, see CreatePayrollRequest.
  billableHours?: number;
  billableRate?: number;
  billablePay?: number;
  /** Overtime frozen onto the record at run time - hours, the rate applied (multiplier included), and the money. */
  overtimeHours?: number;
  overtimeRate?: number;
  overtimePay?: number;
  deductions?: number;
  taxDeduction?: number;
  insuranceDeduction?: number;
  providentFundDeduction?: number;
  attendanceDeduction?: number;
  absentDays?: number;
  /** Frozen sums of the structure's recurring extra components at run time. */
  otherEarnings?: number;
  otherDeductions?: number;
  netSalary: number;
  status: PayrollStatus;
  paymentReference?: string;
  paymentMethod?: PaymentMethod;
  paidAt?: string;
  notes?: string;
  employeeId: number;
  employeeName: string;
  approvedById?: number;
  approvedByName?: string;
  createdAt: string;
}

// CreatePayrollRequest
export interface CreatePayrollRequest {
  employeeId: number;
  payMonth: number;
  payYear: number;
  // Optional - if omitted, the backend pulls these from the employee's active SalaryStructure.
  basicSalary?: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  foodAllowance?: number;
  specialAllowance?: number;
  bonus?: number;
  deductions?: number;
  taxDeduction?: number;
  insuranceDeduction?: number;
  providentFundDeduction?: number;
  notes?: string;
}

// BulkPayrollResult
export interface BulkPayrollResult {
  created: string[];
  skippedAlreadyExists: string[];
  skippedNoSalaryStructure: string[];
}

// SalaryStructureResponse
export interface SalaryStructure {
  id: number;
  employeeId: number;
  employeeName: string;
  effectiveFrom: string;
  effectiveTo?: string;
  grossSalary: number;
  basicSalary: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  foodAllowance?: number;
  specialAllowance?: number;
  providentFund?: number;
  taxDeduction?: number;
  netSalary: number;
  notes?: string;
  approvedById?: number;
  approvedByName?: string;
  createdAt: string;
}

// SalaryStructureRequest
export interface SalaryStructureRequest {
  employeeId: number;
  effectiveFrom: string;
  grossSalary: number;
  basicSalary: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  foodAllowance?: number;
  specialAllowance?: number;
  providentFund?: number;
  taxDeduction?: number;
  notes?: string;
}

export interface EducationQualification {
  id: number;
  employeeId: number;
  employeeName: string;
  degree: string;
  institution: string;
  fieldOfStudy?: string;
  passingYear?: number;
  result?: string;
  notes?: string;
  createdAt: string;
}

export interface EducationQualificationRequest {
  employeeId: number;
  degree: string;
  institution: string;
  fieldOfStudy?: string;
  passingYear?: number;
  result?: string;
  notes?: string;
}

export interface Announcement {
  id: number;
  title: string;
  body: string;
  audience?: string;
  targetDepartmentId?: number;
  targetDepartmentName?: string;
  publishedAt?: string;
  expiresAt?: string;
  scheduledAt?: string;
  published: boolean;
  notifyAll: boolean;
  priority: number;
  attachmentUrl?: string;
  createdById?: number;
  createdByName?: string;
  createdAt: string;
}

export interface AnnouncementRequest {
  title: string;
  body: string;
  audience?: string;
  targetDepartmentId?: number;
  expiresAt?: string;
  scheduledAt?: string;
  notifyAll?: boolean;
  priority?: number;
  attachmentUrl?: string;
}

export interface AnnouncementDraftResponse {
  title: string;
  body: string;
}

export interface Holiday {
  id: number;
  name: string;
  holidayDate: string;
  holidayType?: string;
  description?: string;
  createdAt: string;
}

export interface HolidayRequest {
  name: string;
  holidayDate?: string;
  holidayType?: string;
  description?: string;
}

export interface HolidayDraftResponse {
  name: string;
  date: string;
  type: string;
  description: string;
}

export interface LeavePolicy {
  id: number;
  leaveType: string;
  employmentType: string;
  annualEntitlement: number;
  maxCarryForward: number;
  maxConsecutiveDays?: number;
  requiresApproval: boolean;
  canCarryForward: boolean;
  paid: boolean;
  applicableFromMonths: number;
  active: boolean;
  createdAt: string;
}

export interface LeavePolicyRequest {
  leaveType?: string;
  employmentType?: string;
  annualEntitlement?: number;
  maxCarryForward?: number;
  maxConsecutiveDays?: number;
  requiresApproval?: boolean;
  canCarryForward?: boolean;
  paid?: boolean;
  applicableFromMonths?: number;
}

export interface PerformanceReview {
  id: number;
  reviewPeriodStart: string;
  reviewPeriodEnd: string;
  scoreWorkQuality?: number;
  scoreProductivity?: number;
  scoreCommunication?: number;
  scoreTeamwork?: number;
  scoreInitiative?: number;
  scorePunctuality?: number;
  scoreLeadership?: number;
  scoreProblemSolving?: number;
  scoreInnovation?: number;
  overallScore?: number;
  strengths?: string;
  areasForImprovement?: string;
  goalsForNextPeriod?: string;
  comments?: string;
  // Review outcome
  performanceLevel?: string;
  promotionRecommendation?: string;
  promotionReadiness?: string;
  salaryIncrement?: string;
  employmentStatusRecommendation?: string;
  goalCompletionPercent?: number;
  /** Comma-separated. */
  trainingRecommendation?: string;
  /** Comma-separated. */
  recognition?: string;
  /** JSON array of {title, progress}. */
  goals?: string;
  // Approval chain
  stage?: PerformanceStage;
  selfAssessmentAt?: string;
  selfAssessmentBy?: string;
  managerReviewAt?: string;
  managerReviewBy?: string;
  hrApprovalAt?: string;
  hrApprovalBy?: string;
  finalApprovalAt?: string;
  finalApprovalBy?: string;
  finalised: boolean;
  employeeId: number;
  employeeName?: string;
  reviewedById?: number;
  reviewedByName?: string;
  createdAt: string;
  aiSummary?: string;
}

export type PerformanceStage =
  'SELF_ASSESSMENT' | 'MANAGER_REVIEW' | 'HR_APPROVAL' | 'FINAL_APPROVAL' | 'COMPLETED';

export const PERFORMANCE_STAGES: PerformanceStage[] =
  ['SELF_ASSESSMENT', 'MANAGER_REVIEW', 'HR_APPROVAL', 'FINAL_APPROVAL'];

/** One goal-tracking bar. Persisted as JSON in PerformanceReview.goals. */
export interface PerformanceGoal {
  title: string;
  progress: number;
}

export interface PerformanceReviewRequest {
  employeeId?: number;
  reviewPeriodStart?: string;
  reviewPeriodEnd?: string;
  scoreWorkQuality?: number;
  scoreProductivity?: number;
  scoreCommunication?: number;
  scoreTeamwork?: number;
  scoreInitiative?: number;
  scorePunctuality?: number;
  scoreLeadership?: number;
  scoreProblemSolving?: number;
  scoreInnovation?: number;
  strengths?: string;
  areasForImprovement?: string;
  goalsForNextPeriod?: string;
  comments?: string;
  performanceLevel?: string;
  promotionRecommendation?: string;
  promotionReadiness?: string;
  salaryIncrement?: string;
  employmentStatusRecommendation?: string;
  goalCompletionPercent?: number;
  trainingRecommendation?: string;
  recognition?: string;
  goals?: string;
}

export type ShiftType = 'MORNING' | 'AFTERNOON' | 'FULL_DAY' | 'EVENING' | 'NIGHT' | 'FLEXIBLE';
export const SHIFT_TYPES: ShiftType[] = ['MORNING', 'AFTERNOON', 'FULL_DAY', 'EVENING', 'NIGHT', 'FLEXIBLE'];

export interface Shift {
  id: number;
  name: string;
  shiftType: ShiftType;
  startTime: string;
  endTime: string;
  gracePeriodMinutes: number;
  weeklyOffDays?: string;
  flexible: boolean;
  nightShift: boolean;
  active: boolean;
  workingMinutes: number;
  description?: string;
  notes?: string;
  createdAt: string;
}

export interface EmployeeShiftAssignment {
  id: number;
  employeeId: number;
  employeeName?: string;
  shiftId: number;
  shiftName?: string;
  assignmentStartDate?: string;
  assignmentEndDate?: string;
  active: boolean;
  reason?: string;
  assignedBy?: string;
  notes?: string;
  companyId?: number;
}

export interface EmployeeShiftAssignmentRequest {
  employeeId: number;
  shiftId: number;
  assignmentStartDate?: string;
  assignmentEndDate?: string;
  reason?: string;
  assignedBy?: string;
  notes?: string;
}

export interface ShiftRequest {
  name: string;
  shiftType?: ShiftType;
  startTime?: string;
  endTime?: string;
  gracePeriodMinutes?: number;
  weeklyOffDays?: string;
  flexible?: boolean;
  nightShift?: boolean;
  description?: string;
  notes?: string;
}

export type LeaveType = 'ANNUAL' | 'SICK' | 'CASUAL' | 'MATERNITY' | 'PATERNITY' | 'UNPAID' | 'COMPENSATORY';
export const LEAVE_TYPES: LeaveType[] = ['ANNUAL', 'SICK', 'CASUAL', 'MATERNITY', 'PATERNITY', 'UNPAID', 'COMPENSATORY'];

export type HolidayType = 'NATIONAL' | 'RELIGIOUS' | 'OPTIONAL' | 'COMPANY';
export const HOLIDAY_TYPES: HolidayType[] = ['NATIONAL', 'RELIGIOUS', 'OPTIONAL', 'COMPANY'];

export type AnnouncementAudience = 'ALL' | 'EMPLOYEES' | 'MANAGERS' | 'DEPARTMENT' | 'SPECIFIC';
// SPECIFIC excluded: the backend has no recipient-list model for it yet and
// rejects it on create/update - see AnnouncementServiceImpl.rejectUnsupportedAudience.
export const ANNOUNCEMENT_AUDIENCES: AnnouncementAudience[] = ['ALL', 'EMPLOYEES', 'MANAGERS', 'DEPARTMENT'];

export type JobPostingStatus = 'DRAFT' | 'OPEN' | 'CLOSED' | 'ON_HOLD';
export const JOB_POSTING_STATUSES: JobPostingStatus[] = ['DRAFT', 'OPEN', 'CLOSED', 'ON_HOLD'];

// Ordinal ranking - PHD > MASTER > BACHELOR > DIPLOMA > NONE.
export type EducationLevel = 'NONE' | 'DIPLOMA' | 'BACHELOR' | 'MASTER' | 'PHD';
export const EDUCATION_LEVELS: EducationLevel[] = ['NONE', 'DIPLOMA', 'BACHELOR', 'MASTER', 'PHD'];

export interface JobPosting {
  id: number;
  title: string;
  jobTitle?: string;
  description?: string;
  requirements?: string;
  employmentType?: EmploymentType;
  status: JobPostingStatus;
  vacancies: number;
  salaryMin?: number;
  salaryMax?: number;
  deadline?: string;
  remote: boolean;
  departmentId?: number;
  departmentName?: string;
  createdById?: number;
  createdByName?: string;
  assignedRecruiterId?: number;
  assignedRecruiterName?: string;
  /** Comma-separated. Weighted 40% of the ATS match score - see JobApplication.atsScore. */
  requiredSkills?: string;
  /** Comma-separated - nice-to-have skills and certifications together. Weighted 20%. */
  preferredSkills?: string;
  minExperienceYears?: number;
  minEducationLevel?: EducationLevel;
  createdAt: string;
}

export interface JobPostingRequest {
  title: string;
  jobTitle?: string;
  description?: string;
  requirements?: string;
  employmentType?: EmploymentType;
  status?: JobPostingStatus;
  vacancies?: number;
  salaryMin?: number;
  salaryMax?: number;
  deadline?: string;
  remote?: boolean;
  departmentId?: number;
  requiredSkills?: string;
  preferredSkills?: string;
  minExperienceYears?: number;
  minEducationLevel?: EducationLevel;
}

export type LetterType =
  | 'OFFER' | 'APPOINTMENT' | 'CONFIRMATION' | 'PROMOTION' | 'TRANSFER'
  | 'EXPERIENCE' | 'NOC' | 'SALARY_CERTIFICATE' | 'TERMINATION'
  | 'RESIGNATION_ACCEPTANCE' | 'WARNING' | 'APPRECIATION';
export const LETTER_TYPES: LetterType[] = [
  'OFFER', 'APPOINTMENT', 'CONFIRMATION', 'PROMOTION', 'TRANSFER',
  'EXPERIENCE', 'NOC', 'SALARY_CERTIFICATE', 'TERMINATION',
  'RESIGNATION_ACCEPTANCE', 'WARNING', 'APPRECIATION'
];

export interface OfferLetter {
  id: number;
  letterType: LetterType;
  referenceNumber?: string;
  issueDate: string;
  content: string;
  signedBy?: string;
  fileUrl?: string;
  issued: boolean;
  employeeId?: number;
  employeeName?: string;
  jobApplicationId?: number;
  recipientName?: string;
  recipientEmail?: string;
  createdById?: number;
  createdByName?: string;
  createdAt: string;
}

export interface OfferLetterRequest {
  employeeId?: number;
  jobApplicationId?: number;
  letterType?: LetterType;
  referenceNumber?: string;
  issueDate?: string;
  content?: string;
  signedBy?: string;
}

export type LeaveRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export const LEAVE_REQUEST_STATUSES: LeaveRequestStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];

export interface LeaveRequest {
  id: number;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason?: string;
  status: LeaveRequestStatus;
  rejectionReason?: string;
  reviewedAt?: string;
  employeeId: number;
  employeeName?: string;
  reviewedById?: number;
  reviewedByName?: string;
  createdAt: string;
}

export interface LeaveRequestPayload {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface ReviewLeavePayload {
  status: 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
}

export interface LeaveBalance {
  id: number;
  employeeId?: number;
  employeeName?: string;
  leaveType: LeaveType;
  year: number;
  entitledDays: number;
  usedDays: number;
  pendingDays: number;
  remainingDays: number;
}

export interface LeaveBalanceRequest {
  employeeId: number;
  leaveType: LeaveType;
  year: number;
  totalDays: number;
}

// The generic status dropdown blocks HIRED and the four OFFER_* sub-statuses -
// those only move through the Hire action and the Offers screen respectively.
// See RecruitmentServiceImpl.updateStatus() on the backend.
export type ApplicationStatus =
  | 'APPLIED' | 'SCREENING' | 'SHORTLISTED' | 'INTERVIEW_SCHEDULED'
  | 'INTERVIEWED' | 'SELECTED' | 'OFFER_PENDING' | 'OFFER_SENT'
  | 'OFFER_ACCEPTED' | 'OFFER_REJECTED' | 'HIRED' | 'REJECTED' | 'WITHDRAWN';
export const APPLICATION_STATUSES: ApplicationStatus[] = [
  'APPLIED', 'SCREENING', 'SHORTLISTED', 'INTERVIEW_SCHEDULED',
  'INTERVIEWED', 'SELECTED', 'OFFER_PENDING', 'OFFER_SENT',
  'OFFER_ACCEPTED', 'OFFER_REJECTED', 'HIRED', 'REJECTED', 'WITHDRAWN'
];
/** Statuses settable from the generic status dropdown - excludes HIRED and the offer sub-pipeline. */
export const MANUAL_APPLICATION_STATUSES: ApplicationStatus[] = [
  'APPLIED', 'SCREENING', 'SHORTLISTED', 'INTERVIEW_SCHEDULED',
  'INTERVIEWED', 'SELECTED', 'REJECTED', 'WITHDRAWN'
];

export type ApplicationSource =
  | 'CAREER_PAGE' | 'LINKEDIN' | 'FACEBOOK' | 'JOB_PORTAL'
  | 'EMPLOYEE_REFERRAL' | 'AGENCY' | 'DIRECT' | 'OTHER';
export const APPLICATION_SOURCES: ApplicationSource[] = [
  'CAREER_PAGE', 'LINKEDIN', 'FACEBOOK', 'JOB_PORTAL',
  'EMPLOYEE_REFERRAL', 'AGENCY', 'DIRECT', 'OTHER'
];

// CandidateResponse - the person, independent of any one application.
export interface Candidate {
  id: number;
  name: string;
  email: string;
  phone?: string;
  resumeUrl?: string;
  linkedInUrl?: string;
  portfolioUrl?: string;
  currentTitle?: string;
  skills?: string;
  source?: ApplicationSource;
  notes?: string;
  applicationCount: number;
  createdAt: string;
}

export interface CandidateRequest {
  name?: string;
  email?: string;
  phone?: string;
  resumeUrl?: string;
  linkedInUrl?: string;
  portfolioUrl?: string;
  currentTitle?: string;
  skills?: string;
  source?: ApplicationSource;
  notes?: string;
}

// A resume was uploaded but couldn't be scored, or nothing to score against -
// see CvScoringService on the backend for what leads to each value.
export type AtsParseStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'UNSUPPORTED_FORMAT' | 'NO_RESUME' | 'NOT_APPLICABLE';

export interface JobApplication {
  id: number;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  candidatePhone?: string;
  resumeUrl?: string;
  linkedInUrl?: string;
  portfolioUrl?: string;
  coverLetter?: string;
  source?: ApplicationSource;
  status: ApplicationStatus;
  notes?: string;
  jobPostingId: number;
  jobPostingTitle?: string;
  reviewedById?: number;
  reviewedByName?: string;
  convertedEmployeeId?: number;
  convertedAt?: string;
  scoreEducation?: number;
  scoreExperience?: number;
  scoreTechnicalSkills?: number;
  scoreInterview?: number;
  scoreCommunication?: number;
  overallScore?: number;
  // Automated ATS match - a signal alongside overallScore above, never a
  // replacement. See CvScoringService on the backend for the weighting.
  atsScore?: number;
  /** Comma-separated. */
  atsMatchedRequiredSkills?: string;
  /** Comma-separated. */
  atsMissingRequiredSkills?: string;
  /** Comma-separated. */
  atsMatchedPreferredSkills?: string;
  atsExtractedExperienceYears?: number;
  atsMeetsEducationRequirement?: boolean;
  atsParseStatus?: AtsParseStatus;
  atsParsedAt?: string;
  createdAt: string;
}

// All fields optional - a partial evaluation still produces a sensible overallScore server-side.
export interface EvaluateCandidateRequest {
  scoreEducation?: number;
  scoreExperience?: number;
  scoreTechnicalSkills?: number;
  scoreInterview?: number;
  scoreCommunication?: number;
}

export interface JobApplicationRequest {
  applicantName: string;
  applicantEmail: string;
  applicantPhone?: string;
  resumeUrl?: string;
  linkedInUrl?: string;
  portfolioUrl?: string;
  coverLetter?: string;
  source?: ApplicationSource;
}

// HireApplicationRequest - onboarding details for hiring an OFFERED candidate.
// Applicant name/email/phone come from the JobApplication itself.
export interface HireApplicationRequest {
  password: string;
  officialEmail?: string;
  departmentId?: number;
  designationId?: number;
  reportingManagerId?: number;
  shiftId?: number;
  employmentType?: EmploymentType;
  hireDate?: string;
  confirmationDate?: string;
  probationEndDate?: string;
  contractEndDate?: string;
  basicSalary?: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  bankName?: string;
  bankAccountNumber?: string;
  /** Bangladesh Bank routing number - 9 digits (bank 3 + district 2 + branch 3 + check 1). */
  bankRoutingNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
}

export type HrAssetStatus = 'AVAILABLE' | 'ASSIGNED' | 'UNDER_MAINTENANCE' | 'DISPOSED';
export const HR_ASSET_STATUSES: HrAssetStatus[] = ['AVAILABLE', 'ASSIGNED', 'UNDER_MAINTENANCE', 'DISPOSED'];

export interface HrAsset {
  id: number;
  name: string;
  category?: string;
  serialNumber?: string;
  description?: string;
  purchaseDate?: string;
  purchaseCost?: number;
  status: HrAssetStatus;
  assignedAt?: string;
  returnDate?: string;
  notes?: string;
  assignedToId?: number;
  assignedToName?: string;
  createdAt: string;
  // IT Hardware Specific Fields
  assetTag?: string;
  brand?: string;
  model?: string;
  ipAddress?: string;
  macAddress?: string;
  processorModel?: string;
  ramSize?: string;
  storageSize?: string;
  operatingSystem?: string;
  warrantyExpiry?: string;
}

export interface HrAssetRequest {
  name: string;
  category?: string;
  serialNumber?: string;
  description?: string;
  purchaseDate?: string;
  purchaseCost?: number;
  assignedToId?: number;
  notes?: string;
  assetTag?: string;
  brand?: string;
  model?: string;
  ipAddress?: string;
  macAddress?: string;
  processorModel?: string;
  ramSize?: string;
  storageSize?: string;
  operatingSystem?: string;
  warrantyExpiry?: string;
}

export interface AssetAssignmentHistory {
  id: number;
  assetId: number;
  assetName?: string;
  employeeId: number;
  employeeName?: string;
  assignedAt?: string;
  returnedAt?: string;
  condition?: string;
  conditionOnReturn?: string;
  notes?: string;
  assignedById?: number;
  assignedByName?: string;
  createdAt: string;
}

export type HrExpenseStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'REIMBURSED';
export const HR_EXPENSE_STATUSES: HrExpenseStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'REIMBURSED'];

export interface HrExpense {
  id: number;
  title: string;
  category?: string;
  amount: number;
  expenseDate: string;
  description?: string;
  receiptUrl?: string;
  status: HrExpenseStatus;
  rejectionReason?: string;
  reimbursedAt?: string;
  submittedById?: number;
  submittedByName?: string;
  approvedById?: number;
  approvedByName?: string;
  createdAt: string;
}

export interface HrExpenseRequest {
  title: string;
  category?: string;
  amount: number;
  expenseDate: string;
  description?: string;
  receiptUrl?: string;
}

// NOTE: Attendance/AttendanceRequest used to live here, wired to a frontend-only
// '/hrm/attendance' endpoint that has no backend counterpart (the real one is
// '/company/attendance'). Removed; attendance is owned by the `attendance` module
// (modules/attendance/models/attendance.model.ts), which now also covers manual entry.

// RecruitmentKpiResponse — Recruitment Reports & KPIs page. Mirrors the backend DTO exactly.
export interface FunnelStage {
  stage: string;
  count: number;
}

export interface SourceSlice {
  source: string;
  count: number;
  percent: number;
}

export interface JobKpi {
  jobPostingId: number;
  jobTitle: string;
  status: string;
  applications: number;
  shortlisted: number;
  interviews: number;
  offers: number;
  hired: number;
  timeToFillDays?: number;
  offerAcceptanceRate?: number;
  /** Mean automated ATS match score over this posting's scored applications - null if none have been scored. */
  avgAtsMatchScore?: number;
}

export interface RecruiterKpi {
  recruiterId: number;
  recruiterName?: string;
  jobsManaged: number;
  applications: number;
  shortlisted: number;
  interviews: number;
  offers: number;
  hires: number;
  avgTimeToHireDays?: number;
  offerAcceptanceRate?: number;
  /** Mean automated ATS match score over this recruiter's scored applications - null if none have been scored. */
  avgAtsMatchScore?: number;
}

export interface TopCandidate {
  applicationId: number;
  candidateName: string;
  jobTitle?: string;
  overallScore: number;
}

export interface RecruitmentKpiSummary {
  openPositions: number;
  totalCandidates: number;
  totalApplications: number;
  hiresThisMonth: number;
  hiresTotal: number;
  avgTimeToHireDays?: number;
  avgTimeToFillDays?: number;
  applicationToInterviewRate?: number;
  interviewToHireRate?: number;
  offerAcceptanceRate?: number;
  /** Mean automated ATS match score over applications that were actually scored - see JobApplication.atsScore. */
  avgAtsMatchScore?: number;
  funnel: FunnelStage[];
  sourceBreakdown: SourceSlice[];
  jobKpis: JobKpi[];
  recruiterKpis: RecruiterKpi[];
  topCandidates: TopCandidate[];
}
