import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Company,
  SubscriptionPlan,
  SubscriptionPlanDefinition,
  SubscriptionPlanRequest,
  BillingCycle,
} from '../../models/platform-admin.model';
import { CompanyService } from '../../services/company.service';
import { SubscriptionPlanDefinitionService } from '../../services/subscription-plan-definition.service';
import { DashboardService, PlatformSummary } from '../../../../core/services/dashboard.service';
import { extractErrorMessage } from '../../../../core/utils/http-error.util';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-subscription-management',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './subscription-management.html',
})
export class SubscriptionManagement implements OnInit {
  // SUMMARY
  summary: PlatformSummary | null = null;
  loadingSummary = false;

  // PLAN CATALOG - the actual "create/edit/enable-disable plans" management
  plans: SubscriptionPlanDefinition[] = [];
  loadingPlans = false;
  showPlanForm = false;
  editingPlanId: number | null = null;
  planForm: SubscriptionPlanRequest = { code: '', name: '', description: '', billingCycle: 'MONTHLY', price: 0 };

  // COMPANY LIST (filterable by plan - the core "filter companies by subscription
  // plan" requirement; the change-plan action is repeated here so Super Admin
  // doesn't have to jump back to the Companies page for a plan-only workflow)
  companies: Company[] = [];
  totalPages = 0;
  totalCompanies = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  planFilter: SubscriptionPlan | '' = '';

  // PLAN CHANGE (assign a company to a plan)
  planTarget: Company | null = null;
  planAmountPaid: number | null = null;
  planTransactionRef = '';
  newPlan: SubscriptionPlan = '';

  constructor(
    private companyService: CompanyService,
    private planDefinitionService: SubscriptionPlanDefinitionService,
    private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadSummary();
    this.loadPlans();
    this.load();
  }

  loadSummary(): void {
    this.loadingSummary = true;
    this.cdr.markForCheck();
    this.dashboardService.getPlatformSummary().subscribe({
      next: (res) => { this.summary = res; this.loadingSummary = false; this.cdr.markForCheck(); },
      error: () => { this.loadingSummary = false; this.cdr.markForCheck(); },
    });
  }

  // LOAD PLAN CATALOG (all plans, including disabled ones - this is the admin view)
  loadPlans(): void {
    this.loadingPlans = true;
    this.cdr.markForCheck();
    this.planDefinitionService.list(false).subscribe({
      next: (plans) => {
        this.plans = plans;
        this.loadingPlans = false;
        this.cdr.markForCheck();
      },
      error: () => { this.loadingPlans = false; this.cdr.markForCheck(); },
    });
  }

  // OPEN CREATE / EDIT PLAN
  openCreatePlan(): void {
    this.editingPlanId = null;
    this.planForm = { code: '', name: '', description: '', billingCycle: 'MONTHLY', price: 0 };
    this.showPlanForm = true;
  }

  openEditPlan(p: SubscriptionPlanDefinition): void {
    this.editingPlanId = p.id;
    this.planForm = { code: p.code, name: p.name, description: p.description, billingCycle: p.billingCycle, price: p.price };
    this.showPlanForm = true;
  }

  savePlan(): void {
    const op = this.editingPlanId
      ? this.planDefinitionService.update(this.editingPlanId, this.planForm)
      : this.planDefinitionService.create(this.planForm);
    op.subscribe({
      next: () => {
        this.success = this.editingPlanId ? 'Plan updated' : 'Plan created';
        this.showPlanForm = false;
        this.editingPlanId = null;
        this.cdr.markForCheck();
        this.loadPlans();
        this.loadSummary();
      },
      error: (err) => { this.error = extractErrorMessage(err, 'Failed to save plan'); this.cdr.markForCheck(); },
    });
  }

  toggleActive(p: SubscriptionPlanDefinition): void {
    this.planDefinitionService.toggleActive(p.id).subscribe({
      next: () => { this.loadPlans(); this.cdr.markForCheck(); },
      error: (err) => { this.error = extractErrorMessage(err, 'Failed to update plan'); this.cdr.markForCheck(); },
    });
  }

