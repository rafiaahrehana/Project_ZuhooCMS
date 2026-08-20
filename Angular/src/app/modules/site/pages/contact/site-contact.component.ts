import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SiteService } from '../../services/site.service';
import { SiteSettings } from '../../models/site.model';
import { DEFAULT_SITE } from '../../services/default-site.config';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-contact',
  standalone: true,
  imports: [FormsModule, BreadcrumbComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Contact' }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container">
        <div class="row g-5">
          <div class="col-lg-5">
            <h1 class="fw-bold mb-3">Get in <span style="color: var(--site-primary)">Touch</span></h1>
            <p class="text-muted mb-4">We'd love to hear from you. Reach out through any channel.</p>
            <div class="d-flex flex-column gap-3">
              @if (settings.address) {
                <div class="d-flex align-items-start gap-3">
                  <div class="contact-icon"><i class="bi bi-geo-alt"></i></div>
                  <div><strong>Address</strong><p class="text-muted mb-0 small">{{ settings.address }}</p></div>
                </div>
              }
              @if (settings.phone) {
                <div class="d-flex align-items-start gap-3">
                  <div class="contact-icon"><i class="bi bi-telephone"></i></div>
                  <div><strong>Phone</strong><p class="text-muted mb-0 small">{{ settings.phone }}</p></div>
                </div>
              }
              @if (settings.email) {
                <div class="d-flex align-items-start gap-3">
                  <div class="contact-icon"><i class="bi bi-envelope"></i></div>
                  <div><strong>Email</strong><p class="text-muted mb-0 small">{{ settings.email }}</p></div>
                </div>
              }
            </div>
          </div>
          <div class="col-lg-7">
            <div class="card border-0 shadow-sm p-4" style="border-radius: var(--site-radius)">
              @if (submitted) {
                <div class="text-center py-5">
                  <div class="mb-3"><i class="bi bi-check-circle" style="font-size: 3rem; color: var(--site-primary)"></i></div>
                  <h4 class="fw-bold">Message Sent!</h4>
                  <p class="text-muted">We'll get back to you shortly.</p>
                </div>
              } @else {
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
                      <label class="form-label fw-semibold">Phone</label>
                      <input type="tel" class="form-control" [(ngModel)]="form.phone" name="phone">
                    </div>
                    <div class="col-md-6">
                      <label class="form-label fw-semibold">Subject</label>
                      <input type="text" class="form-control" [(ngModel)]="form.subject" name="subject">
                    </div>
                    <div class="col-12">
                      <label class="form-label fw-semibold">Message *</label>
                      <textarea class="form-control" rows="5" [(ngModel)]="form.message" name="message" required></textarea>
                    </div>
                    <div class="col-12">
                      <button type="submit" class="btn btn-primary px-5" [disabled]="submitting" style="border-radius: var(--site-btn-radius)">
                        @if (submitting) { <span class="spinner-border spinner-border-sm me-2"></span> }
                        Send Message
                      </button>
                    </div>
                  </div>
                </form>
              }
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .contact-icon { width: 44px; height: 44px; border-radius: 12px; background: rgba(var(--site-primary-rgb), 0.08); display: flex; align-items: center; justify-content: center; color: var(--site-primary); font-size: 1.1rem; flex-shrink: 0; }
    .btn-primary { background: var(--site-primary); border-color: var(--site-primary); color: #fff; }
    .btn-primary:hover { background: var(--site-secondary); border-color: var(--site-secondary); }
  `]
})
export class SiteContactPage implements OnInit {
  private siteService = inject(SiteService);
  settings: SiteSettings = DEFAULT_SITE;
  form = { name: '', email: '', phone: '', subject: '', message: '' };
  submitted = false;
  submitting = false;

  ngOnInit(): void { this.siteService.getSettings().subscribe(s => this.settings = s); }

  submit(): void {
    this.submitting = true;
    this.siteService.submitContact(this.form).subscribe(() => {
      this.submitted = true;
      this.submitting = false;
    });
  }
}
