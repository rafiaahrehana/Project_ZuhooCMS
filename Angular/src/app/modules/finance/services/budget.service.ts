import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { Budget, BudgetRequest, FixedAsset, FixedAssetRequest, DepreciationRun } from '../models/finance.model';
import { PagedResponse } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly endpoint = '/company/finance/budgets';
  constructor(private api: ApiService) {}

  listForYear(fiscalYear: number): Observable<Budget[]> {
    return this.api.get<Budget[]>(this.endpoint, { fiscalYear });
  }

  listCategories(): Observable<string[]> {
    return this.api.get<string[]>(`${this.endpoint}/categories`);
  }

  create(payload: BudgetRequest): Observable<Budget> {
    return this.api.post<Budget>(this.endpoint, payload);
  }

  update(id: number, payload: BudgetRequest): Observable<Budget> {
    return this.api.put<Budget>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class FixedAssetService {
  private readonly endpoint = '/company/finance/fixed-assets';
  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<FixedAsset>> {
    return this.api.getPaged<FixedAsset>(this.endpoint, page, size);
  }

  create(payload: FixedAssetRequest): Observable<FixedAsset> {
    return this.api.post<FixedAsset>(this.endpoint, payload);
  }

  dispose(id: number): Observable<FixedAsset> {
    return this.api.post<FixedAsset>(`${this.endpoint}/${id}/dispose`, {});
  }

  runDepreciation(year: number, month: number): Observable<DepreciationRun> {
    return this.api.post<DepreciationRun>(`${this.endpoint}/run-depreciation?year=${year}&month=${month}`, {});
  }

  listRuns(): Observable<DepreciationRun[]> {
    return this.api.get<DepreciationRun[]>(`${this.endpoint}/depreciation-runs`);
  }
}
