import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OfferLetter, OfferLetterRequest, LetterType, LETTER_TYPES, Employee, JobApplication } from '../../models/hrm.model';
import { OfferLetterService } from '../../services/offer-letter.service';
import { EmployeeService } from '../../services/employee.service';
import { RecruitmentService } from '../../services/recruitment.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

// Letter types addressed to a recruitment candidate (not yet an employee).
const CANDIDATE_LETTER_TYPES: LetterType[] = ['OFFER', 'APPOINTMENT'];

@Component({
  selector: 'app-offer-letters',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './offer-letters.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OfferLetters implements OnInit {
  letters: OfferLetter[] = [];
  employees: Employee[] = [];
  candidates: JobApplication[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: OfferLetterRequest = this.emptyForm();

  deleteTarget: OfferLetter | null = null;

  letterTypes = LETTER_TYPES;

  drafting = false;
  draftError = '';

  constructor(
    private letterService: OfferLetterService,
    private employeeService: EmployeeService,
    private recruitmentService: RecruitmentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadEmployees();
    this.loadCandidates();
  }

  // OFFER/APPOINTMENT letters address a recruitment candidate, everything else an employee.
  get isCandidateLetter(): boolean {
    return !!this.form.letterType && CANDIDATE_LETTER_TYPES.includes(this.form.letterType);
  }

  // The selected recipient id for the current letter type (used for validation/enabling).
  get recipientId(): number | undefined {
    return this.isCandidateLetter ? this.form.jobApplicationId : this.form.employeeId;
  }

  // When the letter type flips between candidate/employee, drop the now-irrelevant recipient.
  onLetterTypeChange(): void {
    if (this.isCandidateLetter) {
      this.form.employeeId = undefined;
    } else {
      this.form.jobApplicationId = undefined;
    }
  }

  loadCandidates(): void {
    this.recruitmentService.list(0, 200).subscribe({
      next: (res) => {
        // Exclude rejected/withdrawn — they're not letter recipients.
        this.candidates = (res.content || []).filter(
          (c) => c.status !== 'REJECTED' && c.status !== 'WITHDRAWN',
        );
        this.cdr.markForCheck();
      },
      error: () => { this.candidates = []; this.cdr.markForCheck(); },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.letterService.list(this.page, 20).subscribe({
      next: (res) => {
        this.letters = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load offer/official letters';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadEmployees(): void {
    this.employeeService.list(0, 100).subscribe({
      next: (res) => { this.employees = res.content; this.cdr.markForCheck(); },
      error: () => { this.employees = []; this.cdr.markForCheck(); }
    });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.draftError = '';
    this.showForm = true;
  }

  draftWithAi(): void {
    if (!this.recipientId || !this.form.letterType || this.drafting) return;
    this.drafting = true;
    this.draftError = '';
    this.cdr.markForCheck();
    this.letterService.draftWithAi({
      letterType: this.form.letterType,
      employeeId: this.isCandidateLetter ? undefined : this.form.employeeId,
      jobApplicationId: this.isCandidateLetter ? this.form.jobApplicationId : undefined,
    }).subscribe({
      next: (res) => {
        this.form.content = res.content;
        this.drafting = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.draftError = err?.error?.message || 'Failed to generate draft';
        this.drafting = false;
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload = this.cleanPayload();
    this.letterService.create(payload).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = 'Letter record created successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to create letter';
        this.cdr.markForCheck();
      }
    });
  }

  issue(l: OfferLetter): void {
    this.letterService.issue(l.id).subscribe({
      next: () => {
        this.success = 'Letter marked as issued';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to issue letter';
        this.cdr.markForCheck();
      }
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.letterService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Letter record deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete letter';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  private emptyForm(): OfferLetterRequest {
    return {
      employeeId: undefined,
      jobApplicationId: undefined,
      letterType: 'OFFER',
      referenceNumber: '',
      issueDate: '',
      content: '',
      signedBy: '',
    };
  }

  private cleanPayload(): OfferLetterRequest {
    const payload: any = { ...this.form };
    return payload;
  }
}
