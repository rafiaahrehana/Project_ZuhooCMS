import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { HrAsset, HrAssetRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class HrAssetService {
  private readonly endpoint = '/hr/assets';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<HrAsset>> {
    return this.api.getPaged<HrAsset>(this.endpoint, page, size);
  }

  // Backend returns a plain List<AssetResponse> here, not a paged response.
  listForEmployee(employeeId: number): Observable<HrAsset[]> {
    return this.api.get<HrAsset[]>(`${this.endpoint}/employee/${employeeId}`);
  }

  getById(id: number): Observable<HrAsset> {
    return this.api.get<HrAsset>(`${this.endpoint}/${id}`);
  }

  create(payload: HrAssetRequest): Observable<HrAsset> {
    return this.api.post<HrAsset>(this.endpoint, payload);
  }

  update(id: number, payload: HrAssetRequest): Observable<HrAsset> {
    return this.api.put<HrAsset>(`${this.endpoint}/${id}`, payload);
  }

  assign(id: number, employeeId: number): Observable<HrAsset> {
    return this.api.patch<HrAsset>(`${this.endpoint}/${id}/assign/${employeeId}`, {});
  }

  unassign(id: number): Observable<HrAsset> {
    return this.api.patch<HrAsset>(`${this.endpoint}/${id}/unassign`, {});
  }

  setMaintenance(id: number, underMaintenance: boolean): Observable<HrAsset> {
    return this.api.patch<HrAsset>(`${this.endpoint}/${id}/maintenance?underMaintenance=${underMaintenance}`, {});
  }

  dispose(id: number, reason?: string): Observable<HrAsset> {
    const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return this.api.patch<HrAsset>(`${this.endpoint}/${id}/dispose${query}`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
