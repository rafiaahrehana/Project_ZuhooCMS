import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Invoice, InvoiceRequest, CreditNote, CreditNoteRequest } from '../models/finance.model';

export interface RefundRequest {
  id: number;
  clientInvoiceId: number;
  invoiceNumber: string;
  clientId: number;
  clientName?: string;
  serviceRequestId?: number;
  serviceRequestTitle?: string;
  requestedAmount: number;
  status: string;
  reason?: string;
  requestedAt: string;
  processedByName?: string;
  processedAt?: string;
  rejectionReason?: string;
}

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly endpoint = '/company/finance/invoices';
  constructor(private api: ApiService) {}
  listRefunds(status?: string, page = 0, size = 20): Observable<PagedResponse<RefundRequest>> {
    return this.api.getPaged<RefundRequest>(`${this.endpoint}/refunds`, page, size, status ? { status } : undefined);
  }
  processRefund(id: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/refunds/${id}/process`, {});
  }
  rejectRefund(id: number, reason?: string): Observable<void> {
    return this.api.post<void>(
      `${this.endpoint}/refunds/${id}/reject${reason ? '?reason=' + encodeURIComponent(reason) : ''}`, {});
  }
  list(page = 0, size = 20): Observable<PagedResponse<Invoice>> {
    return this.api.getPaged<Invoice>(this.endpoint, page, size);
  }
  listByStatus(status: string, page = 0): Observable<PagedResponse<Invoice>> {
    return this.api.getPaged<Invoice>(`${this.endpoint}/status/${status}`, page, 20);
  }
  overdue(page = 0): Observable<PagedResponse<Invoice>> {
    return this.api.getPaged<Invoice>(`${this.endpoint}/overdue`, page, 20);
  }
  getById(id: number): Observable<Invoice> {
    return this.api.get<Invoice>(`${this.endpoint}/${id}`);
  }

  draftSummaryWithAi(id: number): Observable<{ summary: string }> {
    return this.api.get<{ summary: string }>(`${this.endpoint}/${id}/ai-summary`);
  }
  // Shared by staff and clients - the backend only lets a client download their own.
  downloadPdf(id: number): Observable<Blob> {
    return this.api.getBlob(`${this.endpoint}/${id}/pdf`);
  }
  // Self-service (CLIENT role) - the caller's own invoices
  my(page = 0, size = 20): Observable<PagedResponse<Invoice>> {
    return this.api.getPaged<Invoice>(`${this.endpoint}/me`, page, size);
  }
  create(payload: InvoiceRequest): Observable<Invoice> {
    return this.api.post<Invoice>(this.endpoint, payload);
  }
  update(id: number, payload: InvoiceRequest): Observable<Invoice> {
    return this.api.patch<Invoice>(`${this.endpoint}/${id}`, payload);
  }
  // Only DRAFT invoices can be deleted - anything already sent must be cancelled
  // instead (reverses the ledger postings it made).
  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
  cancel(id: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/cancel`, {});
  }
  send(id: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/send`, {});
  }
  // Backend exposes POST /{id}/record-payment with `amount` as a request
  // param (there is no /mark-as-paid endpoint for invoices). Marking paid
  // records the full outstanding amount as a payment.
  markPaid(id: number, amount: number): Observable<void> {
    return this.api.post<void>(
      `${this.endpoint}/${id}/record-payment?amount=${encodeURIComponent(amount)}`, {});
  }
  recordPayment(id: number, amount: number): Observable<void> {
    return this.api.post<void>(
      `${this.endpoint}/${id}/record-payment?amount=${encodeURIComponent(amount)}`, {});
  }
  issueCreditNote(payload: CreditNoteRequest): Observable<CreditNote> {
    return this.api.post<CreditNote>(`${this.endpoint}/credit-notes`, payload);
  }
  listCreditNotes(invoiceId?: number, page = 0, size = 20): Observable<PagedResponse<CreditNote>> {
    return this.api.getPaged<CreditNote>(`${this.endpoint}/credit-notes`, page, size, invoiceId ? { invoiceId } : undefined);
  }
}
