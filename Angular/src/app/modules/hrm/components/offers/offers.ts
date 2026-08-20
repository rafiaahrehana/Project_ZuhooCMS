import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PagedResponse } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
import { SalaryComponentsService, SalaryStructureTemplate } from '../../services/salary-components.service';

export interface OfferRow {
  id: number;
  jobApplicationId: number;
  applicantName: string;
  applicantEmail?: string;
  jobPostingTitle?: string;
  applicationStatus?: string;
  offeredJobTitle: string;
  joiningDate?: string;
  expiryDate?: string;
  grossSalary?: number;
  basicSalary?: number;
  houseRent?: number;
  medicalAllowance?: number;
  transportAllowance?: number;
  status: string;
  expired: boolean;
  sentAt?: string;
  decidedAt?: string;
  declineReason?: string;
  notes?: string;
  createdAt: string;
}

@Component({
  selector: 'app-offers',
  imports: [CommonModule, FormsModule, Loader, EmptyState, Pagination, HasPermissionDirective, BosCurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './offers.html',
})
export class Offers implements OnInit {
  offers: OfferRow[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  readonly statuses = ['DRAFT', 'SENT', 'ACCEPTED', 'DECLINED', 'WITHDRAWN'];

  showForm = false;
  saving = false;
  editingId: number | null = null;
  form: any = {};
  applications: { id: number; applicantName: string; jobTitle?: string }[] = [];

  /** Grade templates from Payroll - picking one fills the salary breakdown. */
  templates: SalaryStructureTemplate[] = [];
  selectedTemplateId: number | null = null;

  declineTarget: OfferRow | null = null;
  declineReason = '';
  withdrawTarget: OfferRow | null = null;
  deleteTarget: OfferRow | null = null;
  viewing: OfferRow | null = null;

  constructor(
    private api: ApiService,
    private componentsService: SalaryComponentsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.api.get<PagedResponse<any>>('/recruitment/applications', { page: 0, size: 200 }).subscribe({
      next: (res) => {
        this.applications = (res.content || [])
          .filter((a: any) => !['HIRED', 'REJECTED', 'WITHDRAWN'].includes(a.status))
          .map((a: any) => ({ id: a.id, applicantName: a.candidateName, jobTitle: a.jobPostingTitle }));
        this.cdr.markForCheck();
      },
      error: () => {},
    });
    this.componentsService.templates().subscribe({
      next: (t: SalaryStructureTemplate[]) => { this.templates = t; this.cdr.markForCheck(); },
      error: () => {},
    });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    const params: any = { page: this.page, size: 20 };
    if (this.statusFilter) params.status = this.statusFilter;
    this.api.get<PagedResponse<OfferRow>>('/recruitment/offers', params).subscribe({
      next: (res) => { this.offers = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load offers'; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.selectedTemplateId = null;
    this.form = {};
    this.error = '';
    this.showForm = true;
  }

  openEdit(o: OfferRow): void {
    this.editingId = o.id;
    this.selectedTemplateId = null;
    this.form = {
      jobApplicationId: o.jobApplicationId,
      offeredJobTitle: o.offeredJobTitle,
      joiningDate: o.joiningDate,
      expiryDate: o.expiryDate,
      grossSalary: o.grossSalary,
      basicSalary: o.basicSalary,
      houseRent: o.houseRent,
      medicalAllowance: o.medicalAllowance,
      transportAllowance: o.transportAllowance,
      notes: o.notes,
    };
    this.error = '';
    this.showForm = true;
  }

  /** Grade template fills gross + breakdown, same maths as Salary Structures. */
  onTemplateSelected(): void {
    const t = this.templates.find((x) => x.id === this.selectedTemplateId);
    if (!t) return;
    if (!this.form.offeredJobTitle) this.form.offeredJobTitle = t.structureName;
    if (t.defaultGross) this.form.grossSalary = t.defaultGross;
    const gross = Number(this.form.grossSalary) || 0;
    if (gross > 0 && t.id != null) {
      this.componentsService.breakdown(t.id, gross).subscribe({
        next: (b) => {
          this.form.basicSalary = b.basicSalary;
          this.form.houseRent = b.houseRent;
          this.form.medicalAllowance = b.medicalAllowance;
          this.form.transportAllowance = b.transportAllowance;
          this.cdr.markForCheck();
        },
        error: () => {},
      });
    }
  }

  save(): void {
    if (!this.editingId && !this.form.jobApplicationId) { this.error = 'Pick an application'; return; }
    if (!this.form.offeredJobTitle?.trim()) { this.error = 'Offered job title is required'; return; }
    this.saving = true;
    this.error = '';
    const op = this.editingId
      ? this.api.put<OfferRow>(`/recruitment/offers/${this.editingId}`, this.form)
      : this.api.post<OfferRow>('/recruitment/offers', this.form);
    op.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.editingId = null;
        this.success = 'Offer saved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.saving = false; this.error = err?.error?.message || 'Failed to save offer'; this.cdr.markForCheck(); },
    });
  }

  action(o: OfferRow, verb: 'send' | 'accept'): void {
    this.api.patch<OfferRow>(`/recruitment/offers/${o.id}/${verb}`, {}).subscribe({
      next: () => { this.success = verb === 'send' ? 'Offer sent' : 'Offer accepted'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Action failed'; this.cdr.markForCheck(); },
    });
  }

  doDecline(): void {
    if (!this.declineTarget) return;
    this.api.patch<OfferRow>(`/recruitment/offers/${this.declineTarget.id}/decline`, { reason: this.declineReason || undefined }).subscribe({
      next: () => { this.declineTarget = null; this.declineReason = ''; this.success = 'Offer marked declined'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.declineTarget = null; this.cdr.markForCheck(); },
    });
  }

  doWithdraw(): void {
    if (!this.withdrawTarget) return;
    this.api.patch<OfferRow>(`/recruitment/offers/${this.withdrawTarget.id}/withdraw`, {}).subscribe({
      next: () => { this.withdrawTarget = null; this.success = 'Offer withdrawn'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.withdrawTarget = null; this.cdr.markForCheck(); },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.api.delete(`/recruitment/offers/${this.deleteTarget.id}`).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Draft deleted'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.deleteTarget = null; this.cdr.markForCheck(); },
    });
  }

  goToPage(p: number): void { this.page = p; this.load(); }

  statusBadge(o: OfferRow): string {
    if (o.expired) return 'text-bg-warning';
    return {
      DRAFT: 'text-bg-secondary', SENT: 'text-bg-info', ACCEPTED: 'text-bg-success',
      DECLINED: 'text-bg-danger', WITHDRAWN: 'text-bg-light border',
    }[o.status] || 'text-bg-light';
  }

  statusLabel(o: OfferRow): string {
    return o.expired ? 'EXPIRED' : o.status;
  }
}
