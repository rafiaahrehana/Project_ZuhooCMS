import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PricingPlan } from '../../models/site.model';
import { SiteService } from '../../services/site.service';

@Component({
  selector: 'app-pricing-card',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="card h-100 border-0 shadow-sm pricing-card" [class.featured]="plan.featured">
      @if (plan.featured) {
        <div class="featured-badge">Most Popular</div>
      }
      <div class="card-body p-4 d-flex flex-column">
        <h5 class="fw-bold mb-1">{{ plan.name }}</h5>
        <p class="text-muted small mb-3">{{ plan.description }}</p>
        <div class="mb-4">
          <span class="display-6 fw-bold">{{ plan.price }}</span>
          @if (plan.period) {
            <span class="text-muted">{{ plan.period }}</span>
          }
        </div>
        <ul class="list-unstyled mb-4 flex-grow-1">
          @for (f of plan.features; track f) {
            <li class="mb-2"><i class="bi bi-check-circle-fill me-2 text-success"></i>{{ f }}</li>
          }
        </ul>
        <a [routerLink]="basePath + '/request-service'" class="btn w-100 fw-semibold"
           [class.btn-primary]="plan.featured"
           [class.btn-outline-dark]="!plan.featured"
           style="border-radius: var(--site-btn-radius)">
          {{ plan.cta }}
        </a>
      </div>
    </div>
  `,
  styles: [`
    .pricing-card { border-radius: var(--site-radius); transition: all 0.3s; position: relative; overflow: hidden; }
    .pricing-card:hover { transform: translateY(-6px); box-shadow: 0 12px 30px rgba(0,0,0,0.1) !important; }
    .pricing-card.featured { border: 2px solid var(--site-primary); }
    .featured-badge { position: absolute; top: 16px; right: -30px; background: var(--site-primary); color: #fff; padding: 4px 40px; font-size: 0.75rem; font-weight: 700; transform: rotate(45deg); }
    .btn-primary { background: var(--site-primary); border-color: var(--site-primary); color: #fff; }
    .btn-primary:hover { background: var(--site-secondary); border-color: var(--site-secondary); }
    .btn-outline-dark { border-color: var(--site-primary); color: var(--site-primary); }
    .btn-outline-dark:hover { background: var(--site-primary); color: #fff; }
  `]
})
export class PricingCardComponent {
  @Input({ required: true }) plan!: PricingPlan;

  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();
}
