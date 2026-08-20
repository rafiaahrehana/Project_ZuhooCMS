import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { LeavePolicy, LeavePolicyRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class LeavePolicyService {
  private readonly endpoint = '/hr/leave-policies';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<LeavePolicy>> {
    return this.api.getPaged<LeavePolicy>(this.endpoint, page, size);
  }

  listActive(): Observable<LeavePolicy[]> {
    return this.api.get<LeavePolicy[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<LeavePolicy> {
    return this.api.get<LeavePolicy>(`${this.endpoint}/${id}`);
  }

  create(payload: LeavePolicyRequest): Observable<LeavePolicy> {
    return this.api.post<LeavePolicy>(this.endpoint, payload);
  }

  update(id: number, payload: LeavePolicyRequest): Observable<LeavePolicy> {
    return this.api.put<LeavePolicy>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }

  draftWithAi(remoteWorkAllowed: boolean, additionalContext: string): Observable<{ document: string }> {
    return this.api.post<{ document: string }>(`${this.endpoint}/draft`, { remoteWorkAllowed, additionalContext });
  }
}
