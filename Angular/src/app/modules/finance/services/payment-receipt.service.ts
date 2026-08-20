import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { PaymentReceipt, PaymentReceiptRequest } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class PaymentReceiptService {
  private readonly endpoint = '/company/finance/payment-receipts';
  constructor(private api: ApiService) {}

  create(payload: PaymentReceiptRequest): Observable<PaymentReceipt> {
    return this.api.post<PaymentReceipt>(this.endpoint, payload);
  }

  getById(id: number): Observable<PaymentReceipt> {
    return this.api.get<PaymentReceipt>(`${this.endpoint}/${id}`);
  }

  list(page = 0, size = 20): Observable<PagedResponse<PaymentReceipt>> {
    return this.api.getPaged<PaymentReceipt>(this.endpoint, page, size);
  }

  // Self-service (CLIENT role) - the caller's own payment receipts
  my(page = 0, size = 20): Observable<PagedResponse<PaymentReceipt>> {
    return this.api.getPaged<PaymentReceipt>(`${this.endpoint}/me`, page, size);
  }

  confirm(id: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/confirm`, {});
  }

  // Backend expects "bank" as a @RequestParam query param, not a JSON body
  markAsDeposited(id: number, bank: string): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/deposit?bank=${encodeURIComponent(bank)}`, {});
  }

  // NSF/chargeback: unwinds the GL and restores the invoice balance
  reverse(id: number, reason?: string): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/reverse${reason ? '?reason=' + encodeURIComponent(reason) : ''}`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
