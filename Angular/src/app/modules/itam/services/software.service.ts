import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { SoftwareLicense, SoftwareLicenseSeat } from '../models/itam.model';

@Injectable({ providedIn: 'root' })
export class SoftwareService {
  private readonly endpoint = '/v1/itam/software';
  constructor(private api: ApiService) {}
  list(page = 0, size = 20): Observable<PagedResponse<SoftwareLicense>> { return this.api.getPaged(this.endpoint, page, size); }
  listByStatus(status: string, page = 0): Observable<PagedResponse<SoftwareLicense>> { return this.api.getPaged(`${this.endpoint}/status/${status}`, page, 20); }
  expiring(): Observable<SoftwareLicense[]> { return this.api.get<SoftwareLicense[]>(`${this.endpoint}/expiring`); }
  expired(): Observable<SoftwareLicense[]> { return this.api.get<SoftwareLicense[]>(`${this.endpoint}/expired`); }
  getById(id: number): Observable<SoftwareLicense> { return this.api.get(`${this.endpoint}/${id}`); }
  create(payload: any): Observable<SoftwareLicense> { return this.api.post(this.endpoint, payload); }
  update(id: number, payload: any): Observable<SoftwareLicense> { return this.api.patch(`${this.endpoint}/${id}`, payload); }
  delete(id: number): Observable<void> { return this.api.delete(`${this.endpoint}/${id}`); }
  assignSeat(id: number, employeeId: number): Observable<void> { return this.api.post(`${this.endpoint}/${id}/assign-seat?employeeId=${employeeId}`, {}); }
  releaseSeat(id: number, employeeId: number): Observable<void> { return this.api.post(`${this.endpoint}/${id}/release-seat?employeeId=${employeeId}`, {}); }
  getSeatHolders(id: number): Observable<SoftwareLicenseSeat[]> { return this.api.get(`${this.endpoint}/${id}/seats`); }
  getLicensesForEmployee(employeeId: number): Observable<SoftwareLicenseSeat[]> { return this.api.get(`${this.endpoint}/employee/${employeeId}`); }
}
