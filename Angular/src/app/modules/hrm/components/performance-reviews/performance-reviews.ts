import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  PerformanceReview, PerformanceReviewRequest, PerformanceGoal, Employee,
} from '../../models/hrm.model';
import {
  PerformanceReviewService, PerformanceKpis, PerformanceAttachment,
} from '../../services/performance-review.service';
import { EmployeeService } from '../../services/employee.service';
import { FileUploadService, FileUploadResult } from '../../../../shared/services/file-upload.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

/** The competency set shown on the form, in display order. */
const COMPETENCIES = [
  { key: 'scoreWorkQuality',    label: 'Work Quality' },
  { key: 'scoreProductivity',   label: 'Productivity' },
  { key: 'scoreCommunication',  label: 'Communication' },
  { key: 'scoreTeamwork',       label: 'Teamwork' },
  { key: 'scoreLeadership',     label: 'Leadership' },
  { key: 'scoreProblemSolving', label: 'Problem Solving' },
  { key: 'scoreInnovation',     label: 'Innovation' },
  { key: 'scorePunctuality',    label: 'Punctuality' },
] as const;

const STRENGTH_TAGS = [
  'Leadership', 'Team Player', 'Fast Learner',
  'Problem Solver', 'Positive Attitude', 'Client Handling',
];

const IMPROVEMENT_TAGS = [
  'Time Management', 'Public Speaking', 'Documentation',
  'Presentation Skills', 'Delegation', 'Decision Making',
];

const RECOGNITION_TAGS = [
  'Employee of the Month', 'Top Performer', 'Sales Champion',
  'Innovation Award', 'Team Excellence',
];

const TRAINING_TAGS = [
  'Leadership', 'Communication', 'Java Advanced', 'Angular',
  'Cloud', 'Project Management', 'Cyber Security',
];

