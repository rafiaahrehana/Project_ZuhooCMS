import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { SupportMessage, SupportMessageRequest } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly endpoint = '/v1/support/messages';
  constructor(private api: ApiService) {}

  create(payload: SupportMessageRequest): Observable<SupportMessage> {
    return this.api.post<SupportMessage>(this.endpoint, payload);
  }
  getById(id: number): Observable<SupportMessage> { return this.api.get<SupportMessage>(`${this.endpoint}/${id}`); }
  getByTicket(ticketId: number, page = 0, size = 20): Observable<PagedResponse<SupportMessage>> {
    return this.api.getPaged<SupportMessage>(`${this.endpoint}/ticket/${ticketId}`, page, size);
  }
  getExternalMessages(ticketId: number): Observable<SupportMessage[]> {
    return this.api.get<SupportMessage[]>(`${this.endpoint}/ticket/${ticketId}/external`);
  }
  getInternalNotes(ticketId: number): Observable<SupportMessage[]> {
    return this.api.get<SupportMessage[]>(`${this.endpoint}/ticket/${ticketId}/internal`);
  }
  update(id: number, payload: SupportMessageRequest): Observable<SupportMessage> {
    return this.api.patch<SupportMessage>(`${this.endpoint}/${id}`, payload);
  }
  delete(id: number): Observable<SupportMessage> { return this.api.delete<SupportMessage>(`${this.endpoint}/${id}`); }

  // CLIENT-facing: always external (isInternal is forced false server-side).
  createForClient(payload: SupportMessageRequest): Observable<SupportMessage> {
    return this.api.post<SupportMessage>(`${this.endpoint}/client`, payload);
  }
  getClientMessages(ticketId: number): Observable<SupportMessage[]> {
    return this.api.get<SupportMessage[]>(`${this.endpoint}/client/ticket/${ticketId}`);
  }
}
