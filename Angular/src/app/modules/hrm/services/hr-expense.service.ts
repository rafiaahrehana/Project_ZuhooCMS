import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { HrExpense, HrExpenseRequest } from '../models/hrm.model';

// Expenses are owned by the finance module - this service adapts the HRM
// screens onto /api/company/finance/expenses. Money actions (reimburse,
// delete) are finance-department operations: the backend allows them for
// FINANCE_MANAGER / COMPANY_ADMIN only.
@Injectable({ providedIn: 'root' })
export class HrExpenseService {
  private readonly endpoint = '/company/finance/expenses';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<HrExpense>> {
    return this.api.getPaged<HrExpense>(this.endpoint, page, size)
      .pipe(map((res) => this.normalizePage(res)));
  }

  listMine(page = 0, size = 20): Observable<PagedResponse<HrExpense>> {
    return this.api.getPaged<HrExpense>(`${this.endpoint}/my-expenses`, page, size)
      .pipe(map((res) => this.normalizePage(res)));
  }

  getById(id: number): Observable<HrExpense> {
    return this.api.get<HrExpense>(`${this.endpoint}/${id}`)
      .pipe(map((e) => this.normalize(e)));
  }

  submit(payload: HrExpenseRequest): Observable<HrExpense> {
    // Finance API requires a non-blank description - fall back to the title.
    const body = { ...payload, description: payload.description?.trim() || payload.title };
    return this.api.post<HrExpense>(this.endpoint, body)
      .pipe(map((e) => this.normalize(e)));
  }

  approve(id: number): Observable<void> {
    return this.api.post<void>(
      `${this.endpoint}/${id}/approve?notes=${encodeURIComponent('Approved')}`, {});
  }

  reject(id: number, rejectionReason: string): Observable<void> {
    return this.api.post<void>(
      `${this.endpoint}/${id}/reject?reason=${encodeURIComponent(rejectionReason)}`, {});
  }


  // Finance-department action (COMPANY_ADMIN only).
  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }

  // Map finance ExpenseResponse fields onto the HrExpense view model.
  private normalize(e: any): HrExpense {
    if (!e) return e;
    return {
      ...e,
      status: e.status === 'PAID' ? 'REIMBURSED' : e.status,
      rejectionReason: e.rejectionReason ?? e.approvalNotes,
      reimbursedAt: e.reimbursedAt ?? e.reimbursedDate,
      submittedById: e.submittedById ?? e.employeeId,
    };
  }

  private normalizePage(res: PagedResponse<HrExpense>): PagedResponse<HrExpense> {
    return { ...res, content: (res.content || []).map((e) => this.normalize(e)) };
  }
}
