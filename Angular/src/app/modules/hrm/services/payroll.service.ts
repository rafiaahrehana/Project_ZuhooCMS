import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Payroll, CreatePayrollRequest, BulkPayrollResult } from '../models/hrm.model';
import { PaymentMethod } from '../../finance/models/finance.model';

@Injectable({ providedIn: 'root' })
export class PayrollService {
  private readonly endpoint = '/hr/payroll';

  constructor(private api: ApiService) {}

  listByPeriod(month: number, year: number, page = 0, size = 50): Observable<PagedResponse<Payroll>> {
    return this.api.getPaged<Payroll>(this.endpoint, page, size, { month, year });
  }

  listForEmployee(employeeId: number, page = 0, size = 24): Observable<PagedResponse<Payroll>> {
    return this.api.getPaged<Payroll>(`${this.endpoint}/employee/${employeeId}`, page, size);
  }

  getById(id: number): Observable<Payroll> {
    return this.api.get<Payroll>(`${this.endpoint}/${id}`);
  }

  create(payload: CreatePayrollRequest): Observable<Payroll> {
    return this.api.post<Payroll>(this.endpoint, payload);
  }

  /**
   * Bank disbursement sheet for a period — APPROVED payrolls only.
   * Downloading it moves no money and changes no payroll status; it is the file
   * finance uploads to the bank's corporate portal for bulk salary transfer.
   */
  disbursementCsv(month: number, year: number): Observable<Blob> {
    return this.api.getBlob(`${this.endpoint}/disbursement?month=${month}&year=${year}`);
  }

  /**
   * Payslip PDF for one payroll record.
   *
   * The backend scopes this: PAYROLL_VIEW downloads anyone's, everyone else
   * only their own, so this is safe to call from the employee's own page.
   */
  payslipPdf(id: number): Observable<Blob> {
    return this.api.getBlob(`${this.endpoint}/${id}/payslip`);
  }

  generateForAll(month: number, year: number): Observable<BulkPayrollResult> {
    return this.api.post<BulkPayrollResult>(`${this.endpoint}/generate?month=${month}&year=${year}`, {});
  }

  approve(id: number): Observable<Payroll> {
    return this.api.patch<Payroll>(`${this.endpoint}/${id}/approve`, {});
  }

  markPaid(id: number, paymentReference?: string, paymentMethod?: PaymentMethod): Observable<Payroll> {
    const params = new URLSearchParams();
    if (paymentReference) params.set('paymentReference', paymentReference);
    if (paymentMethod) params.set('paymentMethod', paymentMethod);
    const query = params.toString() ? `?${params.toString()}` : '';
    return this.api.patch<Payroll>(`${this.endpoint}/${id}/pay${query}`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
