import { Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';
import { Stat } from '../../models/site.model';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [NgClass],
  template: `
    <div class="stat-card text-center p-4">
      <div class="stat-icon mb-3">
        <i class="bi" [ngClass]="stat.icon || 'bi-bar-chart'"></i>
      </div>
      <h3 class="fw-bold mb-1">{{ stat.value }}</h3>
      <p class="text-muted mb-0">{{ stat.label }}</p>
    </div>
  `,
  styles: [`
    .stat-card { background: rgba(var(--site-primary-rgb), 0.04); border-radius: var(--site-radius); transition: all 0.3s; }
    .stat-card:hover { background: rgba(var(--site-primary-rgb), 0.08); transform: translateY(-4px); }
    .stat-icon { width: 60px; height: 60px; border-radius: 16px; background: var(--site-gradient); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 1.3rem; margin: 0 auto; }
    h3 { color: var(--site-primary); }
  `]
})
export class StatCardComponent {
  @Input({ required: true }) stat!: Stat;
}
