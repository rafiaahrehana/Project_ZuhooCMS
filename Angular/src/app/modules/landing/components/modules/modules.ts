import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modules',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './modules.html',
  styleUrls: ['./modules.scss']
})
export class ModulesComponent {
  // `accent` keys into the LANDING ACCENT SYSTEM in global.scss - each chip
  // carries its own hue so the radial reads as the whole product spectrum.
  modulesList = signal([
    { title: 'CRM', icon: 'bi-person-lines-fill', accent: 'violet', desc: 'Track sales, pipelines, and customer interactions.' },
    { title: 'HRM', icon: 'bi-people-fill', accent: 'blue', desc: 'Manage employees, payroll, and time off.' },
    { title: 'Finance', icon: 'bi-wallet-fill', accent: 'emerald', desc: 'Accounting, ledgers, and automated invoicing.' },
    { title: 'Inventory', icon: 'bi-box-seam', accent: 'amber', desc: 'Stock control and multi-warehouse management.' },
    { title: 'Procurement', icon: 'bi-cart-check-fill', accent: 'rose', desc: 'Purchase orders and vendor management.' },
    { title: 'Service Desk', icon: 'bi-headset', accent: 'teal', desc: 'ITSM, ticketing, and SLA tracking.' },
    { title: 'Support', icon: 'bi-chat-left-dots-fill', accent: 'indigo', desc: 'Omnichannel customer service portal.' },
    { title: 'AI Assistant', icon: 'bi-stars', accent: 'violet', desc: 'Generative AI for drafting and insights.' },
    { title: 'Documents', icon: 'bi-file-earmark-text-fill', accent: 'lime', desc: 'Secure cloud storage and version control.' },
    { title: 'Workflow Engine', icon: 'bi-arrow-repeat', accent: 'rose', desc: 'Custom state machines and approvals.' },
    { title: 'Analytics', icon: 'bi-bar-chart-fill', accent: 'blue', desc: 'Custom dashboards and real-time reports.' },
    { title: 'Notifications', icon: 'bi-bell-fill', accent: 'amber', desc: 'Push, email, and SMS alerts engine.' }
  ]);

  // The radial: 7 chips on the outer ring, 5 on the inner, each with its
  // resting angle precomputed so the CSS counter-spin can cancel it.
  readonly outer = computed(() =>
    this.modulesList().slice(0, 7).map((m, i) => ({ ...m, angle: Math.round(i * (360 / 7)) })));
  readonly inner = computed(() =>
    this.modulesList().slice(7).map((m, i) => ({ ...m, angle: Math.round(i * (360 / 5) + 36) })));
}
