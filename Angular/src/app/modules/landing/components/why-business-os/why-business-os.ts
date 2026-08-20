import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-why-business-os',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './why-business-os.html',
  styleUrls: ['./why-business-os.scss']
})
export class WhyBusinessOsComponent {
  // The "Engineered for Scale" grid. Six equal glass tiles on the charcoal
  // band - per-card colour fields are gone, the section's --accent channel
  // tints all of them.
  features = signal([
    { title: 'Multi-Tenant SaaS', icon: 'bi-building', desc: 'Isolate data across organizations seamlessly with per-tenant scoping on every query.' },
    { title: 'AI Powered', icon: 'bi-robot', desc: 'Automate workflows with intelligent AI agents that draft, summarize, and learn across your operations.' },
    { title: 'Enterprise Security', icon: 'bi-shield-check', desc: 'End-to-end encryption, automatic key rotation, and role-based access controls.' },
    { title: 'Workflow Automation', icon: 'bi-diagram-3', desc: 'Visual builders engineered to map complex business logic effortlessly.' },
    { title: 'Cloud Native', icon: 'bi-cloud', desc: 'Deploy anywhere with standard Docker and Kubernetes-ready architecture.' },
    { title: 'Scalable Architecture', icon: 'bi-graph-up-arrow', desc: 'Grow without performance hits - scale instances horizontally on demand.' }
  ]);
}
