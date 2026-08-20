import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { Proposal, ProposalAttachment, ProposalAttachmentRequest, ProposalRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ProposalService {
  constructor(private api: ApiService) {}

  private endpoint(requestId: number): string {
    return `/service-requests/${requestId}/proposal`;
  }

  get(requestId: number): Observable<Proposal | null> {
    return this.api.get<Proposal | null>(this.endpoint(requestId));
  }

  save(requestId: number, payload: ProposalRequest): Observable<Proposal> {
    return this.api.put<Proposal>(this.endpoint(requestId), payload);
  }

  send(requestId: number): Observable<Proposal> {
    return this.api.post<Proposal>(`${this.endpoint(requestId)}/send`, {});
  }

  accept(requestId: number): Observable<Proposal> {
    return this.api.post<Proposal>(`${this.endpoint(requestId)}/accept`, {});
  }

  requestChanges(requestId: number, feedback: string): Observable<Proposal> {
    return this.api.post<Proposal>(`${this.endpoint(requestId)}/request-changes`, { feedback });
  }

  addAttachment(requestId: number, payload: ProposalAttachmentRequest): Observable<ProposalAttachment> {
    return this.api.post<ProposalAttachment>(`${this.endpoint(requestId)}/attachments`, payload);
  }

  deleteAttachment(requestId: number, attachmentId: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint(requestId)}/attachments/${attachmentId}`);
  }
}
