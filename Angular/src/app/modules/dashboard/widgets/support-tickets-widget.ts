import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-support-tickets-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Support Tickets" [value]="summary.openTickets" icon="bi-chat-left-dots"
      variant="dark" [sub]="summary.newTickets + ' new'" link="/support/tickets" />
  `,
})
export class SupportTicketsWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
