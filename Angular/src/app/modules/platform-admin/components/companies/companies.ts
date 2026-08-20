import { ActivatedRoute } from '@angular/router';
import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Company,
  COMPANY_STATUSES,
  CompanyStatus,
  RegisterCompanyRequest,
  SubscriptionPlan,
  SubscriptionPlanDefinition,
} from '../../models/platform-admin.model';
import { CompanyService } from '../../services/company.service';
import { SubscriptionPlanDefinitionService } from '../../services/subscription-plan-definition.service';
import { AuthService } from '../../../../core/services/auth.service';
import { extractErrorMessage } from '../../../../core/utils/http-error.util';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-companies',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './companies.html',
})
export class Companies implements OnInit {
  // VARIABLES
  companies: Company[] = [];
  totalPages = 0;
  totalCompanies = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  statusFilter: CompanyStatus | '' = '';
  planFilter: SubscriptionPlan | '' = '';
  keyword = '';

  // KPI COUNTS DERIVED FROM CURRENT LOAD
  kpi = { total: 0, active: 0, trial: 0, suspended: 0 };
  loadingKpi = false;

  // REGISTER MODAL
  showRegister = false;
  form: RegisterCompanyRequest = {
    companyName: '', subdomain: '', ownerFirstName: '', ownerLastName: '', ownerEmail: '', ownerPassword: ''
  };

  // PLAN CHANGE
  planTarget: Company | null = null;
  planAmountPaid: number | null = null;
  planTransactionRef = '';
  newPlan: SubscriptionPlan = 'STARTER';

  // STATUS CHANGE
  statusTarget: Company | null = null;
  newStatus: CompanyStatus = 'ACTIVE';

  deactivateTarget: Company | null = null;

  // ACCESS COMPANY (impersonate)
  impersonateTarget: Company | null = null;
  impersonateReason = '';
  impersonating = false;

  statuses = COMPANY_STATUSES;
  plans: SubscriptionPlanDefinition[] = [];

