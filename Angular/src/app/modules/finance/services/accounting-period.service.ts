import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { AccountingPeriod, FiscalYearSummary } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class AccountingPeriodService {
  private readonly endpoint = '/company/finance/accounting-periods';
  constructor(private api: ApiService) {}

  listForYear(fiscalYear: number): Observable<AccountingPeriod[]> {
    return this.api.get<AccountingPeriod[]>(this.endpoint, { fiscalYear });
  }

  closePeriod(id: number): Observable<AccountingPeriod> {
    return this.api.post<AccountingPeriod>(`${this.endpoint}/${id}/close`, {});
  }

  reopenPeriod(id: number): Observable<AccountingPeriod> {
    return this.api.post<AccountingPeriod>(`${this.endpoint}/${id}/reopen`, {});
  }

  closeFiscalYear(fiscalYear: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/close-fiscal-year?fiscalYear=${fiscalYear}`, {});
  }

  listFiscalYears(): Observable<FiscalYearSummary[]> {
    return this.api.get<FiscalYearSummary[]>(`${this.endpoint}/fiscal-years`);
  }
}
