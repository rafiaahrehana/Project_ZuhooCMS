import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { SiteService } from '../../services/site.service';
import { PricingPlan } from '../../models/site.model';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';
import { PricingCardComponent } from '../../components/pricing-card/pricing-card.component';

@Component({
  selector: 'app-site-pricing',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BreadcrumbComponent, PricingCardComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Pricing' }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container">
        <div class="text-center mb-5">
          <h1 class="fw-bold">Pricing <span style="color: var(--site-primary)">Plans</span></h1>
          <p class="text-muted" style="max-width: 600px; margin: 0 auto">Simple, transparent pricing for every business size.</p>
        </div>
        @if (loading) {
          <div class="text-center py-5"><div class="spinner-border" style="color: var(--site-primary)"></div></div>
        } @else {
          <div class="row g-4 justify-content-center">
            @for (p of plans; track p.id) {
              <div class="col-md-6 col-lg-4"><app-pricing-card [plan]="p"></app-pricing-card></div>
            }
          </div>
        }
      </div>
    </section>
  `
})
export class SitePricingPage implements OnInit {
  private siteService = inject(SiteService);
  constructor(private cdr: ChangeDetectorRef) {}
  plans: PricingPlan[] = [];
  loading = true;
  ngOnInit(): void {
    this.siteService.getPricing().subscribe(p => {
      this.plans = p;
      this.loading = false;
      this.cdr.markForCheck();
    });
  }
}
