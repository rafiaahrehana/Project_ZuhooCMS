import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

export type RunStatus =
  'DRAFT' | 'CALCULATED' | 'PENDING_APPROVAL' | 'APPROVED' | 'PAID' | 'REJECTED' | 'CANCELLED';

export interface PayrollRun {
  id: number;
  runNumber: string;
  payMonth: number;
  payYear: number;
  payPeriodStart: string;
  payPeriodEnd: string;
  paymentDate?: string;
  totalEmployees: number;
  totalGross: number;
  totalDeduction: number;
  totalNet: number;
  status: RunStatus;
  remarks?: string;
  rejectionReason?: string;
  approvedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class PayrollRunService {
  private readonly endpoint = '/hr/payroll-runs';

  constructor(private api: ApiService) {}

  list(): Observable<PayrollRun[]> {
    return this.api.get<PayrollRun[]>(this.endpoint);
  }

  /** Returns null when the period has no run yet (204). */
  forPeriod(month: number, year: number): Observable<PayrollRun | null> {
    return this.api.get<PayrollRun | null>(`${this.endpoint}/period?month=${month}&year=${year}`);
  }

  create(month: number, year: number, remarks?: string): Observable<PayrollRun> {
    return this.api.post<PayrollRun>(this.endpoint, { month, year, remarks });
  }

  recalculate(id: number): Observable<PayrollRun> {
    return this.api.post<PayrollRun>(`${this.endpoint}/${id}/recalculate`, {});
  }

  submit(id: number): Observable<PayrollRun> {
    return this.api.post<PayrollRun>(`${this.endpoint}/${id}/submit`, {});
  }

  approve(id: number): Observable<PayrollRun> {
    return this.api.post<PayrollRun>(`${this.endpoint}/${id}/approve`, {});
  }

  reject(id: number, reason: string): Observable<PayrollRun> {
    return this.api.post<PayrollRun>(`${this.endpoint}/${id}/reject`, { reason });
  }

  cancel(id: number): Observable<PayrollRun> {
    return this.api.post<PayrollRun>(`${this.endpoint}/${id}/cancel`, {});
  }

  pay(id: number, paymentMethod: string, referencePrefix?: string): Observable<PayrollRun> {
    return this.api.post<PayrollRun>(`${this.endpoint}/${id}/pay`, { paymentMethod, referencePrefix });
  }
}
