import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Vendor, VendorRequest, VendorBill, VendorBillRequest, VendorBillStatus, ApAgeingReport } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class VendorService {
  private readonly endpoint = '/company/finance/vendors';
  constructor(private api: ApiService) {}

  list(search?: string, page = 0, size = 20): Observable<PagedResponse<Vendor>> {
    return this.api.getPaged<Vendor>(this.endpoint, page, size, search ? { search } : undefined);
  }

  listActive(): Observable<Vendor[]> {
    return this.api.get<Vendor[]>(`${this.endpoint}/active`);
  }

  create(payload: VendorRequest): Observable<Vendor> {
    return this.api.post<Vendor>(this.endpoint, payload);
  }

  update(id: number, payload: VendorRequest): Observable<Vendor> {
    return this.api.put<Vendor>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<Vendor> {
    return this.api.patch<Vendor>(`${this.endpoint}/${id}/toggle`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class VendorBillService {
  private readonly endpoint = '/company/finance/vendor-bills';
  constructor(private api: ApiService) {}

  list(status?: VendorBillStatus | '', vendorId?: number, page = 0, size = 20): Observable<PagedResponse<VendorBill>> {
    const params: any = {};
    if (status) params.status = status;
    if (vendorId) params.vendorId = vendorId;
    return this.api.getPaged<VendorBill>(this.endpoint, page, size, Object.keys(params).length ? params : undefined);
  }

  create(payload: VendorBillRequest): Observable<VendorBill> {
    return this.api.post<VendorBill>(this.endpoint, payload);
  }

  approve(id: number): Observable<VendorBill> {
    return this.api.post<VendorBill>(`${this.endpoint}/${id}/approve`, {});
  }

  pay(id: number, amount: number): Observable<VendorBill> {
    return this.api.post<VendorBill>(`${this.endpoint}/${id}/pay?amount=${encodeURIComponent(amount)}`, {});
  }

  cancel(id: number): Observable<VendorBill> {
    return this.api.post<VendorBill>(`${this.endpoint}/${id}/cancel`, {});
  }

  apAgeing(asOfDate?: string): Observable<ApAgeingReport> {
    return this.api.get<ApAgeingReport>(`${this.endpoint}/ap-ageing`, asOfDate ? { asOfDate } : undefined);
  }
}
