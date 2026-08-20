import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

import { BosCurrencyPipe } from '../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-wallet-balance-widget',
  imports: [BosCurrencyPipe, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Wallet Balance" [value]="summary.walletBalance | bosCurrency:'BDT ':'symbol':'1.0-0'"
      icon="bi-wallet2" variant="dark" link="/finance/wallet" />
  `,
})
export class WalletBalanceWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
