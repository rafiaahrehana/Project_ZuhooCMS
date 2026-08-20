import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import {
  ChangeStageRequest,
  Client,
  DuplicateMatch,
  LOST_REASONS,
  LostReason,
  Opportunity,
  OpportunityStage,
  OPEN_STAGES,
  PipelineSummary,
  Tag,
} from '../../models/crm.model';
import { OpportunityService } from '../../services/opportunity.service';
import { ClientService } from '../../services/client.service';
import { TagService } from '../../services/tag.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { EmployeeService } from '../../../hrm/services/employee.service';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
// Fixed per-stage color, never reassigned by data (see dataviz skill) - each open
// stage gets a visually distinct dark hue so columns are easy to tell apart at a
// glance, then the reserved status colors (purple/green/red) for the later stages.
const STAGE_COLORS: Record<string, string> = {
  QUALIFICATION: '#1e3a8a', // dark navy blue
  PRESENTATION: '#92400e', // dark amber/brown
  PROPOSAL: '#115e59', // dark teal
  NEGOTIATION: '#7d55fa',
  WON: '#10b981',
  LOST: '#ef4444',
};

@Component({
  selector: 'app-pipeline-board',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, Loader, HasPermissionDirective, DragDropModule, ConfirmDialog],
  templateUrl: './pipeline-board.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './pipeline-board.scss',
})
export class PipelineBoard implements OnInit {
  stages: OpportunityStage[] = [...OPEN_STAGES, 'WON', 'LOST'];
  openStages = OPEN_STAGES;
  columns: Record<string, Opportunity[]> = {};
  summary?: PipelineSummary;

  // Kanban/Table view toggle - same underlying data, different layout.
  viewMode: 'kanban' | 'table' = 'kanban';
  allOpportunities: Opportunity[] = [];
  tableSortField: keyof Opportunity | '' = '';
  tableSortAsc = true;
  loading = false;
  error = '';
  lostReasonFor: Opportunity | null = null;
  lostReason = '';
  lostReasonCode: LostReason | null = null;
  readonly lostReasons = LOST_REASONS;

  // Won-duplicate confirm modal state - shown only when moving a client-less
  // opportunity to WON and a possible-duplicate Client is found beforehand.
  wonDuplicateFor: Opportunity | null = null;
  wonDuplicateMatch: DuplicateMatch | null = null;

  // Create/Edit modal state - null editing means "create"
  showForm = false;
  editing: Opportunity | null = null;
  saving = false;
  form: any = this.emptyForm();
  clients: Client[] = [];
  tags: Tag[] = [];
  tagFilter: number | null = null;

  // Quick "add a new tag" row inside the create/edit form's tag picker
  newTagName = '';
  newTagColor = '#7d55fa';
  addingTag = false;

