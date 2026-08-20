import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Client } from '../models/crm.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly endpoint = '/clients';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20, status?: string, tagId?: number | null): Observable<PagedResponse<Client>> {
    const params: any = {};
    if (status) params.status = status;
    if (tagId) params.tagId = tagId;
    return this.api.getPaged<Client>(this.endpoint, page, size, Object.keys(params).length ? params : undefined);
  }

  // Lightweight, ungated - use this for pickers/dropdowns outside the Clients admin page.
  listActive(): Observable<Client[]> {
    return this.api.get<Client[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<Client> {
    return this.api.get<Client>(`${this.endpoint}/${id}`);
  }

  // Self-service (CLIENT role) - resolves the caller's own Client record server-side
  getMyProfile(): Observable<Client> {
    return this.api.get<Client>(`${this.endpoint}/me`);
  }

  updateMyProfile(payload: Partial<Client>): Observable<Client> {
    return this.api.patch<Client>(`${this.endpoint}/me`, payload);
  }

  // Backend CreateClientRequest also takes email + password to provision the portal user
  create(payload: any): Observable<Client> {
    return this.api.post<Client>(this.endpoint, payload);
  }

  update(id: number, payload: Partial<Client>): Observable<Client> {
    return this.api.patch<Client>(`${this.endpoint}/${id}`, payload);
  }

  /**
   * Creates a portal login for this client and emails a one-time set-password
   * link. No password is chosen here - the client sets their own, so nothing
   * has to be communicated out of band.
   *
   * Safe to call again for a client who already has a login: it just issues a
   * fresh link, which is what you want when the first one expired.
   */
  inviteToPortal(id: number): Observable<Client> {
    return this.api.post<Client>(`${this.endpoint}/${id}/invite-portal`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
