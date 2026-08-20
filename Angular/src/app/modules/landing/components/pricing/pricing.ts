import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SubscriptionPlanService, SubscriptionPlanOption } from '../../../subscription/services/subscription-plan.service';

// Presentation-only metadata the billing catalog itself doesn't store (it only
// has code/name/price/description) - feature bullets, the "Most Popular" flag,
// and per-plan CTA copy are a marketing decision, not billing data. Keyed by
// the plan's stable `code` so it survives a Super Admin renaming/repricing a
// plan; a plan with no entry here still renders (via its own `description`),
// it just won't get the curated bullet list.
interface PlanPresentation {
  features: string[];
  highlighted?: boolean;
  ctaText: string;
  ctaLink: string;
}

const PLAN_PRESENTATION: Record<string, PlanPresentation> = {
  FREE: {
    features: ['Full platform access', 'No credit card required', 'Explore all core features'],
    ctaText: 'Start Free Trial',
    ctaLink: '/auth/register',
  },
  STARTER: {
    features: ['Up to 10 team members', 'Core CRM & HRM modules', 'Standard reporting', 'Email support'],
    ctaText: 'Get Started',
    ctaLink: '/auth/register',
  },
  PRO: {
    features: ['Unlimited team members', 'All modules included', 'AI-powered automation', 'Advanced analytics', 'Priority support'],
    highlighted: true,
    ctaText: 'Get Started',
    ctaLink: '/auth/register',
  },
  ENTERPRISE: {
    features: ['Custom integrations', 'Dedicated account manager', 'SLA & premium support', 'Advanced security', 'Onboarding & training'],
    ctaText: 'Contact Sales',
    ctaLink: '/contact',
  },
  // The yearly plan. Its billing row carries no description, so without this
  // entry the card rendered with zero bullets and read as broken next to the
  // other four.
  GROWTH: {
    features: ['Everything in Pro', 'Billed once a year', 'Two months free vs monthly', 'Annual usage review'],
    ctaText: 'Get Started',
    ctaLink: '/auth/register',
  },
};

export interface DisplayPlan {
  code: string;
  name: string;
  priceLabel: string;
  periodLabel: string;
  description?: string;
  features: string[];
  highlighted: boolean;
  ctaText: string;
  ctaLink: string;
}

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pricing.html',
  styleUrls: ['./pricing.scss']
})
export class PricingComponent implements OnInit {
  trustBadges = ['No setup fees', 'Cancel anytime', 'Secure & reliable', '24/7 Support'];

  plans = signal<DisplayPlan[]>([]);
  loading = signal(true);
  error = signal(false);

  constructor(private planService: SubscriptionPlanService) {}

  ngOnInit(): void {
    // Real platform pricing, not hardcoded copy - these are the exact rows
    // SslCommerzServiceImpl charges a company owner for a plan upgrade.
    this.planService.list().subscribe({
      next: (plans) => {
        this.plans.set(plans.map((p) => this.toDisplayPlan(p)));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  private toDisplayPlan(plan: SubscriptionPlanOption): DisplayPlan {
    const presentation = PLAN_PRESENTATION[plan.code];
    const isFree = plan.price === 0;
    return {
      code: plan.code,
      name: plan.name,
      priceLabel: isFree ? 'Free' : this.formatBdt(plan.price),
      periodLabel: isFree
        ? '14 days free'
        : plan.billingCycle === 'YEARLY' ? 'per year' : 'per month',
      description: plan.description,
      features: presentation?.features
        ?? (plan.description ? [plan.description] : []),
      highlighted: presentation?.highlighted ?? false,
      ctaText: presentation?.ctaText ?? 'Get Started',
      ctaLink: presentation?.ctaLink ?? '/auth/register',
    };
  }

  private formatBdt(amount: number): string {
    return '৳' + amount.toLocaleString('en-US', { maximumFractionDigits: 0 });
  }
}
