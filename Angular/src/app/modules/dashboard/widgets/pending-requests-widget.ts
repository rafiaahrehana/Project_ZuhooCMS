import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-pending-requests-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Pending Requests" [value]="summary.pendingRequests" icon="bi-hourglass-split"
      variant="purple" link="/servicedesk/requests" />
  `,
})
export class PendingRequestsWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
