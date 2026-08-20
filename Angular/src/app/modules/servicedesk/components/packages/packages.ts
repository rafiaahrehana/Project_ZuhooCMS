import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  BILLING_CYCLES,
  CompanyService,
  PackageSubscription,
  ServicePackage,
  ServicePackageRequest,
  SUBSCRIPTION_STATUSES,
  SubscriptionStatus,
} from '../../models/servicedesk.model';
import { GatewayPaymentService } from '../../../../core/services/gateway-payment.service';
import { ServicePackageService } from '../../services/service-package.service';
import { CompanyServiceService } from '../../services/company-service.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
type Tab = 'packages' | 'subscriptions';

@Component({
  selector: 'app-service-packages',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './packages.html',
  styleUrls: ['./packages.scss']
})
export class Packages implements OnInit {
  // VARIABLES
  tab: Tab = 'packages';

  packages: ServicePackage[] = [];
  subscriptions: PackageSubscription[] = [];
  availableServices: CompanyService[] = [];

  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: ServicePackageRequest = { name: '' };

  deleteTarget: ServicePackage | null = null;
  cancelTarget: PackageSubscription | null = null;
  cancelReason = '';

  billingCycles = BILLING_CYCLES;
  subscriptionStatuses = SUBSCRIPTION_STATUSES;

  constructor(
    private packageService: ServicePackageService,
    private gatewayPayment: GatewayPaymentService,
    private serviceService: CompanyServiceService,
    private cdr: ChangeDetectorRef,
  ) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void { this.load(); }

  // SWITCH TAB
  setTab(tab: Tab): void {
    if (this.tab === tab) return;
    this.tab = tab;
    this.page = 0;
    this.load();
  }

