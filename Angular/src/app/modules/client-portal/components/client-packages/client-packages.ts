import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServicePackageService } from '../../../servicedesk/services/service-package.service';
import { PackageSubscription, ServicePackage } from '../../../servicedesk/models/servicedesk.model';
import { GatewayPaymentService } from '../../../../core/services/gateway-payment.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';

type Tab = 'subscriptions' | 'browse';

@Component({
  selector: 'app-client-packages',
  imports: [BosCurrencyPipe, CommonModule, Pagination, Loader, EmptyState],
  templateUrl: './client-packages.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientPackages implements OnInit {
  tab: Tab = 'subscriptions';

  subscriptions: PackageSubscription[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';

  catalog: ServicePackage[] = [];
  catalogLoading = false;
  // The package.id currently being subscribed - covers both the subscribe()
  // call and the gateway redirect that follows it, so the button can't be
  // double-clicked into two PENDING_PAYMENT subscriptions for the same package.
  subscribingId: number | null = null;

  constructor(
    private packageService: ServicePackageService,
    private gatewayPayment: GatewayPaymentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  setTab(tab: Tab): void {
    this.tab = tab;
    if (tab === 'browse' && !this.catalog.length) this.loadCatalog();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.packageService.mySubscriptions(this.page).subscribe({
      next: (res) => {
        this.subscriptions = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load your packages';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadCatalog(): void {
    this.catalogLoading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.packageService.listActive().subscribe({
      next: (packages) => {
        this.catalog = packages;
        this.catalogLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load the package catalog';
        this.catalogLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  alreadySubscribed(pkg: ServicePackage): boolean {
    return this.subscriptions.some((s) => s.packageId === pkg.id && s.status === 'ACTIVE');
  }

  // subscribe() creates a PENDING_PAYMENT PackageSubscription server-side (one-
  // active-subscription-per-package is enforced there too); the gateway then
  // activates it on a validated success callback, same as invoice payments.
  subscribeAndPay(pkg: ServicePackage): void {
    if (this.subscribingId || this.alreadySubscribed(pkg)) return;
    this.subscribingId = pkg.id;
    this.error = '';
    this.cdr.markForCheck();

    this.packageService.subscribe({ packageId: pkg.id }).subscribe({
      next: (subscription) => {
        this.gatewayPayment.redirectToGateway(
          'PACKAGE_SUBSCRIPTION',
          subscription.id,
          subscription.pricePaid ?? pkg.effectivePrice ?? pkg.packagePrice ?? 0,
          (msg) => {
            this.subscribingId = null;
            this.error = msg;
            this.cdr.markForCheck();
          },
        );
      },
      error: (err) => {
        this.subscribingId = null;
        this.error = err?.error?.message || 'Failed to start subscription';
        this.cdr.markForCheck();
      },
    });
  }

  statusClass(status: string): string {
    return {
      ACTIVE: 'text-bg-success',
      EXPIRED: 'text-bg-secondary',
      SUSPENDED: 'text-bg-warning',
      CANCELLED: 'text-bg-danger',
      PENDING: 'text-bg-info',
    }[status] || 'text-bg-secondary';
  }

  usagePercent(sub: PackageSubscription): number {
    if (!sub.requestQuota) return 0;
    return Math.min(100, Math.round((sub.requestsUsed / sub.requestQuota) * 100));
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
