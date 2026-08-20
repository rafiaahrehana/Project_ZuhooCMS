import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Employee,
  JobPosting,
  JobPostingRequest,
  JobPostingStatus,
  JOB_POSTING_STATUSES,
  EMPLOYMENT_TYPES,
  EDUCATION_LEVELS,
  Department
} from '../../models/hrm.model';
import { JobPostingService } from '../../services/job-posting.service';
import { DepartmentService } from '../../services/department.service';
import { EmployeeService } from '../../services/employee.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { SkillTagInput } from '../../../../shared/components/skill-tag-input/skill-tag-input';

@Component({
  selector: 'app-job-postings',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective, SkillTagInput],
  templateUrl: './job-postings.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobPostings implements OnInit {
  postings: JobPosting[] = [];
  departments: Department[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: JobPostingRequest = this.emptyForm();

  deleteTarget: JobPosting | null = null;

  statuses = JOB_POSTING_STATUSES;
  employmentTypes = EMPLOYMENT_TYPES;
  educationLevels = EDUCATION_LEVELS;

  // Assign Recruiter quick action
  recruiterTarget: JobPosting | null = null;
  recruiterSelection: number | null = null;
  assigningRecruiter = false;
  employees: Employee[] = [];

  constructor(
    private postingService: JobPostingService,
    private departmentService: DepartmentService,
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadDepartments();
  }

  private loadEmployees(): void {
    if (this.employees.length) return;
    this.employeeService.list(0, 200).subscribe({
      next: (res) => { this.employees = res.content; this.cdr.markForCheck(); },
      error: () => { this.employees = []; this.cdr.markForCheck(); },
    });
  }

  openAssignRecruiter(p: JobPosting): void {
    this.recruiterTarget = p;
    this.recruiterSelection = p.assignedRecruiterId ?? null;
    this.loadEmployees();
  }

  saveRecruiterAssignment(): void {
    if (!this.recruiterTarget) return;
    this.assigningRecruiter = true;
    this.postingService.assignRecruiter(this.recruiterTarget.id, this.recruiterSelection).subscribe({
      next: () => {
        this.assigningRecruiter = false;
        this.recruiterTarget = null;
        this.success = 'Recruiter assignment updated';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.assigningRecruiter = false;
        this.error = err?.error?.message || 'Failed to assign recruiter';
        this.cdr.markForCheck();
      },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.postingService.list(this.page, 20).subscribe({
      next: (res) => {
        this.postings = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load job postings';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadDepartments(): void {
    this.departmentService.listActive().subscribe({
      next: (d) => { this.departments = d; this.cdr.markForCheck(); },
      error: () => { this.departments = []; this.cdr.markForCheck(); }
    });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.showForm = true;
  }

  openEdit(p: JobPosting): void {
    this.form = {
      title: p.title,
      jobTitle: p.jobTitle,
      description: p.description,
      requirements: p.requirements,
      employmentType: p.employmentType,
      status: p.status,
      vacancies: p.vacancies,
      salaryMin: p.salaryMin,
      salaryMax: p.salaryMax,
      deadline: p.deadline,
      remote: p.remote,
      departmentId: p.departmentId,
      requiredSkills: p.requiredSkills,
      preferredSkills: p.preferredSkills,
      minExperienceYears: p.minExperienceYears,
      minEducationLevel: p.minEducationLevel,
    };
    this.selectedId = p.id;
    this.isEdit = true;
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload = this.cleanPayload();
    const request = this.isEdit && this.selectedId
      ? this.postingService.update(this.selectedId, payload)
      : this.postingService.create(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.isEdit ? 'Job posting updated' : 'Job posting created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save job posting';
        this.cdr.markForCheck();
      }
    });
  }

  publish(p: JobPosting): void {
    this.postingService.publish(p.id).subscribe({
      next: () => {
        this.success = 'Job posting published successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to publish job posting';
        this.cdr.markForCheck();
      }
    });
  }

  close(p: JobPosting): void {
    this.postingService.close(p.id).subscribe({
      next: () => {
        this.success = 'Job posting closed successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to close job posting';
        this.cdr.markForCheck();
      }
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.postingService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Job posting deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete job posting';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  // Top-accent color per posting status.
  statusColor(status?: string): string {
    switch (status) {
      case 'OPEN': return '#16A34A';
      case 'DRAFT': return '#D97706';
      case 'CLOSED': return '#DC2626';
      case 'ON_HOLD': return '#64748B';
      default: return '#6B46FF';
    }
  }

  private emptyForm(): JobPostingRequest {
    return {
      title: '',
      jobTitle: '',
      description: '',
      requirements: '',
      employmentType: 'FULL_TIME',
      status: 'DRAFT',
      vacancies: 1,
      remote: false,
    };
  }

  private cleanPayload(): JobPostingRequest {
    const payload: any = { ...this.form };
    if (!payload.departmentId) delete payload.departmentId;
    if (!payload.deadline) delete payload.deadline;
    if (!payload.minEducationLevel) delete payload.minEducationLevel;
    if (payload.minExperienceYears === '' || payload.minExperienceYears == null) delete payload.minExperienceYears;
    return payload;
  }
}
