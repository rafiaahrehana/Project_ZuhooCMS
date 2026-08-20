import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Expense } from '../../finance/models/finance.model';

// Reuses the Expense model - PlatformExpenseController uses the exact same
// ExpenseRequest/ExpenseResponse DTOs as the company-level ExpenseController,
// just scoped to platform-level spend under /api/platform/finance/expenses.
@Injectable({ providedIn: 'root' })
export class PlatformExpenseService {
  private readonly endpoint = '/platform/finance/expenses';
  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<Expense>> {
    return this.api.getPaged<Expense>(this.endpoint, page, size);
  }

  listByStatus(status: string, page = 0): Observable<PagedResponse<Expense>> {
    return this.api.getPaged<Expense>(`${this.endpoint}/status/${status}`, page, 20);
  }

  getByVendor(vendorName: string, page = 0): Observable<PagedResponse<Expense>> {
    return this.api.getPaged<Expense>(`${this.endpoint}/vendor/${encodeURIComponent(vendorName)}`, page, 20);
  }

  getById(id: number): Observable<Expense> {
    return this.api.get<Expense>(`${this.endpoint}/${id}`);
  }

  create(payload: Partial<Expense>): Observable<Expense> {
    return this.api.post<Expense>(this.endpoint, payload);
  }

  update(id: number, payload: Partial<Expense>): Observable<Expense> {
    return this.api.patch<Expense>(`${this.endpoint}/${id}`, payload);
  }

  approve(id: number, notes: string): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/approve?notes=${encodeURIComponent(notes)}`, {});
  }

  reject(id: number, reason: string): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/reject?reason=${encodeURIComponent(reason)}`, {});
  }

  markAsPaid(id: number, reimbursementMethod?: string, referenceNumber?: string): Observable<void> {
    const params = new URLSearchParams();
    if (reimbursementMethod) params.set('reimbursementMethod', reimbursementMethod);
    if (referenceNumber) params.set('referenceNumber', referenceNumber);
    const qs = params.toString();
    return this.api.post<void>(`${this.endpoint}/${id}/mark-as-paid${qs ? '?' + qs : ''}`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