  // COMPANY LIST

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.companyService.list(this.page, 20, undefined, this.planFilter || undefined).subscribe({
      next: (res) => {
        this.companies = res.content;
        this.totalPages = res.totalPages;
        this.totalCompanies = res.totalElements;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.error = 'Failed to load companies'; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  filterByPlan(plan: SubscriptionPlan | ''): void {
    this.planFilter = plan;
    this.page = 0;
    this.load();
  }

  goToPage(p: number): void { this.page = p; this.load(); }

  // OPEN PLAN CHANGE (assign a company to one of the catalog plans)
  openPlanChange(c: Company): void {
    this.planTarget = c;
    this.newPlan = c.subscriptionPlan;
    this.planAmountPaid = this.plans.find(p => p.code === c.subscriptionPlan)?.price ?? null;
    this.planTransactionRef = '';
  }

  onPlanChange(planCode: SubscriptionPlan): void {
    this.planAmountPaid = this.plans.find(p => p.code === planCode)?.price ?? 0;
  }

  doChangePlan(): void {
    if (!this.planTarget) return;
    this.companyService.changePlan(
      this.planTarget.id, this.newPlan,
      this.planAmountPaid ?? undefined,
      this.planTransactionRef || undefined,
    ).subscribe({
      next: () => {
        this.planTarget = null;
        this.success = 'Plan updated';
        this.cdr.markForCheck();
        this.load();
        this.loadSummary();
      },
      error: (err) => {
        this.error = extractErrorMessage(err, 'Failed to change plan');
        this.planTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  // Fixed styling for the four legacy codes; a plan Super Admin adds later falls
  // back to a neutral badge.
  planClass(plan: SubscriptionPlan): string {
    const classes: Record<string, string> = {
      FREE: 'text-bg-light border',
      STARTER: 'text-bg-info',
      PRO: 'text-bg-primary',
      ENTERPRISE: 'text-bg-dark',
    };
    return classes[plan] || 'text-bg-secondary';
  }

  getPlanBorderTop(code: string): string {
    const borders: Record<string, string> = {
      FREE: '4px solid #64748b',
      STARTER: '4px solid #0d9488',
      PRO: '4px solid #7c3aed',
      ENTERPRISE: '4px solid #1e1b4b',
      GROWTH: '4px solid #d97706',
    };
    return borders[code] || '4px solid #3b82f6';
  }

  getPlanBadgeStyle(code: string): { [key: string]: string } {
    const styles: Record<string, { bg: string; color: string }> = {
      FREE: { bg: '#f1f5f9', color: '#475569' },
      STARTER: { bg: '#ccfbf1', color: '#0f766e' },
      PRO: { bg: '#f3e8ff', color: '#6b21a8' },
      ENTERPRISE: { bg: '#e0e7ff', color: '#3730a3' },
      GROWTH: { bg: '#fef3c7', color: '#92400e' },
    };
    const s = styles[code] || { bg: '#e2e8f0', color: '#334155' };
    return {
      'background-color': s.bg,
      'color': s.color,
      'font-weight': '700',
      'letter-spacing': '0.5px'
    };
  }

  getSummaryCardTheme(code: string): { borderTop: string; iconBg: string; iconColor: string; icon: string } {
    const themes: Record<string, { borderTop: string; iconBg: string; iconColor: string; icon: string }> = {
      FREE: { borderTop: '4px solid #64748b', iconBg: '#f1f5f9', iconColor: '#475569', icon: 'bi-box' },
      STARTER: { borderTop: '4px solid #0d9488', iconBg: '#ccfbf1', iconColor: '#0d9488', icon: 'bi-lightning-charge' },
      PRO: { borderTop: '4px solid #7c3aed', iconBg: '#f3e8ff', iconColor: '#7c3aed', icon: 'bi-stars' },
      ENTERPRISE: { borderTop: '4px solid #1e1b4b', iconBg: '#e0e7ff', iconColor: '#1e1b4b', icon: 'bi-shield-check' },
      GROWTH: { borderTop: '4px solid #d97706', iconBg: '#fef3c7', iconColor: '#d97706', icon: 'bi-graph-up-arrow' },
    };
    return themes[code] || { borderTop: '4px solid #3b82f6', iconBg: '#dbeafe', iconColor: '#2563eb', icon: 'bi-box' };
  }
}
