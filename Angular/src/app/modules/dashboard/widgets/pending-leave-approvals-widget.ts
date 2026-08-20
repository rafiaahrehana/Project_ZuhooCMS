import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-pending-leave-approvals-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Pending Leave Approvals" [value]="summary.pendingLeaveApprovals" icon="bi-calendar-minus"
      [variant]="summary.pendingLeaveApprovals > 0 ? 'danger' : 'success'" link="/hrm/leaves" />
  `,
})
export class PendingLeaveApprovalsWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
