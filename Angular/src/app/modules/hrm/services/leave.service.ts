import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import {
  LeaveRequest,
  LeaveRequestPayload,
  LeaveRequestStatus,
  ReviewLeavePayload,
  LeaveBalance,
} from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class LeaveService {
  private readonly endpoint = '/hr/leaves';

  constructor(private api: ApiService) {}

  // LIST ALL LEAVE REQUESTS (HR / MANAGER VIEW)
  list(page = 0, size = 20, status?: LeaveRequestStatus): Observable<PagedResponse<LeaveRequest>> {
    const params: Record<string, string | number> = {};
    if (status) params['status'] = status;
    return this.api.getPaged<LeaveRequest>(this.endpoint, page, size, params);
  }

  // LIST OWN LEAVE REQUESTS (EMPLOYEE VIEW)
  listMine(page = 0, size = 20): Observable<PagedResponse<LeaveRequest>> {
    return this.api.getPaged<LeaveRequest>(`${this.endpoint}/my`, page, size);
  }

  // SINGLE LEAVE REQUEST
  getById(id: number): Observable<LeaveRequest> {
    return this.api.get<LeaveRequest>(`${this.endpoint}/${id}`);
  }

  // APPLY FOR LEAVE
  apply(payload: LeaveRequestPayload): Observable<LeaveRequest> {
    return this.api.post<LeaveRequest>(this.endpoint, payload);
  }

  // REVIEW (APPROVE / REJECT)
  review(id: number, payload: ReviewLeavePayload): Observable<LeaveRequest> {
    return this.api.patch<LeaveRequest>(`${this.endpoint}/${id}/review`, payload);
  }

  // CANCEL OWN REQUEST
  cancel(id: number): Observable<string> {
    return this.api.patch<string>(`${this.endpoint}/${id}/cancel`, {});
  }

  // MY LEAVE BALANCES
  myBalances(year?: number): Observable<LeaveBalance[]> {
    const params = year ? { year } : undefined;
    return this.api.get<LeaveBalance[]>(`${this.endpoint}/balances/my`, params);
  }

  // BALANCES FOR ANY EMPLOYEE (HR)
  employeeBalances(employeeId: number, year?: number): Observable<LeaveBalance[]> {
    const params = year ? { year } : undefined;
    return this.api.get<LeaveBalance[]>(`${this.endpoint}/balances/employee/${employeeId}`, params);
  }
}
