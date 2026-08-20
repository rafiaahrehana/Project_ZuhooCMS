import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-total-leads-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Total Leads" [value]="summary.totalLeads" icon="bi-person-plus"
      variant="purple" [sub]="summary.newLeads + ' new, ' + summary.qualifiedLeads + ' qualified'" link="/crm/leads" />
  `,
})
export class TotalLeadsWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
