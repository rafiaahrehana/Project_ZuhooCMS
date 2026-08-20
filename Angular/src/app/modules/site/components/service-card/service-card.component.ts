import { Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';
import { Service } from '../../models/site.model';

@Component({
  selector: 'app-service-card',
  standalone: true,
  imports: [NgClass],
  template: `
    <div class="card h-100 border-0 shadow-sm service-card">
      <div class="card-body p-4">
        <div class="icon-box mb-3">
          <i class="bi" [ngClass]="service.icon || 'bi-gear'"></i>
        </div>
        <h5 class="card-title fw-bold">{{ service.title }}</h5>
        <p class="card-text text-muted mb-3">{{ service.summary }}</p>
        @if (service.startingPrice) {
          <div class="mb-3"><span class="badge bg-primary bg-opacity-10 text-primary">From {{ service.startingPrice }}</span></div>
        }
        <a [href]="'/services/' + service.slug" class="stretched-link">
          <span class="visually-hidden">View {{ service.title }}</span>
        </a>
      </div>
    </div>
  `,
  styles: [`
    .service-card { border-radius: var(--site-radius); transition: all 0.3s; cursor: pointer; }
    .service-card:hover { transform: translateY(-6px); box-shadow: 0 12px 30px rgba(var(--site-primary-rgb), 0.15) !important; }
    .icon-box { width: 56px; height: 56px; border-radius: 14px; background: var(--site-gradient); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 1.3rem; }
    .bg-primary { background-color: var(--site-primary) !important; }
    .text-primary { color: var(--site-primary) !important; }
  `]
})
export class ServiceCardComponent {
  @Input({ required: true }) service!: Service;
}
