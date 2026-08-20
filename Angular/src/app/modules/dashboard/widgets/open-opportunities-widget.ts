import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

import { BosCurrencyPipe } from '../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-open-opportunities-widget',
  imports: [BosCurrencyPipe, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Open Opportunities" [value]="summary.openOpportunities" icon="bi-kanban"
      variant="primary" [sub]="(summary.pipelineValue | bosCurrency:'BDT ':'symbol':'1.0-0') + ' pipeline'" link="/crm/pipeline" />
  `,
})
export class OpenOpportunitiesWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
