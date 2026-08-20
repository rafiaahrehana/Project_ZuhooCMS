import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { ChartOfAccount } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class CoaService {
  private readonly endpoint = '/company/finance/chart-of-accounts';
  constructor(private api: ApiService) {}
  list(page = 0, size = 50): Observable<PagedResponse<ChartOfAccount>> {
    return this.api.getPaged<ChartOfAccount>(this.endpoint, page, size);
  }
  listByType(type: string): Observable<PagedResponse<ChartOfAccount>> {
    return this.api.getPaged<ChartOfAccount>(`${this.endpoint}/type/${type}`, 0, 100);
  }
  getById(id: number): Observable<ChartOfAccount> {
    return this.api.get<ChartOfAccount>(`${this.endpoint}/${id}`);
  }
  create(payload: Partial<ChartOfAccount>): Observable<ChartOfAccount> {
    return this.api.post<ChartOfAccount>(this.endpoint, payload);
  }
  update(id: number, payload: Partial<ChartOfAccount>): Observable<ChartOfAccount> {
    return this.api.patch<ChartOfAccount>(`${this.endpoint}/${id}`, payload);
  }
  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
