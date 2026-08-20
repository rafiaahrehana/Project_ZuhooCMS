import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DashboardSummary } from '../../../core/services/dashboard.service';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

import { BosCurrencyPipe } from '../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-wallet-credits-widget',
  imports: [BosCurrencyPipe, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-stat-card label="Wallet Credits" [value]="summary.walletCreditBalance | bosCurrency:'BDT ':'symbol':'1.0-0'"
      icon="bi-cash-stack" variant="success" link="/finance/wallet" />
  `,
})
export class WalletCreditsWidget {
  @Input({ required: true }) summary!: DashboardSummary;
}
