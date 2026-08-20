import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { SLAPolicy, SLAPolicyRequest } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class SLAPolicyService {
  private readonly endpoint = '/support/sla-policies';
  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<SLAPolicy>> { return this.api.getPaged<SLAPolicy>(this.endpoint, page, size); }
  active(): Observable<SLAPolicy[]> { return this.api.get<SLAPolicy[]>(`${this.endpoint}/active`); }
  getById(id: number): Observable<SLAPolicy> { return this.api.get<SLAPolicy>(`${this.endpoint}/${id}`); }
  getByPriority(priority: string): Observable<SLAPolicy> { return this.api.get<SLAPolicy>(`${this.endpoint}/priority/${priority}`); }
  create(payload: SLAPolicyRequest): Observable<SLAPolicy> { return this.api.post<SLAPolicy>(this.endpoint, payload); }
  update(id: number, payload: SLAPolicyRequest): Observable<SLAPolicy> { return this.api.patch<SLAPolicy>(`${this.endpoint}/${id}`, payload); }
  // Backend expects @RequestParam boolean active, and returns no body
  updateStatus(id: number, active: boolean): Observable<void> { return this.api.patch<void>(`${this.endpoint}/${id}/status?active=${active}`, {}); }
  delete(id: number): Observable<SLAPolicy> { return this.api.delete<SLAPolicy>(`${this.endpoint}/${id}`); }
}
