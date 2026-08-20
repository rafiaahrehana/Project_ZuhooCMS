import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ProfitLossReport, BalanceSheetReport, TrialBalanceReport, AgeingReport, CashFlowReport, AccountLedgerReport } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class FinancialReportService {
  private readonly endpoint = '/company/finance/reports';
  constructor(private api: ApiService) {}

  profitLoss(startDate: string, endDate: string): Observable<ProfitLossReport> {
    return this.api.get<ProfitLossReport>(`${this.endpoint}/profit-loss`, { startDate, endDate });
  }

  balanceSheet(asOfDate: string): Observable<BalanceSheetReport> {
    return this.api.get<BalanceSheetReport>(`${this.endpoint}/balance-sheet`, { asOfDate });
  }

  trialBalance(asOfDate: string): Observable<TrialBalanceReport> {
    return this.api.get<TrialBalanceReport>(`${this.endpoint}/trial-balance`, { asOfDate });
  }

  ageing(asOfDate: string): Observable<AgeingReport> {
    return this.api.get<AgeingReport>(`${this.endpoint}/ageing`, { asOfDate });
  }

  cashFlow(startDate: string, endDate: string): Observable<CashFlowReport> {
    return this.api.get<CashFlowReport>(`${this.endpoint}/cash-flow`, { startDate, endDate });
  }

  accountLedger(accountId: number, startDate: string, endDate: string): Observable<AccountLedgerReport> {
    return this.api.get<AccountLedgerReport>(`${this.endpoint}/ledger`, { accountId, startDate, endDate });
  }
}
