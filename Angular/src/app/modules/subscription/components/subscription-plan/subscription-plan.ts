import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { PortalService } from '../../../portal/portal.service';
import { GatewayPaymentService } from '../../../../core/services/gateway-payment.service';
import { SubscriptionPlanService, SubscriptionPlanOption } from '../../services/subscription-plan.service';
import { Loader } from '../../../../shared/components/loader/loader';

@Component({
  selector: 'app-subscription-plan',
  imports: [CommonModule, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './subscription-plan.html',
  styleUrls: ['./subscription-plan.scss'],
})
export class SubscriptionPlan implements OnInit {
  trustBadges = [
    { icon: 'bi-tag', label: 'No setup fees' },
    { icon: 'bi-x-circle', label: 'Cancel anytime' },
    { icon: 'bi-shield-check', label: 'Secure & reliable' },
    { icon: 'bi-headset', label: '24/7 Support' },
  ];
  private static readonly PLAN_ICONS: Record<string, string> = {
    FREE: 'bi-gift',
    STARTER: 'bi-rocket-takeoff',
    PRO: 'bi-lightning-charge-fill',
    ENTERPRISE: 'bi-building',
  };

  loading = false;
  error = '';
  upgradingPlanId: number | null = null;

  currentPlanCode = '';
  currentPlanPrice = 0;
  plans: SubscriptionPlanOption[] = [];

  constructor(
    private portalService: PortalService,
    private planService: SubscriptionPlanService,
    private gatewayPayment: GatewayPaymentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    forkJoin({
      company: this.portalService.getMyCompany(),
      plans: this.planService.list(),
    }).subscribe({
      next: ({ company, plans }) => {
        this.currentPlanCode = company.subscriptionPlan || '';
        this.plans = plans;
        this.currentPlanPrice = plans.find(p => p.code === this.currentPlanCode)?.price ?? 0;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load subscription plans';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  isCurrent(plan: SubscriptionPlanOption): boolean {
    return plan.code === this.currentPlanCode;
  }

  isPopular(plan: SubscriptionPlanOption): boolean {
    return plan.code?.toUpperCase() === 'PRO';
  }

  planIcon(plan: SubscriptionPlanOption): string {
    return SubscriptionPlan.PLAN_ICONS[plan.code?.toUpperCase()] || 'bi-box-seam';
  }

  // Seeded descriptions are comma-separated phrases (e.g. "Email support, up
  // to 10 users, standard integrations.") - split into a feature checklist.
  planFeatures(plan: SubscriptionPlanOption): string[] {
    if (!plan.description) return [];
    return plan.description
      .replace(/\.$/, '')
      .split(',')
      .map(f => f.trim())
      .filter(f => f.length > 0)
      .map(f => f.charAt(0).toUpperCase() + f.slice(1));
  }

  // A plan the company isn't already on and that costs more - matches the
  // backend's own upgrade-eligibility rule (SslCommerzServiceImpl.validateTarget).
  isUpgrade(plan: SubscriptionPlanOption): boolean {
    return !this.isCurrent(plan) && plan.price > this.currentPlanPrice;
  }

  upgrade(plan: SubscriptionPlanOption): void {
    this.upgradingPlanId = plan.id;
    this.error = '';
    this.cdr.markForCheck();
    this.gatewayPayment.redirectToGateway(
      'PLATFORM_SUBSCRIPTION',
      plan.id,
      plan.price,
      (msg) => {
        this.error = msg;
        this.upgradingPlanId = null;
        this.cdr.markForCheck();
      },
    );
  }
}
