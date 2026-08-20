import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-total-employees-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Total Employees" [value]="summary.totalEmployees" icon="bi-person-vcard"
      variant="purple" link="/hrm/employees" />
  `,
})
export class TotalEmployeesWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
