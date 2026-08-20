import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { ClientContact } from '../models/crm.model';

@Injectable({ providedIn: 'root' })
export class ContactService {
  constructor(private api: ApiService) {}

  listByClient(clientId: number): Observable<ClientContact[]> {
    return this.api.get<ClientContact[]>(`/clients/${clientId}/contacts`);
  }

  // Cross-client global list, for the standalone Contacts page.
  listAll(page = 0, size = 20, keyword?: string): Observable<PagedResponse<ClientContact>> {
    return this.api.getPaged<ClientContact>('/crm/contacts', page, size, keyword ? { keyword } : undefined);
  }

  create(clientId: number, payload: Partial<ClientContact>): Observable<ClientContact> {
    return this.api.post<ClientContact>(`/clients/${clientId}/contacts`, payload);
  }

  update(clientId: number, id: number, payload: Partial<ClientContact>): Observable<ClientContact> {
    return this.api.patch<ClientContact>(`/clients/${clientId}/contacts/${id}`, payload);
  }

  markPrimary(clientId: number, id: number): Observable<ClientContact> {
    return this.api.patch<ClientContact>(`/clients/${clientId}/contacts/${id}/primary`, {});
  }

  delete(clientId: number, id: number): Observable<void> {
    return this.api.delete<void>(`/clients/${clientId}/contacts/${id}`);
  }
}
