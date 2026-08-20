import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-sla-breached-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Service Requests" [value]="summary.totalServiceRequests" icon="bi-clipboard-check"
      variant="purple" link="/servicedesk/requests" />
  `,
})
export class SlaBreachedWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
