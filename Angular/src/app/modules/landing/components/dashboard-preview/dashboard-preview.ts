import { Component, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * The product showcase. A light section holding one dark navy card - a
 * browser-framed miniature of the real dashboard built from the
 * DEMO TENANT'S ACTUAL NUMBERS - every figure here is
 * what a visitor sees when they click through to the live demo, which is the
 * whole point: the section is a preview, not an illustration.
 */
@Component({
  selector: 'app-dashboard-preview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-preview.html',
  styleUrls: ['./dashboard-preview.scss']
})
export class DashboardPreviewComponent {
  startingDemo = signal(false);

  // Mirrors the seeded Dhrubotara tenant. If DemoDataSeeder's figures change,
  // change these - a preview that disagrees with the demo it opens is worse
  // than no preview.
  readonly stats = [
    { label: 'Employees', value: '10', icon: 'bi-people' },
    { label: 'Open pipeline', value: '৳28.0L', icon: 'bi-graph-up-arrow' },
    { label: 'Net payroll / month', value: '৳9.4L', icon: 'bi-cash-stack' },
    { label: 'Outstanding invoices', value: '৳2.9L', icon: 'bi-receipt' },
  ];

  readonly deals = [
    { name: 'ERP for garments unit', client: 'Padma Textiles', amount: '৳12,00,000', stage: 'Negotiation', pct: 80 },
    { name: 'Hospital management system', client: 'City General Hospital', amount: '৳8,50,000', stage: 'Proposal', pct: 55 },
    { name: 'Grocery delivery app', client: 'Meghna Agro Foods', amount: '৳6,00,000', stage: 'Presentation', pct: 35 },
  ];

  constructor(
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  seeDemo(): void {
    if (this.startingDemo()) return;
    this.startingDemo.set(true);
    this.auth.startDemo().subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.startingDemo.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
