import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { StageApproval } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ApprovalService {
  private readonly endpoint = '/approvals';

  constructor(private api: ApiService) {}

  pending(page = 0, size = 20): Observable<PagedResponse<StageApproval>> {
    return this.api.getPaged<StageApproval>(`${this.endpoint}/pending`, page, size);
  }

  forRequest(serviceRequestId: number): Observable<StageApproval[]> {
    return this.api.get<StageApproval[]>(`${this.endpoint}/request/${serviceRequestId}`);
  }

  approve(id: number, decisionNotes?: string): Observable<StageApproval> {
    return this.api.post<StageApproval>(`${this.endpoint}/${id}/approve`, { decisionNotes });
  }

  reject(id: number, decisionNotes: string): Observable<StageApproval> {
    return this.api.post<StageApproval>(`${this.endpoint}/${id}/reject`, { decisionNotes });
  }
}
