import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { LeaveBalance, LeaveBalanceRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class LeaveBalanceService {
  private readonly endpoint = '/hr/leave-balances';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20, year?: number): Observable<PagedResponse<LeaveBalance>> {
    const params = year ? { year } : undefined;
    return this.api.getPaged<LeaveBalance>(this.endpoint, page, size, params);
  }

  /**
   * The caller's own balances for a year. list() returns every employee's,
   * which an employee neither needs nor should see.
   */
  listMine(year?: number): Observable<LeaveBalance[]> {
    const q = year ? `?year=${year}` : '';
    return this.api.get<LeaveBalance[]>(`${this.endpoint}/my${q}`);
  }

  create(payload: LeaveBalanceRequest): Observable<LeaveBalance> {
    return this.api.post<LeaveBalance>(this.endpoint, payload);
  }

  update(id: number, payload: LeaveBalanceRequest): Observable<LeaveBalance> {
    return this.api.put<LeaveBalance>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