@Component({
  selector: 'app-performance-reviews',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './performance-reviews.html',
  styleUrl: './performance-reviews.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerformanceReviews implements OnInit {
  reviews: PerformanceReview[] = [];
  employees: Employee[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: PerformanceReviewRequest = this.emptyForm();

  deleteTarget: PerformanceReview | null = null;

  summarisingId: number | null = null;
  summaryError = '';

  // ── Form context ────────────────────────────────────────────
  /** The employee the form is about — drives the summary card. */
  selectedEmployee?: Employee;
  /** The review currently open in the form — drives the approval timeline. */
  editingReview?: PerformanceReview;
  /** Objective KPIs for the review period. Null until loaded. */
  kpis?: PerformanceKpis | null;
  kpisLoading = false;
  kpisError = '';
  /** Earlier reviews for this employee — history table and previous score. */
  employeeHistory: PerformanceReview[] = [];
  attachments: PerformanceAttachment[] = [];
  uploading = false;

  readonly competencies = COMPETENCIES;
  readonly strengthTags = STRENGTH_TAGS;
  readonly improvementTags = IMPROVEMENT_TAGS;
  readonly recognitionTags = RECOGNITION_TAGS;
  readonly trainingTags = TRAINING_TAGS;

  readonly performanceLevels = ['Outstanding', 'Exceeds Expectations', 'Meets Expectations', 'Needs Improvement'];
  readonly promotionOptions = ['Highly Recommended', 'Recommended', 'Needs Improvement', 'Not Recommended'];
  readonly readinessOptions = ['High', 'Medium', 'Low'];
  readonly salaryOptions = ['No Increment', '5%', '8%', '10%', '12%', 'Custom'];
  readonly employmentOptions = ['Promote', 'Retain', 'Performance Improvement Plan', 'Terminate'];

  /** Selected tags, kept as arrays and serialised to comma-separated on save. */
  selectedStrengths: string[] = [];
  selectedImprovements: string[] = [];
  selectedRecognition: string[] = [];
  selectedTraining: string[] = [];
  /** Goal bars, serialised to JSON on save. */
  goalList: PerformanceGoal[] = [];

  readonly stageOrder = [
    { key: 'SELF_ASSESSMENT', label: 'Self Assessment' },
    { key: 'MANAGER_REVIEW',  label: 'Manager Review' },
    { key: 'HR_APPROVAL',     label: 'HR Approval' },
    { key: 'FINAL_APPROVAL',  label: 'Final Approval' },
  ];

  constructor(
    private reviewService: PerformanceReviewService,
    private employeeService: EmployeeService,
    private fileUpload: FileUploadService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadEmployees();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.reviewService.list(this.page, 20).subscribe({
      next: (res) => {
        this.reviews = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load performance reviews';
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

  // ── Form context loading ────────────────────────────────────

  /** Called when the employee or either period date changes. */
  onContextChange(): void {
    this.selectedEmployee = this.employees.find((e) => e.id === this.form.employeeId);
    this.loadKpis();
    this.loadHistory();
    this.cdr.markForCheck();
  }

  private loadKpis(): void {
    const { employeeId, reviewPeriodStart, reviewPeriodEnd } = this.form;
    if (!employeeId || !reviewPeriodStart || !reviewPeriodEnd) {
      this.kpis = null;
      return;
    }
    this.kpisLoading = true;
    this.kpisError = '';
    this.reviewService.kpis(employeeId, reviewPeriodStart, reviewPeriodEnd).subscribe({
      next: (k) => { this.kpis = k; this.kpisLoading = false; this.cdr.markForCheck(); },
      error: () => {
        // A KPI failure must not block the review itself.
        this.kpis = null;
        this.kpisError = 'Could not load KPIs for this period';
        this.kpisLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private loadHistory(): void {
    if (!this.form.employeeId) { this.employeeHistory = []; return; }
    this.reviewService.listForEmployee(this.form.employeeId, 0, 20).subscribe({
      next: (res) => {
        // Exclude the review being edited so it doesn't list itself as history.
        this.employeeHistory = res.content.filter((r) => r.id !== this.selectedId);
        this.cdr.markForCheck();
      },
      error: () => { this.employeeHistory = []; this.cdr.markForCheck(); },
    });
  }

  /** Most recent finalised score before this review — the "previous score" card. */
  get previousScore(): number | null {
    const scored = this.employeeHistory.filter((r) => r.overallScore != null);
    return scored.length ? scored[0].overallScore! : null;
  }

  /** Live average of whatever competencies are filled in, on a 1-10 scale. */
  get liveOverall(): number | null {
    const vals = this.competencies
      .map((c) => Number((this.form as any)[c.key]))
      .filter((v) => !isNaN(v) && v > 0);
    if (!vals.length) return null;
    return Math.round((vals.reduce((a, b) => a + b, 0) / vals.length) * 10) / 10;
  }

  // ── Tag helpers ─────────────────────────────────────────────

  toggleTag(list: string[], tag: string): void {
    const i = list.indexOf(tag);
    if (i >= 0) list.splice(i, 1); else list.push(tag);
    this.cdr.markForCheck();
  }

  isSelected(list: string[], tag: string): boolean {
    return list.includes(tag);
  }

  // ── Goal bars ───────────────────────────────────────────────

  addGoal(): void {
    this.goalList.push({ title: '', progress: 0 });
    this.cdr.markForCheck();
  }

  removeGoal(i: number): void {
    this.goalList.splice(i, 1);
    this.cdr.markForCheck();
  }

  /** Mean progress across goals — feeds goalCompletionPercent. */
  get goalCompletion(): number {
    const withTitle = this.goalList.filter((g) => g.title?.trim());
    if (!withTitle.length) return 0;
    const total = withTitle.reduce((sum, g) => sum + (Number(g.progress) || 0), 0);
    return Math.round(total / withTitle.length);
  }

  // ── CRUD ────────────────────────────────────────────────────

  openCreate(): void {
    this.form = this.emptyForm();
    this.resetContext();
    this.isEdit = false;
    this.selectedId = null;
    this.showForm = true;
  }

  openEdit(r: PerformanceReview): void {
    this.form = {
      employeeId: r.employeeId,
      reviewPeriodStart: r.reviewPeriodStart,
      reviewPeriodEnd: r.reviewPeriodEnd,
      scoreWorkQuality: r.scoreWorkQuality,
      scoreProductivity: r.scoreProductivity,
      scoreCommunication: r.scoreCommunication,
      scoreTeamwork: r.scoreTeamwork,
      scoreLeadership: r.scoreLeadership,
      scoreProblemSolving: r.scoreProblemSolving,
      scoreInnovation: r.scoreInnovation,
      scorePunctuality: r.scorePunctuality,
      strengths: r.strengths,
      areasForImprovement: r.areasForImprovement,
      goalsForNextPeriod: r.goalsForNextPeriod,
      comments: r.comments,
      performanceLevel: r.performanceLevel,
      promotionRecommendation: r.promotionRecommendation,
      promotionReadiness: r.promotionReadiness,
      salaryIncrement: r.salaryIncrement,
      employmentStatusRecommendation: r.employmentStatusRecommendation,
      goalCompletionPercent: r.goalCompletionPercent,
    };
    this.selectedStrengths = this.splitTags(r.strengths);
    this.selectedImprovements = this.splitTags(r.areasForImprovement);
    this.selectedRecognition = this.splitTags(r.recognition);
    this.selectedTraining = this.splitTags(r.trainingRecommendation);
    this.goalList = this.parseGoals(r.goals);

    this.selectedId = r.id;
    this.editingReview = r;
    this.isEdit = true;
    this.showForm = true;
    this.onContextChange();
    this.loadAttachments(r.id);
  }

  save(): void {
    this.saving = true;
    this.error = '';

    const payload: PerformanceReviewRequest = {
      ...this.form,
      strengths: this.joinTags(this.selectedStrengths),
      areasForImprovement: this.joinTags(this.selectedImprovements),
      recognition: this.joinTags(this.selectedRecognition),
      trainingRecommendation: this.joinTags(this.selectedTraining),
      goals: this.goalList.filter((g) => g.title?.trim()).length
        ? JSON.stringify(this.goalList.filter((g) => g.title?.trim()))
        : undefined,
      goalCompletionPercent: this.goalList.length ? this.goalCompletion : this.form.goalCompletionPercent,
    };

    const request = this.isEdit && this.selectedId
      ? this.reviewService.update(this.selectedId, payload)
      : this.reviewService.create(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.isEdit ? 'Performance review updated' : 'Performance review created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save review';
        this.cdr.markForCheck();
      }
    });
  }

  finalise(r: PerformanceReview): void {
    this.reviewService.finalise(r.id).subscribe({
      next: () => {
        this.success = 'Performance review finalised successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to finalise review';
        this.cdr.markForCheck();
      }
    });
  }

  /** Signs off the current approval stage. */
  advance(r: PerformanceReview): void {
    this.reviewService.advanceStage(r.id).subscribe({
      next: (updated) => {
        this.success = updated.finalised
          ? 'Review completed and finalised'
          : `Advanced to ${this.stageLabel(updated.stage)}`;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to advance stage';
        this.cdr.markForCheck();
      },
    });
  }

  stageLabel(stage?: string): string {
    return this.stageOrder.find((s) => s.key === stage)?.label ?? 'Completed';
  }

  /** done | current | pending — drives the timeline markers. */
  stageState(review: PerformanceReview | undefined, stageKey: string): 'done' | 'current' | 'pending' {
    if (!review) return 'pending';
    if (review.finalised) return 'done';
    const order = this.stageOrder.map((s) => s.key);
    const currentIdx = order.indexOf(review.stage ?? 'SELF_ASSESSMENT');
    const thisIdx = order.indexOf(stageKey);
    if (thisIdx < currentIdx) return 'done';
    if (thisIdx === currentIdx) return 'current';
    return 'pending';
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.reviewService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Performance review deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete review';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  summarise(r: PerformanceReview): void {
    if (this.summarisingId) return;
    this.summarisingId = r.id;
    this.summaryError = '';
    this.cdr.markForCheck();
    this.reviewService.summarise(r.id).subscribe({
      next: (res) => {
        r.aiSummary = res.aiSummary;
        this.summarisingId = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.summaryError = err?.error?.message || 'Failed to generate summary';
        this.summarisingId = null;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Attachments ─────────────────────────────────────────────

  private loadAttachments(reviewId: number): void {
    this.reviewService.listAttachments(reviewId).subscribe({
      next: (a) => { this.attachments = a; this.cdr.markForCheck(); },
      error: () => { this.attachments = []; this.cdr.markForCheck(); },
    });
  }

  /**
   * Two steps by design: the binary goes to the shared /upload endpoint, then
   * only the returned URL is recorded against the review.
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.selectedId) return;

    this.uploading = true;
    this.cdr.markForCheck();

    this.fileUpload.upload(file).subscribe({
      next: (res: FileUploadResult) => {
        this.reviewService.addAttachment(this.selectedId!, {
          fileName: file.name,
          fileUrl: res.fileUrl,
          fileType: file.type,
          fileSizeBytes: file.size,
        }).subscribe({
          next: (saved) => {
            this.attachments = [saved, ...this.attachments];
            this.uploading = false;
            input.value = '';
            this.cdr.markForCheck();
          },
          error: (err) => {
            this.error = err?.error?.message || 'Failed to attach the file';
            this.uploading = false;
            this.cdr.markForCheck();
          },
        });
      },
      error: () => {
        this.error = 'File upload failed';
        this.uploading = false;
        this.cdr.markForCheck();
      },
    });
  }

  removeAttachment(a: PerformanceAttachment): void {
    if (!this.selectedId) return;
    this.reviewService.deleteAttachment(this.selectedId, a.id).subscribe({
      next: () => {
        this.attachments = this.attachments.filter((x) => x.id !== a.id);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to remove the attachment';
        this.cdr.markForCheck();
      },
    });
  }

  // ── Helpers ─────────────────────────────────────────────────

  private splitTags(value?: string): string[] {
    return (value ?? '').split(',').map((s) => s.trim()).filter(Boolean);
  }

  private joinTags(list: string[]): string | undefined {
    return list.length ? list.join(', ') : undefined;
  }

  private parseGoals(json?: string): PerformanceGoal[] {
    if (!json) return [];
    try {
      const parsed = JSON.parse(json);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      // Older reviews may hold free text here rather than JSON; don't crash on them.
      return [];
    }
  }

  private resetContext(): void {
    this.selectedEmployee = undefined;
    this.editingReview = undefined;
    this.kpis = null;
    this.kpisError = '';
    this.employeeHistory = [];
    this.attachments = [];
    this.selectedStrengths = [];
    this.selectedImprovements = [];
    this.selectedRecognition = [];
    this.selectedTraining = [];
    this.goalList = [];
  }

  private emptyForm(): PerformanceReviewRequest {
    return {
      employeeId: undefined,
      reviewPeriodStart: '',
      reviewPeriodEnd: '',
      scoreWorkQuality: 5,
      scoreProductivity: 5,
      scoreCommunication: 5,
      scoreTeamwork: 5,
      scoreLeadership: 5,
      scoreProblemSolving: 5,
      scoreInnovation: 5,
      scorePunctuality: 5,
      strengths: '',
      areasForImprovement: '',
      goalsForNextPeriod: '',
      comments: '',
    };
  }
}
