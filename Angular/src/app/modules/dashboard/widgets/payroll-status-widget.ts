import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-payroll-status-widget',
  imports: [StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Payroll Processed" [value]="summary.payrollProcessedThisMonth" icon="bi-cash-coin"
      variant="info" [sub]="'of ' + summary.totalEmployees + ' employees this month'" link="/hrm/payroll" />
  `,
})
export class PayrollStatusWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
