import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SiteService } from '../../services/site.service';
import { Service, SiteSettings } from '../../models/site.model';
import { DEFAULT_SITE } from '../../services/default-site.config';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-request-service',
  standalone: true,
  imports: [FormsModule, BreadcrumbComponent, RouterLink],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Request a Service' }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container" style="max-width: 700px">
        <h1 class="fw-bold mb-4 text-center">Request a <span style="color: var(--site-primary)">Service</span></h1>
        @if (code) {
          <div class="card border-0 shadow-sm p-5 text-center" style="border-radius: var(--site-radius)">
            <i class="bi bi-check-circle mb-3" style="font-size: 3rem; color: var(--site-primary)"></i>
            <h4 class="fw-bold">Request Submitted!</h4>
            <p class="text-muted">Your tracking code is:</p>
            <div class="code-box mb-3">{{ code }}</div>
            <p class="small text-muted">Save this code to track your request status.</p>
            <a [routerLink]="basePath + '/track-request'" class="btn btn-outline-dark mt-2" style="border-radius: var(--site-btn-radius)">Track Request</a>
          </div>
        } @else {
          <div class="card border-0 shadow-sm p-4" style="border-radius: var(--site-radius)">
            <form (ngSubmit)="submit()">
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label fw-semibold">Full Name *</label>
                  <input type="text" class="form-control" [(ngModel)]="form.name" name="name" required>
                </div>
                <div class="col-md-6">
                  <label class="form-label fw-semibold">Email *</label>
                  <input type="email" class="form-control" [(ngModel)]="form.email" name="email" required>
                </div>
                <div class="col-md-6">
                  <label class="form-label fw-semibold">Phone *</label>
                  <input type="tel" class="form-control" [(ngModel)]="form.phone" name="phone" required>
                </div>
                <div class="col-md-6">
                  <label class="form-label fw-semibold">Service</label>
                  <select class="form-select" [(ngModel)]="form.serviceId" name="serviceId">
                    <option [ngValue]="null">-- Select --</option>
                    @for (s of services; track s.id) {
                      <option [ngValue]="s.id">{{ s.title }}</option>
                    }
                  </select>
                </div>
                <div class="col-12">
                  <label class="form-label fw-semibold">Message *</label>
                  <textarea class="form-control" rows="5" [(ngModel)]="form.message" name="message" required
                            placeholder="Describe what you need..."></textarea>
                </div>
                <div class="col-12">
                  <button type="submit" class="btn btn-primary px-5" [disabled]="submitting" style="border-radius: var(--site-btn-radius)">
                    @if (submitting) { <span class="spinner-border spinner-border-sm me-2"></span> }
                    Submit Request
                  </button>
                </div>
              </div>
            </form>
          </div>
        }
      </div>
    </section>
  `,
  styles: [`
    .btn-primary { background: var(--site-primary); border-color: var(--site-primary); color: #fff; }
    .code-box { display: inline-block; background: rgba(var(--site-primary-rgb), 0.08); color: var(--site-primary); padding: 12px 32px; border-radius: var(--site-radius); font-size: 1.5rem; font-weight: 700; letter-spacing: 3px; font-family: monospace; }
  `]
})
export class SiteRequestServicePage implements OnInit {
  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();
  services: Service[] = [];
  form: { name: string; email: string; phone: string; message: string; serviceId?: number } = { name: '', email: '', phone: '', message: '' };
  submitting = false;
  code = '';

  ngOnInit(): void {
    this.siteService.getServices().subscribe(s => this.services = s);
  }

  submit(): void {
    this.submitting = true;
    this.siteService.submitServiceRequest(this.form).subscribe(r => {
      this.code = r.code;
      this.submitting = false;
    });
  }
}
