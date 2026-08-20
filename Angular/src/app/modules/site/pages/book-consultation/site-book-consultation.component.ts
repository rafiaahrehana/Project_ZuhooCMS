import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SiteService } from '../../services/site.service';
import { SiteSettings } from '../../models/site.model';
import { DEFAULT_SITE } from '../../services/default-site.config';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-book-consultation',
  standalone: true,
  imports: [FormsModule, BreadcrumbComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Book a Consultation' }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container" style="max-width: 700px">
        <h1 class="fw-bold mb-4 text-center">Book a <span style="color: var(--site-primary)">Consultation</span></h1>
        @if (submitted) {
          <div class="card border-0 shadow-sm p-5 text-center" style="border-radius: var(--site-radius)">
            <i class="bi bi-check-circle mb-3" style="font-size: 3rem; color: var(--site-primary)"></i>
            <h4 class="fw-bold">Consultation Booked!</h4>
            <p class="text-muted">We'll contact you shortly to confirm your appointment.</p>
          </div>
        } @else {
          <div class="card border-0 shadow-sm p-4" style="border-radius: var(--site-radius)">
            <form (ngSubmit)="submit()">
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label fw-semibold">Name *</label>
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
                  <label class="form-label fw-semibold">Preferred Date</label>
                  <input type="date" class="form-control" [(ngModel)]="form.preferredDate" name="preferredDate">
                </div>
                <div class="col-12">
                  <label class="form-label fw-semibold">What would you like to discuss? *</label>
                  <textarea class="form-control" rows="5" [(ngModel)]="form.message" name="message" required></textarea>
                </div>
                <div class="col-12">
                  <button type="submit" class="btn btn-primary px-5" [disabled]="submitting" style="border-radius: var(--site-btn-radius)">
                    @if (submitting) { <span class="spinner-border spinner-border-sm me-2"></span> }
                    Book Consultation
                  </button>
                </div>
              </div>
            </form>
          </div>
        }
      </div>
    </section>
  `,
  styles: [`.btn-primary { background: var(--site-primary); border-color: var(--site-primary); color: #fff; }`]
})
export class SiteBookConsultationPage {
  private siteService = inject(SiteService);
  form = { name: '', email: '', phone: '', preferredDate: '', message: '' };
  submitting = false;
  submitted = false;

  submit(): void {
    this.submitting = true;
    this.siteService.submitContact({ ...this.form, subject: 'Consultation Request' }).subscribe(() => {
      this.submitted = true;
      this.submitting = false;
    });
  }
}
