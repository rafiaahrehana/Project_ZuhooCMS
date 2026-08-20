import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { GeneralLedgerEntry } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class GeneralLedgerService {
  private readonly endpoint = '/company/finance/general-ledger';
  constructor(private api: ApiService) {}

  getById(id: number): Observable<GeneralLedgerEntry> {
    return this.api.get<GeneralLedgerEntry>(`${this.endpoint}/${id}`);
  }

  list(page = 0, size = 20): Observable<PagedResponse<GeneralLedgerEntry>> {
    return this.api.getPaged<GeneralLedgerEntry>(this.endpoint, page, size);
  }

  getByAccount(accountId: number, page = 0, size = 20): Observable<PagedResponse<GeneralLedgerEntry>> {
    return this.api.getPaged<GeneralLedgerEntry>(`${this.endpoint}/account/${accountId}`, page, size);
  }

  getByDateRange(start: string, end: string, page = 0, size = 20): Observable<PagedResponse<GeneralLedgerEntry>> {
    return this.api.getPaged<GeneralLedgerEntry>(`${this.endpoint}/date-range`, page, size, { start, end });
  }

  getAccountBalance(accountId: number): Observable<number> {
    return this.api.get<number>(`${this.endpoint}/account/${accountId}/balance`);
  }

  // Backend expects "notes" as an optional @RequestParam query param, not a JSON body
  reconcile(id: number, notes?: string): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/reconcile${notes ? '?notes=' + encodeURIComponent(notes) : ''}`, {});
  }
}
