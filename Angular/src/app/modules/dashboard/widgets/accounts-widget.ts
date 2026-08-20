import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-accounts-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Total Clients" [value]="summary.totalClients" icon="bi-people" variant="info" link="/crm/clients" />
  `,
})
export class AccountsWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