  // LOAD BASED ON TAB
  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    if (this.tab === 'packages') {
      this.packageService.list(this.page).subscribe({
        next: (res) => { this.packages = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
        error: () => { this.error = 'Failed to load'; this.loading = false; this.cdr.markForCheck(); }
      });
    } else {
      this.packageService.listSubscriptions(this.page).subscribe({
        next: (res) => { this.subscriptions = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
        error: () => { this.error = 'Failed to load'; this.loading = false; this.cdr.markForCheck(); }
      });
    }
  }

  // LAZY LOAD ACTIVE SERVICES FOR PACKAGE COMPOSITION
  loadServices(): void {
    if (this.availableServices.length) return;
    this.serviceService.listActive().subscribe({
      next: (res) => { this.availableServices = res; this.cdr.markForCheck(); },
      error: () => { this.availableServices = []; this.cdr.markForCheck(); }
    });
  }

  // OPEN CREATE / EDIT PACKAGE FORM
  openCreate(): void {
    this.editingId = null;
    this.form = { name: '', serviceIds: [] };
    this.showForm = true;
    this.loadServices();
  }

  openEdit(p: ServicePackage): void {
    this.editingId = p.id;
    this.form = {
      name: p.name,
      nameBn: p.nameBn,
      description: p.description,
      descriptionBn: p.descriptionBn,
      iconUrl: p.iconUrl,
      packagePrice: p.packagePrice,
      discountPercent: p.discountPercent,
      billingCycle: p.billingCycle,
      requestQuota: p.requestQuota,
      autoRenew: p.autoRenew,
      deliveryDays: p.deliveryDays,
      terms: p.terms,
      featured: p.featured,
      popular: p.popular,
      serviceIds: (p.services || []).map(s => s.id),
    };
    this.showForm = true;
    this.loadServices();
  }

  // SAVE PACKAGE
  save(): void {
    const op = this.editingId
      ? this.packageService.update(this.editingId, this.form)
      : this.packageService.create(this.form);
    op.subscribe({
      next: () => {
        this.success = this.editingId ? 'Package updated' : 'Package created';
        this.showForm = false; this.editingId = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save package'; this.cdr.markForCheck(); }
    });
  }

  // TOGGLE INCLUSION OF A SERVICE IN THE PACKAGE
  toggleService(serviceId: number): void {
    const ids = this.form.serviceIds || [];
    const i = ids.indexOf(serviceId);
    if (i > -1) ids.splice(i, 1); else ids.push(serviceId);
    this.form.serviceIds = ids;
  }

  isServiceSelected(serviceId: number): boolean {
    return (this.form.serviceIds || []).indexOf(serviceId) > -1;
  }

  // SUM OF SELECTED SERVICES' PRICES (used as the auto price when no manual override is set)
  get computedTotalPrice(): number {
    const ids = this.form.serviceIds || [];
    return this.availableServices
      .filter(s => ids.includes(s.id))
      .reduce((sum, s) => sum + (s.price || 0), 0);
  }

  // TOTAL AFTER APPLYING THE DISCOUNT %
  get computedEffectivePrice(): number {
    const discount = this.form.discountPercent || 0;
    return Math.max(0, this.computedTotalPrice * (1 - discount / 100));
  }

  // CLEAR MANUAL PRICE OVERRIDE SO THE PACKAGE USES THE AUTO-CALCULATED PRICE
  useAutoPrice(): void {
    this.form.packagePrice = undefined;
  }

  // TOGGLE ACTIVE
  toggle(p: ServicePackage): void {
    this.packageService.toggle(p.id).subscribe({
      next: () => this.load(),
      error: (err) => { this.error = err?.error?.message || 'Failed to toggle package'; this.cdr.markForCheck(); }
    });
  }

  // DELETE PACKAGE
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.packageService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Package deleted'; this.cdr.markForCheck(); this.load(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete package'; this.cdr.markForCheck(); }
    });
  }

  // SUBSCRIPTION ACTIONS
  activateSubscription(s: PackageSubscription): void {
    this.packageService.activateSubscription(s.id).subscribe({
      next: () => { this.success = 'Subscription activated'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to activate'; this.cdr.markForCheck(); }
    });
  }

  // SSLCommerz checkout for a pending subscription; on validated success the
  // backend activates it (activateForCompany) and redirects to /payment-result
  payOnline(s: PackageSubscription): void {
    if (!s.pricePaid || s.pricePaid <= 0) {
      this.error = 'This subscription has no payable amount - use Activate instead';
      return;
    }
    this.gatewayPayment.redirectToGateway('PACKAGE_SUBSCRIPTION', s.id, s.pricePaid,
      (msg) => { this.error = msg; this.cdr.markForCheck(); });
  }

  suspendSubscription(s: PackageSubscription): void {
    this.packageService.suspendSubscription(s.id).subscribe({
      next: () => { this.success = 'Subscription suspended'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to suspend'; this.cdr.markForCheck(); }
    });
  }

  reactivateSubscription(s: PackageSubscription): void {
    this.packageService.reactivateSubscription(s.id).subscribe({
      next: () => { this.success = 'Subscription reactivated'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to reactivate'; this.cdr.markForCheck(); }
    });
  }

  openCancel(s: PackageSubscription): void {
    this.cancelTarget = s;
    this.cancelReason = '';
  }

  doCancel(): void {
    if (!this.cancelTarget) return;
    this.packageService.cancelSubscription(this.cancelTarget.id, this.cancelReason).subscribe({
      next: () => { this.cancelTarget = null; this.success = 'Subscription cancelled'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to cancel'; this.cancelTarget = null; this.cdr.markForCheck(); }
    });
  }

  // PAGINATION
  goToPage(p: number): void { this.page = p; this.load(); }

  // BADGES
  subscriptionStatusClass(status: SubscriptionStatus): string {
    return {
      ACTIVE: 'text-bg-success',
      PENDING_PAYMENT: 'text-bg-warning',
      SUSPENDED: 'text-bg-secondary',
      EXPIRED: 'text-bg-secondary',
      CANCELLED: 'text-bg-danger',
    }[status] || 'text-bg-secondary';
  }

  // COLOR PALETTES FOR SERVICE PACKAGES
  private colorPalettes = [
    { border: 'rgba(99, 102, 241, 0.2)', hover: 'rgba(99, 102, 241, 0.8)', bg: 'rgba(99, 102, 241, 0.08)', text: '#6366f1' }, // Indigo
    { border: 'rgba(59, 130, 246, 0.2)', hover: 'rgba(59, 130, 246, 0.8)', bg: 'rgba(59, 130, 246, 0.08)', text: '#3b82f6' }, // Blue
    { border: 'rgba(16, 185, 129, 0.2)', hover: 'rgba(16, 185, 129, 0.8)', bg: 'rgba(16, 185, 129, 0.08)', text: '#10b981' }, // Green
    { border: 'rgba(139, 92, 246, 0.2)', hover: 'rgba(139, 92, 246, 0.8)', bg: 'rgba(139, 92, 246, 0.08)', text: '#8b5cf6' }, // Purple
    { border: 'rgba(244, 63, 94, 0.2)', hover: 'rgba(244, 63, 94, 0.8)', bg: 'rgba(244, 63, 94, 0.08)', text: '#f43f5e' }, // Rose
  ];

  getPalette(index: number) {
    return this.colorPalettes[index % this.colorPalettes.length];
  }

  getPackageBorderColor(index: number): string {
    return this.getPalette(index).border;
  }

  getPackageHoverBorderColor(index: number): string {
    return this.getPalette(index).hover;
  }

  getIconBg(index: number): string {
    return this.getPalette(index).bg;
  }

  getIconColor(index: number): string {
    return this.getPalette(index).text;
  }
}
