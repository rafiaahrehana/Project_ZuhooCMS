import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { BiometricDevice, BiometricDeviceRequest, BiometricDeviceStatus } from '../models/attendance.model';

@Injectable({ providedIn: 'root' })
export class BiometricDeviceService {
  private readonly endpoint = '/company/biometric/devices';
  constructor(private api: ApiService) {}

  create(payload: BiometricDeviceRequest): Observable<BiometricDevice> {
    return this.api.post<BiometricDevice>(this.endpoint, payload);
  }

  getById(id: number): Observable<BiometricDevice> {
    return this.api.get<BiometricDevice>(`${this.endpoint}/${id}`);
  }

  list(page = 0, size = 20): Observable<PagedResponse<BiometricDevice>> {
    return this.api.getPaged<BiometricDevice>(this.endpoint, page, size);
  }

  getByStatus(status: BiometricDeviceStatus, page = 0, size = 20): Observable<PagedResponse<BiometricDevice>> {
    return this.api.getPaged<BiometricDevice>(`${this.endpoint}/status/${status}`, page, size);
  }

  getOnline(): Observable<BiometricDevice[]> {
    return this.api.get<BiometricDevice[]>(`${this.endpoint}/online`);
  }

  update(id: number, payload: BiometricDeviceRequest): Observable<BiometricDevice> {
    return this.api.patch<BiometricDevice>(`${this.endpoint}/${id}`, payload);
  }

  updateStatus(id: number, status: BiometricDeviceStatus): Observable<void> {
    return this.api.patch<void>(`${this.endpoint}/${id}/status?status=${status}`, {});
  }

  recordSync(id: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/sync`, {});
  }

  delete(id: number): Observable<BiometricDevice> {
    return this.api.delete<BiometricDevice>(`${this.endpoint}/${id}`);
  }
}
