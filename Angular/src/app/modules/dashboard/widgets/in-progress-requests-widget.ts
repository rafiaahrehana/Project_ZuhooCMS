import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-in-progress-requests-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="In Progress" [value]="summary.inProgressRequests" icon="bi-arrow-repeat"
      variant="info" [sub]="summary.completedRequestsAllTime + ' completed all-time'" link="/servicedesk/requests" />
  `,
})
export class InProgressRequestsWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