  constructor(
    private companyService: CompanyService,
    private planDefinitionService: SubscriptionPlanDefinitionService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void {
    // Dashboard stat cards deep-link here pre-filtered (?status=TRIAL, ?plan=PRO ...)
    const qp = this.route.snapshot.queryParamMap;
    this.statusFilter = (qp.get('status') as CompanyStatus) || '';
    this.planFilter = (qp.get('plan') as SubscriptionPlan) || '';
    this.keyword = qp.get('keyword') || '';
    this.load();
    this.loadKpi();
    // Includes inactive plans - a company can already be on a plan Super Admin
    // has since disabled, and it still needs to show correctly in the filter/badges.
    this.planDefinitionService.list(false).subscribe({
      next: (plans) => { this.plans = plans; this.cdr.markForCheck(); },
      error: () => {},
    });
  }

  // LOAD COMPANIES (RESPECTS STATUS FILTER)
  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.companyService.list(this.page, 20, this.statusFilter || undefined, this.planFilter || undefined, this.keyword || undefined).subscribe({
      next: (res) => {
        this.companies = res.content;
        this.totalPages = res.totalPages;
        this.totalCompanies = res.totalElements;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.error = 'Failed to load companies'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // LOAD KPI COUNTS BY FETCHING COUNTS PER STATUS (SEPARATE FROM MAIN LIST)
  // Uses a single unfiltered page-1 call for total, and per-status calls for the breakdown
  loadKpi(): void {
    this.loadingKpi = true;
    this.cdr.markForCheck();
    this.companyService.list(0, 1).subscribe({
      next: (res) => { this.kpi.total = res.totalElements; this.tryFinishKpi(); },
      error: () => { this.loadingKpi = false; this.cdr.markForCheck(); }
    });
    this.companyService.list(0, 1, 'ACTIVE').subscribe({
      next: (res) => { this.kpi.active = res.totalElements; this.tryFinishKpi(); },
      error: () => { /* no-op */ }
    });
    this.companyService.list(0, 1, 'TRIAL').subscribe({
      next: (res) => { this.kpi.trial = res.totalElements; this.tryFinishKpi(); },
      error: () => { /* no-op */ }
    });
    this.companyService.list(0, 1, 'SUSPENDED').subscribe({
      next: (res) => { this.kpi.suspended = res.totalElements; this.tryFinishKpi(); },
      error: () => { /* no-op */ }
    });
  }

  private kpiReady = 0;
  private tryFinishKpi(): void {
    this.kpiReady++;
    if (this.kpiReady >= 4) { this.loadingKpi = false; this.cdr.markForCheck(); }
  }

  // OPEN REGISTER MODAL
  openRegister(): void {
    this.form = { companyName: '', subdomain: '', ownerFirstName: '', ownerLastName: '', ownerEmail: '', ownerPassword: '' };
    this.showRegister = true;
  }

  // REGISTER COMPANY
  register(): void {
    this.companyService.register(this.form).subscribe({
      next: () => { this.showRegister = false; this.success = 'Company registered'; this.cdr.markForCheck(); this.load(); this.kpiReady = 0; this.loadKpi(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to register company'; this.cdr.markForCheck(); }
    });
  }

  // OPEN PLAN CHANGE
  openPlanChange(c: Company): void {
    this.planTarget = c;
    this.newPlan = c.subscriptionPlan;
    this.onPlanChange(this.newPlan);
    this.planTransactionRef = '';
  }

  // Defaults the amount-paid field to the plan's catalog price - still freely
  // editable, e.g. for a negotiated discount.
  onPlanChange(planCode: SubscriptionPlan): void {
    this.planAmountPaid = this.plans.find(p => p.code === planCode)?.price ?? 0;
  }

  doChangePlan(): void {
    if (!this.planTarget) return;
    this.companyService.changePlan(
      this.planTarget.id, this.newPlan,
      this.planAmountPaid ?? undefined,
      this.planTransactionRef || undefined
    ).subscribe({
      next: () => { this.planTarget = null; this.success = 'Plan updated'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to change plan'; this.planTarget = null; this.cdr.markForCheck(); }
    });
  }

  // OPEN STATUS CHANGE
  openStatusChange(c: Company): void {
    this.statusTarget = c;
    this.newStatus = c.status;
  }

  doChangeStatus(): void {
    if (!this.statusTarget) return;
    this.companyService.changeStatus(this.statusTarget.id, this.newStatus).subscribe({
      next: () => { this.statusTarget = null; this.success = 'Status updated'; this.cdr.markForCheck(); this.load(); this.kpiReady = 0; this.loadKpi(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to change status'; this.statusTarget = null; this.cdr.markForCheck(); }
    });
  }

  // DEACTIVATE
  doDeactivate(): void {
    if (!this.deactivateTarget) return;
    this.companyService.deactivate(this.deactivateTarget.id).subscribe({
      next: () => { this.deactivateTarget = null; this.success = 'Company deactivated'; this.cdr.markForCheck(); this.load(); this.kpiReady = 0; this.loadKpi(); },
      error: (err) => { this.deactivateTarget = null; this.error = extractErrorMessage(err, 'Cannot deactivate company'); this.cdr.markForCheck(); }
    });
  }

  // ACCESS COMPANY (impersonate)
  openImpersonate(c: Company): void {
    this.impersonateTarget = c;
    this.impersonateReason = '';
  }

  doImpersonate(): void {
    if (!this.impersonateTarget || !this.impersonateReason.trim()) return;
    this.impersonating = true;
    this.cdr.markForCheck();
    this.companyService.impersonate(this.impersonateTarget.id, this.impersonateReason.trim()).subscribe({
      next: (res) => {
        this.impersonating = false;
        this.impersonateTarget = null;
        this.auth.startImpersonation(res);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.impersonating = false;
        this.error = err?.error?.message || 'Failed to access company';
        this.impersonateTarget = null;
        this.cdr.markForCheck();
      }
    });
  }

  // PAGINATION
  goToPage(p: number): void { this.page = p; this.load(); }

  // BADGE STYLING
  statusClass(status: CompanyStatus): string {
    return {
      PENDING_VERIFICATION: 'text-bg-secondary',
      TRIAL: 'text-bg-warning',
      ACTIVE: 'text-bg-success',
      SUSPENDED: 'text-bg-danger',
      DEACTIVATED: 'text-bg-secondary',
    }[status];
  }

  // Fixed styling for the four legacy codes; any plan Super Admin adds later
  // (a code this map doesn't know) falls back to a neutral badge.
  planClass(plan: SubscriptionPlan): string {
    const classes: Record<string, string> = {
      FREE: 'text-bg-light border',
      STARTER: 'text-bg-info',
      PRO: 'text-bg-primary',
      ENTERPRISE: 'text-bg-dark',
    };
    return classes[plan] || 'text-bg-secondary';
  }
}