  constructor(
    private opportunityService: OpportunityService,
    private clientService: ClientService,
    private tagService: TagService,
    private employeeService: EmployeeService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  viewReports(): void {
    this.router.navigate(['/crm/pipeline/reports']);
  }

  stageColor(stage: string): string {
    return STAGE_COLORS[stage] || '#6b7280';
  }

  stageBgClass(stage: string): string {
    return 'stage-bg-' + stage.toLowerCase();
  }

  get lostValue(): number {
    return this.summary?.stages.find((s) => s.stage === 'LOST')?.totalAmount || 0;
  }

  /** For the Owner select - the backend accepts ownerId but no UI ever offered it. */
  employees: { id: number; firstName: string; lastName: string }[] = [];
  deleteTarget: Opportunity | null = null;

  ngOnInit(): void {
    this.load();
    this.clientService.listActive().subscribe({ next: (res) => { this.clients = res; this.cdr.markForCheck(); } });
    this.tagService.list().subscribe({ next: (tags) => { this.tags = tags; this.cdr.markForCheck(); } });
    this.employeeService.list(0, 500).subscribe({
      next: (res: any) => { this.employees = res.content || []; this.cdr.markForCheck(); },
      error: () => {},
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.opportunityService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.cdr.markForCheck(); this.load(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete opportunity';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  private emptyForm(): any {
    return {
      name: '',
      clientId: null,
      description: '',
      stage: 'QUALIFICATION',
      amount: null,
      probability: null,
      expectedCloseDate: '',
      nextStep: '',
      ownerId: null,
      source: '',
      tagIds: [],
    };
  }

  openCreate(): void {
    this.editing = null;
    this.form = this.emptyForm();
    this.showForm = true;
    this.cdr.markForCheck();
  }

  openEdit(opportunity: Opportunity): void {
    this.editing = opportunity;
    this.form = {
      name: opportunity.name,
      clientId: opportunity.clientId,
      description: opportunity.description || '',
      stage: opportunity.stage,
      amount: opportunity.amount ?? null,
      probability: opportunity.probability ?? null,
      expectedCloseDate: opportunity.expectedCloseDate || '',
      nextStep: opportunity.nextStep || '',
      ownerId: opportunity.ownerId ?? null,
      source: opportunity.source || '',
      tagIds: opportunity.tags?.map((t) => t.id) || [],
    };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  save(): void {
    if (!this.form.name?.trim() || (!this.editing && !this.form.clientId)) {
      this.error = 'Name and client are required';
      this.cdr.markForCheck();
      return;
    }
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    const payload: any = {
      name: this.form.name.trim(),
      clientId: this.form.clientId,
      description: this.form.description || undefined,
      amount: this.form.amount ?? undefined,
      probability: this.form.probability ?? undefined,
      expectedCloseDate: this.form.expectedCloseDate || undefined,
      nextStep: this.form.nextStep || undefined,
      ownerId: this.form.ownerId ?? undefined,
      source: this.form.source?.trim() || undefined,
      tagIds: this.form.tagIds,
    };
    // Stage changes on an existing opportunity go through the dedicated /stage endpoint
    // (it records lost reasons and timeline entries), so it's only sent on create.
    if (!this.editing) payload.stage = this.form.stage;

    const obs = this.editing
      ? this.opportunityService.update(this.editing.id, payload)
      : this.opportunityService.create(payload);
    obs.subscribe({
      next: () => {
        this.showForm = false;
        this.saving = false;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save opportunity';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  setTagFilter(tagId: number | null): void {
    this.tagFilter = tagId;
    this.load();
  }

  createTag(): void {
    const name = this.newTagName.trim();
    if (!name || this.addingTag) return;
    this.addingTag = true;
    this.tagService.create({ name, color: this.newTagColor }).subscribe({
      next: (tag) => {
        this.tags = [...this.tags, tag];
        this.form.tagIds = [...this.form.tagIds, tag.id];
        this.newTagName = '';
        this.addingTag = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create tag';
        this.addingTag = false;
        this.cdr.markForCheck();
      },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.opportunityService.list(0, 200, this.tagFilter ? { tagId: this.tagFilter } : undefined).subscribe({
      next: (page) => {
        this.columns = {};
        this.stages.forEach((s) => (this.columns[s] = []));
        page.content.forEach((o) => (this.columns[o.stage] ??= []).push(o));
        this.allOpportunities = page.content;
        this.applyTableSort();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load pipeline';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
    this.opportunityService.pipelineSummary().subscribe({
      next: (s) => { this.summary = s; this.cdr.markForCheck(); },
    });
  }

  // Dropping into a different stage column defers entirely to move() - it already
  // handles the Lost-reason modal, the Won-duplicate pre-check modal, and reverting
  // the optimistic move (via load()) on cancel/error. Dropping within the same column
  // is a cosmetic-only reorder (there's no persisted ordering field per stage).
  onDrop(event: CdkDragDrop<Opportunity[]>, targetStage: OpportunityStage): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }
    const opportunity = event.previousContainer.data[event.previousIndex];
    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
    this.move(opportunity, targetStage);
  }

  move(opportunity: Opportunity, stage: OpportunityStage): void {
    if (stage === opportunity.stage) return;
    if (stage === 'LOST') {
      this.lostReasonFor = opportunity;
      this.lostReason = '';
      this.lostReasonCode = null;
      this.cdr.markForCheck();
      return;
    }
    if (stage === 'WON' && !opportunity.clientId) {
      this.opportunityService.previewWonDuplicate(opportunity.id).subscribe({
        next: (match) => {
          if (match) {
            this.wonDuplicateFor = opportunity;
            this.wonDuplicateMatch = match;
            this.cdr.markForCheck();
          } else {
            this.commitChangeStage(opportunity, stage);
          }
        },
        // Fail open - don't let a preview-check failure block a real stage change.
        error: () => this.commitChangeStage(opportunity, stage),
      });
      return;
    }
    this.commitChangeStage(opportunity, stage);
  }

  private commitChangeStage(opportunity: Opportunity, stage: OpportunityStage, options?: Partial<ChangeStageRequest>): void {
    this.opportunityService.changeStage(opportunity.id, stage, options).subscribe({
      next: () => this.load(),
      error: () => {
        this.error = 'Failed to change stage';
        this.cdr.markForCheck();
        this.load(); // Revert UI dropdown
      },
    });
  }

  cancelLost(): void {
    this.lostReasonFor = null;
    this.lostReason = '';
    this.cdr.markForCheck();
    this.load(); // Revert UI dropdown
  }

  cancelWonDuplicate(): void {
    this.wonDuplicateFor = null;
    this.wonDuplicateMatch = null;
    this.cdr.markForCheck();
    this.load(); // Revert UI dropdown
  }

  linkToExistingClient(): void {
    if (!this.wonDuplicateFor || !this.wonDuplicateMatch) return;
    this.commitChangeStage(this.wonDuplicateFor, 'WON', { linkToExistingClientId: this.wonDuplicateMatch.clientId });
    this.wonDuplicateFor = null;
    this.wonDuplicateMatch = null;
    this.cdr.markForCheck();
  }

  createNewClientAnyway(): void {
    if (!this.wonDuplicateFor) return;
    this.commitChangeStage(this.wonDuplicateFor, 'WON', { forceCreateNewClient: true });
    this.wonDuplicateFor = null;
    this.wonDuplicateMatch = null;
    this.cdr.markForCheck();
  }

  confirmLost(): void {
    if (!this.lostReasonFor || !this.lostReasonCode) return;
    // Mirrors the backend rule: the code is required, text is detail, and
    // OTHER without detail explains nothing.
    if (this.lostReasonCode === 'OTHER' && !this.lostReason.trim()) return;
    this.opportunityService
      .changeStage(this.lostReasonFor.id, 'LOST', {
        lostReasonCode: this.lostReasonCode,
        lostReason: this.lostReason.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.lostReasonFor = null;
          this.cdr.markForCheck();
          this.load();
        },
        error: () => {
          this.error = 'Failed to close opportunity';
          this.lostReasonFor = null;
          this.cdr.markForCheck();
          this.load(); // Revert UI dropdown
        },
      });
  }

  stageLabel(stage: string): string {
    return stage.charAt(0) + stage.slice(1).toLowerCase();
  }

  columnTotal(stage: OpportunityStage): number {
    return (this.columns[stage] || []).reduce((sum, o) => sum + (o.amount || 0), 0);
  }

  setViewMode(mode: 'kanban' | 'table'): void {
    this.viewMode = mode;
    this.cdr.markForCheck();
  }

  sortTable(field: keyof Opportunity): void {
    this.tableSortAsc = this.tableSortField === field ? !this.tableSortAsc : true;
    this.tableSortField = field;
    this.applyTableSort();
    this.cdr.markForCheck();
  }

  sortIcon(field: keyof Opportunity): string {
    if (this.tableSortField !== field) return 'bi-arrow-down-up text-muted';
    return this.tableSortAsc ? 'bi-sort-up' : 'bi-sort-down';
  }

  private applyTableSort(): void {
    const field = this.tableSortField;
    if (!field) return;
    const dir = this.tableSortAsc ? 1 : -1;
    this.allOpportunities = [...this.allOpportunities].sort((a, b) => {
      const av = a[field];
      const bv = b[field];
      if (av == null && bv == null) return 0;
      if (av == null) return -1 * dir;
      if (bv == null) return 1 * dir;
      if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir;
      return (av > bv ? 1 : av < bv ? -1 : 0) * dir;
    });
  }
}
