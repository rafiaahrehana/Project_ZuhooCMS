import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CrmActivity, CrmActivityType, DuplicateMatch, Lead, Tag } from '../../models/crm.model';
import { LeadService } from '../../services/lead.service';
import { TagService } from '../../services/tag.service';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Employee } from '../../../hrm/models/hrm.model';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { DuplicateWarningModal } from '../../../../shared/components/duplicate-warning-modal/duplicate-warning-modal';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
// Mirror of backend LeadStatus / LeadSource / Priority enums
const LEAD_STATUSES = ['NEW', 'CONTACTED', 'QUALIFIED', 'DISQUALIFIED'] as const;
const LEAD_SOURCES = ['WEBSITE', 'REFERRAL', 'SOCIAL_MEDIA', 'EMAIL', 'PHONE', 'COLD_CALL', 'OTHER'] as const;
const PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'URGENT'] as const;

@Component({
  selector: 'app-leads',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective, DuplicateWarningModal],
  templateUrl: './leads.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './leads.scss',
})
export class Leads implements OnInit {
  leads: Lead[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  sourceFilter = '';
  priorityFilter = '';
  tagFilter: number | null = null;
  keyword = '';
  tags: Tag[] = [];

  // Quick views backed by dedicated backend endpoints
  view: 'ALL' | 'MY' | 'UNASSIGNED' | 'HIGH_PRIORITY' | 'NEVER_CONTACTED' | 'STALE' = 'ALL';
  views = [
    { key: 'ALL' as const, label: 'All' },
    { key: 'MY' as const, label: 'My Leads' },
    { key: 'UNASSIGNED' as const, label: 'Unassigned' },
    { key: 'HIGH_PRIORITY' as const, label: 'High Priority' },
    { key: 'NEVER_CONTACTED' as const, label: 'Never Contacted' },
    { key: 'STALE' as const, label: 'Stale' },
  ];

  activeCount: number | null = null;
  myActiveCount: number | null = null;

  statuses = LEAD_STATUSES;
  sources = LEAD_SOURCES;
  priorities = PRIORITIES;
  employees: Employee[] = [];

  // Create/Edit modal state - null id means "create"
  editing: Lead | null = null;
  showForm = false;
  saving = false;
  form: any = this.emptyForm();

  deleteTarget: Lead | null = null;
  duplicateMatch: DuplicateMatch | null = null;

  // Quick "add a new tag" row inside the create/edit form's tag picker
  newTagName = '';
  newTagColor = '#7d55fa';
  addingTag = false;

  // Convert-to-Opportunity modal state
  convertTarget: Lead | null = null;
  convertForm: any = this.emptyConvertForm();
  converting = false;

  // Lead activities modal state
  activityTarget: Lead | null = null;
  leadActivities: CrmActivity[] = [];
  activitiesLoading = false;
  newLeadActivity: Partial<CrmActivity> = { type: 'NOTE' };
  activityTypes: CrmActivityType[] = ['CALL', 'EMAIL', 'MEETING', 'NOTE', 'TASK', 'FOLLOW_UP'];

  aiSummary = '';
  summarising = false;
  aiSummaryError = '';

  constructor(
    private leadService: LeadService,
    private employeeService: EmployeeService,
    private tagService: TagService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadStats();
    this.employeeService.list(0, 100).subscribe({ next: (res) => {
      this.employees = res.content;
      this.cdr.markForCheck();
    } });
    this.tagService.list().subscribe({ next: (tags) => { this.tags = tags; this.cdr.markForCheck(); } });
  }

  tagColor(tagId: number): string {
    return this.tags.find((t) => t.id === tagId)?.color || '#6b7280';
  }

  private emptyForm(): any {
    return {
      contactName: '', companyName: '', email: '', phone: '', industry: '', jobTitle: '',
      status: 'NEW', source: 'OTHER', sourceOther: '', priority: 'NORMAL',
      estimatedValue: null, expectedCloseDate: null, assignedToId: null, notes: '', tagIds: [],
    };
  }

  onSourceChange(): void {
    if (this.form.source !== 'OTHER') this.form.sourceOther = '';
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

  importing = false;

  /**
   * CSV import. The result message carries both halves - created count and
   * skipped lines with reasons - because a silent partial import reads as a
   * full one until someone counts rows.
   */
  onImportFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // so picking the same file again re-fires change
    if (!file || this.importing) return;

    this.importing = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.leadService.importCsv(file).subscribe({
      next: (res) => {
        this.importing = false;
        this.success = `Imported ${res.created} lead${res.created === 1 ? '' : 's'}.`
          + (res.skipped.length ? ` Skipped ${res.skipped.length}: ${res.skipped.slice(0, 5).join('; ')}`
              + (res.skipped.length > 5 ? ` (and ${res.skipped.length - 5} more)` : '') : '');
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.importing = false;
        this.error = err?.error?.message || 'Import failed - check the file format.';
        this.cdr.markForCheck();
      },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    const obs = {
      MY: () => this.leadService.my(this.page, 20),
      UNASSIGNED: () => this.leadService.unassigned(this.page, 20),
      HIGH_PRIORITY: () => this.leadService.highPriority(this.page, 20),
      NEVER_CONTACTED: () => this.leadService.neverContacted(this.page, 20),
      STALE: () => this.leadService.stale(this.page, 20),
      ALL: () => this.leadService.filter({
        keyword: this.keyword.trim() || null,
        status: this.statusFilter || null,
        source: this.sourceFilter || null,
        priority: this.priorityFilter || null,
        tagId: this.tagFilter || null,
      }, this.page, 20),
    }[this.view]();
    obs.subscribe({
      next: (res) => {
        this.leads = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load leads';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  setView(view: typeof this.view): void {
    this.view = view;
    this.page = 0;
    this.cdr.markForCheck();
    this.load();
  }

  loadStats(): void {
    this.leadService.countActive().subscribe({ next: (n) => {
      this.activeCount = n;
      this.cdr.markForCheck();
    } });
    this.leadService.countMyActive().subscribe({ next: (n) => {
      this.myActiveCount = n;
      this.cdr.markForCheck();
    } });
  }

  openCreate(): void {
    this.editing = null;
    this.form = this.emptyForm();
    this.showForm = true;
    this.cdr.markForCheck();
  }

  openEdit(lead: Lead): void {
    this.editing = lead;
    this.form = {
      contactName: lead.contactName, companyName: lead.companyName || '', email: lead.email || '',
      phone: lead.phone || '', industry: lead.industry || '', jobTitle: lead.jobTitle || '',
      status: lead.status, source: lead.source, sourceOther: lead.sourceOther || '', priority: lead.priority || 'NORMAL',
      estimatedValue: lead.estimatedValue ?? null, expectedCloseDate: lead.expectedCloseDate ?? null,
      assignedToId: lead.assignedToId ?? null, notes: lead.notes || '',
      tagIds: lead.tags?.map((t) => t.id) || [],
    };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  save(): void {
    if (!this.form.contactName?.trim()) {
      this.error = 'Contact name is required';
      this.cdr.markForCheck();
      return;
    }
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    const payload: any = {};
    Object.entries(this.form).forEach(([k, v]) => {
      if (v !== '' && v !== null) payload[k] = v;
    });
    const obs = this.editing
      ? this.leadService.update(this.editing.id, payload)
      : this.leadService.create(payload);
    obs.subscribe({
      next: (res) => {
        this.success = this.editing ? 'Lead updated' : 'Lead created';
        this.saving = false;
        this.showForm = false;
        if (!this.editing && res.possibleDuplicate) {
          this.duplicateMatch = res.possibleDuplicate;
        }
        this.load();
        this.loadStats();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save lead';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.leadService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.success = 'Lead deleted';
        this.deleteTarget = null;
        this.load();
        this.loadStats();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete lead';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  private emptyConvertForm(): any {
    return { opportunityName: '', expectedValue: null, expectedCloseDate: '' };
  }

  openConvert(lead: Lead): void {
    this.convertTarget = lead;
    this.convertForm = {
      opportunityName: (lead.companyName || lead.contactName) + ' — New Opportunity',
      expectedValue: lead.estimatedValue ?? null,
      expectedCloseDate: lead.expectedCloseDate || '',
    };
    this.cdr.markForCheck();
  }

  cancelConvert(): void {
    this.convertTarget = null;
    this.converting = false;
    this.cdr.markForCheck();
  }

  confirmConvert(): void {
    if (!this.convertTarget || !this.convertForm.opportunityName?.trim()
        || !this.convertForm.expectedValue || !this.convertForm.expectedCloseDate) {
      return;
    }
    const lead = this.convertTarget;
    this.converting = true;
    this.cdr.markForCheck();
    this.leadService.convertToOpportunity(lead.id, {
      opportunityName: this.convertForm.opportunityName.trim(),
      expectedValue: this.convertForm.expectedValue,
      expectedCloseDate: this.convertForm.expectedCloseDate,
    }).subscribe({
      next: (opportunity) => {
        this.success = `Lead "${lead.contactName}" converted to opportunity "${opportunity.name}"`;
        this.error = '';
        this.convertTarget = null;
        this.converting = false;
        this.load();
        this.loadStats();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to convert lead to opportunity';
        this.converting = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ----- Lead activities (uses lead-scoped backend endpoints) -----

  openActivities(lead: Lead): void {
    this.activityTarget = lead;
    this.newLeadActivity = { type: 'NOTE' };
    this.aiSummary = '';
    this.aiSummaryError = '';
    this.cdr.markForCheck();
    this.loadLeadActivities();
  }

  summariseLead(): void {
    if (!this.activityTarget || this.summarising) return;
    this.summarising = true;
    this.aiSummaryError = '';
    this.leadService.summarise(this.activityTarget.id).subscribe({
      next: (lead) => {
        this.aiSummary = lead.aiSummary || '';
        this.summarising = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.aiSummaryError = err?.error?.message || 'Failed to generate summary';
        this.summarising = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadLeadActivities(): void {
    if (!this.activityTarget) return;
    this.activitiesLoading = true;
    this.cdr.markForCheck();
    this.leadService.listActivities(this.activityTarget.id, 0, 30).subscribe({
      next: (res) => {
        this.leadActivities = res.content;
        this.activitiesLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load activities';
        this.activitiesLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  logLeadActivity(): void {
    if (!this.activityTarget || !this.newLeadActivity.subject?.trim()) return;
    this.leadService.addActivity(this.activityTarget.id, this.newLeadActivity).subscribe({
      next: () => {
        this.newLeadActivity = { type: 'NOTE' };
        this.loadLeadActivities();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to log activity';
        this.cdr.markForCheck();
      },
    });
  }

  deleteLeadActivity(activity: CrmActivity): void {
    if (!this.activityTarget) return;
    this.leadService.deleteActivity(this.activityTarget.id, activity.id).subscribe({
      next: () => this.loadLeadActivities(),
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete activity';
        this.cdr.markForCheck();
      },
    });
  }

  statusClass(status: string): string {
    return {
      NEW: 'text-bg-primary', CONTACTED: 'text-bg-info', QUALIFIED: 'text-bg-success',
      DISQUALIFIED: 'text-bg-danger',
    }[status] || 'text-bg-secondary';
  }

  goToPage(p: number): void {
    this.page = p;
    this.cdr.markForCheck();
    this.load();
  }

  downloadPdf(): void {
    this.leadService.downloadPdf(this.statusFilter || undefined).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'leads.pdf';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.error = 'Failed to download leads PDF'; this.cdr.markForCheck(); },
    });
  }
}
