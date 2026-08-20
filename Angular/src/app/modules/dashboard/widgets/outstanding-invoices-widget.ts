import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

import { BosCurrencyPipe } from '../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-outstanding-invoices-widget',
  imports: [BosCurrencyPipe, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Outstanding Invoices" [value]="summary.outstandingInvoiceAmount | bosCurrency:'BDT ':'symbol':'1.0-0'"
      icon="bi-file-earmark-text" [variant]="summary.outstandingInvoiceAmount > 0 ? 'danger' : 'success'" link="/finance/invoices" />
  `,
})
export class OutstandingInvoicesWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
