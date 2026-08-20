import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { SiteService } from '../../services/site.service';
import { TrackedRequest } from '../../models/site.model';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-track-request',
  standalone: true,
  imports: [FormsModule, BreadcrumbComponent, DatePipe],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Track Request' }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container" style="max-width: 600px">
        <h1 class="fw-bold mb-4 text-center">Track Your <span style="color: var(--site-primary)">Request</span></h1>
        <div class="card border-0 shadow-sm p-4 mb-4" style="border-radius: var(--site-radius)">
          <form (ngSubmit)="track()" class="d-flex gap-3">
            <input type="text" class="form-control" [(ngModel)]="code" name="code" placeholder="Enter tracking code" required
                   style="border-radius: var(--site-btn-radius)">
            <button type="submit" class="btn btn-primary px-4" [disabled]="!code" style="border-radius: var(--site-btn-radius)">
              <i class="bi bi-search"></i>
            </button>
          </form>
        </div>

        @if (error) {
          <div class="alert alert-warning text-center" style="border-radius: var(--site-radius)">{{ error }}</div>
        }

        @if (tracked) {
          <div class="card border-0 shadow-sm p-4" style="border-radius: var(--site-radius)">
            <div class="d-flex justify-content-between align-items-start mb-3">
              <div>
                <h5 class="fw-bold mb-1">{{ tracked.service }}</h5>
                <small class="text-muted">Code: {{ tracked.code }}</small>
              </div>
              <span class="badge" [class.bg-success]="tracked.status === 'COMPLETED'" [class.bg-primary]="tracked.status !== 'COMPLETED'"
                    style="border-radius: var(--site-btn-radius)">{{ tracked.status }}</span>
            </div>
            <small class="text-muted d-block mb-3">Submitted: {{ tracked.submittedAt | date:'medium' }}</small>
            <div class="timeline">
              @for (step of tracked.steps; track step.label) {
                <div class="timeline-step" [class.done]="step.done">
                  <div class="step-dot"></div>
                  <div class="step-label">{{ step.label }}</div>
                </div>
              }
            </div>
          </div>
        }
      </div>
    </section>
  `,
  styles: [`
    .btn-primary { background: var(--site-primary); border-color: var(--site-primary); }
    .timeline { position: relative; padding-left: 30px; }
    .timeline-step { position: relative; padding-bottom: 20px; padding-left: 20px; border-left: 2px solid #e2e8f0; }
    .timeline-step:last-child { border-left-color: transparent; }
    .timeline-step .step-dot { position: absolute; left: -8px; top: 2px; width: 14px; height: 14px; border-radius: 50%; background: #e2e8f0; }
    .timeline-step.done .step-dot { background: var(--site-primary); }
    .timeline-step.done { border-left-color: var(--site-primary); }
    .step-label { font-size: 0.9rem; color: #64748b; }
    .timeline-step.done .step-label { color: var(--site-primary); font-weight: 600; }
  `]
})
export class SiteTrackRequestPage {
  private siteService = inject(SiteService);
  code = '';
  tracked: TrackedRequest | null = null;
  error = '';

  track(): void {
    this.error = '';
    this.tracked = null;
    this.siteService.trackRequest(this.code).subscribe(r => {
      if (r) this.tracked = r;
      else this.error = 'Request not found. Please check your code.';
    });
  }
}
