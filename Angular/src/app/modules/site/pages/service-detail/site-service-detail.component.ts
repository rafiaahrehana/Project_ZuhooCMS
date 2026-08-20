import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { SiteService } from '../../services/site.service';
import { Service } from '../../models/site.model';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-service-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BreadcrumbComponent, NgClass, RouterLink],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Services', url: '/services' }, { label: service?.title || 'Service' }]"></app-breadcrumb>
      </div>
    </div>

    @if (loading) {
      <div class="text-center py-5"><div class="spinner-border" style="color: var(--site-primary)"></div></div>
    } @else if (service) {
      <section class="py-5">
        <div class="container">
          <div class="row g-5">
            <div class="col-lg-8">
              <div class="d-flex align-items-center gap-3 mb-4">
                <div class="service-icon"><i class="bi" [ngClass]="service.icon || 'bi-gear'"></i></div>
                <h1 class="fw-bold mb-0">{{ service.title }}</h1>
              </div>
              <p class="lead text-muted mb-4">{{ service.summary }}</p>
              <div class="service-body" [innerHTML]="service.description"></div>

              @if (service.features?.length) {
                <div class="mt-4">
                  <h5 class="fw-bold mb-3">What's Included</h5>
                  <div class="row g-2">
                    @for (f of service.features; track f) {
                      <div class="col-sm-6">
                        <div class="d-flex align-items-center gap-2 p-2 rounded" style="background: rgba(var(--site-primary-rgb), 0.04)">
                          <i class="bi bi-check-circle-fill" style="color: var(--site-primary)"></i>
                          <span>{{ f }}</span>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
            <div class="col-lg-4">
              <div class="card border-0 shadow-sm p-4 sticky-top" style="border-radius: var(--site-radius); top: 100px">
                <h5 class="fw-bold mb-3">Service Details</h5>
                @if (service.startingPrice) {
                  <div class="d-flex justify-content-between py-2 border-bottom">
                    <span class="text-muted">Starting From</span>
                    <strong style="color: var(--site-primary)">{{ service.startingPrice }}</strong>
                  </div>
                }
                @if (service.estimatedTime) {
                  <div class="d-flex justify-content-between py-2 border-bottom">
                    <span class="text-muted">Timeline</span>
                    <strong>{{ service.estimatedTime }}</strong>
                  </div>
                }
                <a [routerLink]="basePath + '/request-service'" class="btn btn-primary w-100 mt-4" style="border-radius: var(--site-btn-radius)">
                  Request This Service <i class="bi bi-arrow-right ms-2"></i>
                </a>
                <a [routerLink]="basePath + '/contact'" class="btn btn-outline-dark w-100 mt-2" style="border-radius: var(--site-btn-radius)">
                  Contact Us
                </a>
              </div>
            </div>
          </div>
        </div>
      </section>
    }
  `,
  styles: [`
    .service-icon { width: 64px; height: 64px; border-radius: 16px; background: var(--site-gradient); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 1.5rem; flex-shrink: 0; }
    .btn-primary { background: var(--site-primary); border-color: var(--site-primary); color: #fff; }
    .btn-primary:hover { background: var(--site-secondary); border-color: var(--site-secondary); }
    .service-body ::ng-deep p { color: #64748b; line-height: 1.8; }
  `]
})
export class SiteServiceDetailPage implements OnInit {
  private route = inject(ActivatedRoute);
  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();
  constructor(private cdr: ChangeDetectorRef) {}
  service: Service | null = null;
  loading = true;

  ngOnInit(): void {
    this.route.params.subscribe(p => {
      this.siteService.getService(p['slug']).subscribe(s => {
        this.service = s;
        this.loading = false;
        this.cdr.markForCheck();
      });
    });
  }
}
