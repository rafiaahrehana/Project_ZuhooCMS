import { Component, Input } from '@angular/core';
import { Testimonial } from '../../models/site.model';

@Component({
  selector: 'app-testimonial-card',
  standalone: true,
  template: `
    <div class="card h-100 border-0 shadow-sm testimonial-card p-4">
      <div class="mb-3">
        @for (s of stars; track s) { <i class="bi bi-star-fill text-warning"></i> }
      </div>
      <p class="mb-4 fst-italic" style="color: var(--site-primary); opacity: 0.85;">"{{ t.quote }}"</p>
      <div class="d-flex align-items-center gap-3">
        <div class="avatar">
          @if (t.avatarUrl) {
            <img [src]="t.avatarUrl" [alt]="t.name" class="rounded-circle" width="48" height="48">
          } @else {
            <div class="avatar-placeholder">{{ t.name.charAt(0) }}</div>
          }
        </div>
        <div>
          <h6 class="mb-0 fw-bold">{{ t.name }}</h6>
          <small class="text-muted">{{ t.role }}{{ t.company ? ', ' + t.company : '' }}</small>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .testimonial-card { border-radius: var(--site-radius); }
    .avatar-placeholder { width: 48px; height: 48px; border-radius: 50%; background: var(--site-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 1.1rem; }
  `]
})
export class TestimonialCardComponent {
  @Input({ required: true }) t!: Testimonial;
  get stars(): number[] {
    return Array(this.t.rating || 5).fill(0);
  }
}
