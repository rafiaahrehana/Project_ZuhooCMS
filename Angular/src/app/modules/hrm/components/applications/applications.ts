import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ApplicationStatus,
  APPLICATION_STATUSES,
  APPLICATION_SOURCES,
  Candidate,
  Department,
  Designation,
  Employee,
  EMPLOYMENT_TYPES,
  EvaluateCandidateRequest,
  HireApplicationRequest,
  JobApplication,
  JobApplicationRequest,
  JobPosting,
  MANUAL_APPLICATION_STATUSES,
} from '../../models/hrm.model';
import { RecruitmentService } from '../../services/recruitment.service';
import { JobPostingService } from '../../services/job-posting.service';
import { CandidateService } from '../../services/candidate.service';
import { DepartmentService } from '../../services/department.service';
import { DesignationService } from '../../services/designation.service';
import { ShiftService } from '../../services/shift.service';
import { EmployeeService } from '../../services/employee.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { FileUploadResult } from '../../../../shared/services/file-upload.service';

@Component({
  selector: 'app-applications',
  imports: [CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective, FileUpload],
  templateUrl: './applications.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Applications implements OnInit {
  // VARIABLES
  applications: JobApplication[] = [];
  jobs: JobPosting[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  statusFilter: ApplicationStatus | '' = '';

  showForm = false;
  selectedJobId: number | null = null;
  form: JobApplicationRequest = { applicantName: '', applicantEmail: '' };

  statusTarget: JobApplication | null = null;
  newStatus: ApplicationStatus = 'APPLIED';
  statusNotes = '';
  deleteTarget: JobApplication | null = null;

  // FULL APPLICANT DETAIL - everything about this person and this specific
  // application in one place (resume/LinkedIn/portfolio, cover letter, both
  // scores) - the JobApplication row already has most of it, but current
  // title/skills/candidate notes only live on the Candidate record itself.
  detailTarget: JobApplication | null = null;
  detailCandidate: Candidate | null = null;
  detailLoading = false;

  // ATS MATCH BREAKDOWN - a signal alongside the manual Evaluate score above,
  // never a replacement. See CvScoringService on the backend.
  atsTarget: JobApplication | null = null;
  private readonly atsStatusLabels: Record<string, string> = {
    PENDING: 'Not scored yet',
    FAILED: 'Could not read this resume',
    UNSUPPORTED_FORMAT: 'Resume format not supported (PDF or DOCX only)',
    NO_RESUME: 'No resume on file to parse',
    NOT_APPLICABLE: 'This job has no ATS requirements set',
  };

  atsStatusLabel(a: JobApplication): string {
    return this.atsStatusLabels[a.atsParseStatus || 'NOT_APPLICABLE'] || '';
  }

  skillList(csv?: string): string[] {
    return csv ? csv.split(',').map((s) => s.trim()).filter((s) => s.length > 0) : [];
  }

  // EVALUATE - weights mirror RecruitmentServiceImpl.evaluate(): Education 20% /
  // Experience 25% / Technical Skills 25% / Interview 20% / Communication 10%.
  evaluateTarget: JobApplication | null = null;
  evaluateForm: EvaluateCandidateRequest = {};
  evaluating = false;
  private readonly scoreWeights: Record<keyof EvaluateCandidateRequest, number> = {
    scoreEducation: 0.20, scoreExperience: 0.25, scoreTechnicalSkills: 0.25,
    scoreInterview: 0.20, scoreCommunication: 0.10,
  };

  /** Live preview computed the same way the backend will - lets HR see the score before saving. */
  get evaluatePreview(): number | null {
    const entries = Object.entries(this.evaluateForm) as [keyof EvaluateCandidateRequest, number | undefined][];
    const present = entries.filter(([, v]) => v != null && v !== ('' as any));
    if (!present.length) return null;
    const totalWeight = present.reduce((sum, [k]) => sum + this.scoreWeights[k], 0);
    const weightedSum = present.reduce((sum, [k, v]) => sum + this.scoreWeights[k] * (v as number), 0);
    return Math.round((weightedSum / totalWeight) * 10) / 10;
  }

  statuses = APPLICATION_STATUSES;
  sources = APPLICATION_SOURCES;
  // HIRED and the four OFFER_* sub-statuses excluded: HIRED only happens
  // through the dedicated Hire action, and offer statuses only move through
  // the Offers screen - the backend rejects both from this generic dropdown,
  // this just keeps the options from being offered in the first place.
  updatableStatuses = MANUAL_APPLICATION_STATUSES;

  // HIRE MODAL - a 4-step onboarding wizard rather than one wall of fields.
  hireTarget: JobApplication | null = null;
  hireForm: HireApplicationRequest = { password: '' };
  hireConfirmPassword = '';
  hireShowPassword = false;
  hiring = false;
  readonly hireSteps = ['Account', 'Position', 'Dates & Salary', 'Bank & Emergency'];
  hireStep = 1;
  /** The ACCEPTED offer whose terms were pre-filled, for the banner. */
  offerApplied: any = null;

  /** Step 1 gates on the only hard requirements; later steps are all optional. */
  get hireStepValid(): boolean {
    if (this.hireStep === 1) {
      return !!this.hireForm.password && this.hireForm.password.length >= 8
        && this.hireForm.password === this.hireConfirmPassword;
    }
    return true;
  }

  goToHireStep(step: number): void {
    if (step > 1 && !this.hireStepValid && this.hireStep === 1) return;
    this.hireStep = step;
  }
  departments: Department[] = [];
  designations: Designation[] = [];
  shifts: any[] = [];
  employees: Employee[] = [];
  types = EMPLOYMENT_TYPES;

  constructor(
    private recruitmentService: RecruitmentService,
    private jobPostingService: JobPostingService,
    private departmentService: DepartmentService,
    private designationService: DesignationService,
    private shiftService: ShiftService,
    private employeeService: EmployeeService,
    private candidateService: CandidateService,
    private cdr: ChangeDetectorRef,
  ) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void { this.load(); }

  // OPEN FULL APPLICANT DETAIL
  openDetail(a: JobApplication): void {
    this.detailTarget = a;
    this.detailCandidate = null;
    this.detailLoading = true;
    this.candidateService.getById(a.candidateId).subscribe({
      next: (c) => { this.detailCandidate = c; this.detailLoading = false; this.cdr.markForCheck(); },
      error: () => { this.detailLoading = false; this.cdr.markForCheck(); },
    });
  }

  closeDetail(): void {
    this.detailTarget = null;
    this.detailCandidate = null;
  }

  // LOAD APPLICATIONS
  load(): void {
    this.loading = true;
    this.error = '';
    this.recruitmentService.list(this.page, 20, this.statusFilter || undefined).subscribe({
      next: (res) => { this.applications = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load applications'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // LOAD OPEN JOB POSTINGS FOR THE APPLY DROPDOWN
  loadJobs(): void {
    if (this.jobs.length) return;
    this.jobPostingService.listOpen().subscribe({
      next: (res) => { this.jobs = res; this.cdr.markForCheck(); },
      error: () => { this.jobs = []; this.cdr.markForCheck(); }
    });
  }

  // OPEN APPLY FORM
  openApply(): void {
    this.form = { applicantName: '', applicantEmail: '' };
    this.selectedJobId = null;
    this.showForm = true;
    this.loadJobs();
  }

  onResumeUploaded(result: FileUploadResult): void {
    this.form.resumeUrl = result.fileUrl;
  }

  // SUBMIT APPLICATION
  apply(): void {
    if (!this.selectedJobId) return;
    this.recruitmentService.apply(this.selectedJobId, this.form).subscribe({
      next: () => { this.showForm = false; this.success = 'Application submitted'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to submit application'; this.cdr.markForCheck(); }
    });
  }

  // OPEN STATUS CHANGE DIALOG
  openStatusChange(a: JobApplication): void {
    this.statusTarget = a;
    this.newStatus = a.status;
    this.statusNotes = a.notes || '';
  }

  // SUBMIT STATUS CHANGE
  doStatusChange(): void {
    if (!this.statusTarget) return;
    this.recruitmentService.updateStatus(this.statusTarget.id, this.newStatus, this.statusNotes).subscribe({
      next: () => { this.statusTarget = null; this.success = 'Application updated'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to update'; this.statusTarget = null; this.cdr.markForCheck(); }
    });
  }

  // OPEN EVALUATE DIALOG
  openEvaluate(a: JobApplication): void {
    this.evaluateTarget = a;
    this.evaluateForm = {
      scoreEducation: a.scoreEducation,
      scoreExperience: a.scoreExperience,
      scoreTechnicalSkills: a.scoreTechnicalSkills,
      scoreInterview: a.scoreInterview,
      scoreCommunication: a.scoreCommunication,
    };
  }

  // SUBMIT EVALUATION
  doEvaluate(): void {
    if (!this.evaluateTarget) return;
    this.evaluating = true;
    this.recruitmentService.evaluate(this.evaluateTarget.id, this.evaluateForm).subscribe({
      next: () => {
        this.evaluating = false;
        this.evaluateTarget = null;
        this.success = 'Candidate evaluation saved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.evaluating = false;
        this.error = err?.error?.message || 'Failed to save evaluation';
        this.cdr.markForCheck();
      },
    });
  }

  // LOAD DROPDOWN DATA FOR THE HIRE FORM (ONCE)
  private loadHireLookups(): void {
    if (this.departments.length) return;
    this.departmentService.listActive().subscribe({ next: (d) => { this.departments = d; this.cdr.markForCheck(); } });
    this.designationService.listActive().subscribe({ next: (d) => { this.designations = d; this.cdr.markForCheck(); } });
    this.shiftService.listActive().subscribe({ next: (res) => { this.shifts = res; this.cdr.markForCheck(); } });
    this.employeeService.list(0, 100).subscribe({ next: (res) => { this.employees = res.content; this.cdr.markForCheck(); } });
  }

  // OPEN HIRE DIALOG
  openHire(a: JobApplication): void {
    this.hireTarget = a;
    this.hireForm = { password: '' };
    this.hireConfirmPassword = '';
    this.hireShowPassword = false;
    this.hireStep = 1;
    this.offerApplied = null;
    this.loadHireLookups();

    // The accepted offer is the agreed terms - pre-fill so HR reviews rather
    // than retypes. The backend applies the same defaults for blanks anyway.
    this.recruitmentService.offersForApplication(a.id).subscribe({
      next: (offers: any[]) => {
        const accepted = (offers || []).find((o) => o.status === 'ACCEPTED');
        if (!accepted || this.hireTarget?.id !== a.id) return;
        this.offerApplied = accepted;
        this.hireForm.basicSalary = accepted.basicSalary ?? this.hireForm.basicSalary;
        this.hireForm.houseRent = accepted.houseRent ?? this.hireForm.houseRent;
        this.hireForm.medicalAllowance = accepted.medicalAllowance ?? this.hireForm.medicalAllowance;
        this.hireForm.transportAllowance = accepted.transportAllowance ?? this.hireForm.transportAllowance;
        this.hireForm.hireDate = accepted.joiningDate ?? this.hireForm.hireDate;
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  // SUBMIT HIRE
  doHire(): void {
    if (!this.hireTarget || this.hiring) return;
    if (this.hireForm.password !== this.hireConfirmPassword) {
      this.error = 'Passwords do not match';
      this.cdr.markForCheck();
      return;
    }
    this.hiring = true;
    this.recruitmentService.hire(this.hireTarget.id, this.hireForm).subscribe({
      next: () => {
        this.hiring = false;
        this.hireTarget = null;
        this.success = 'Candidate hired — employee record created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.hiring = false;
        this.error = err?.error?.message || 'Failed to hire candidate';
        this.cdr.markForCheck();
      },
    });
  }

  // DELETE APPLICATION
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.recruitmentService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Application deleted'; this.cdr.markForCheck(); this.load(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete application'; this.cdr.markForCheck(); }
    });
  }

  // PAGINATION
  goToPage(p: number): void { this.page = p; this.load(); }

  // STATUS BADGE CLASS
  statusClass(status: ApplicationStatus): string {
    return {
      APPLIED: 'text-bg-secondary',
      SCREENING: 'text-bg-info',
      SHORTLISTED: 'text-bg-info',
      INTERVIEW_SCHEDULED: 'text-bg-primary',
      INTERVIEWED: 'text-bg-primary',
      SELECTED: 'text-bg-primary',
      OFFER_PENDING: 'text-bg-warning',
      OFFER_SENT: 'text-bg-warning',
      OFFER_ACCEPTED: 'text-bg-success',
      OFFER_REJECTED: 'text-bg-danger',
      HIRED: 'text-bg-success',
      REJECTED: 'text-bg-danger',
      WITHDRAWN: 'text-bg-secondary',
    }[status] || 'text-bg-secondary';
  }
}
