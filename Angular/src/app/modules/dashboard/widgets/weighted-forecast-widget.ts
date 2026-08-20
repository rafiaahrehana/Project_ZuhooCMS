import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

import { BosCurrencyPipe } from '../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-weighted-forecast-widget',
  imports: [BosCurrencyPipe, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Weighted Forecast" [value]="summary.weightedForecast | bosCurrency:'BDT ':'symbol':'1.0-0'"
      icon="bi-graph-up-arrow" variant="success" link="/crm/pipeline" />
  `,
})
export class WeightedForecastWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
