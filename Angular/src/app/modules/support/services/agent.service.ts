import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { SupportAgent } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class AgentService {
  private readonly endpoint = '/v1/support/agents';
  constructor(private api: ApiService) {}
  list(page = 0): Observable<PagedResponse<SupportAgent>> { return this.api.getPaged<SupportAgent>(this.endpoint, page, 20); }
  // Backend returns a plain List<SupportAgentResponse> for /available, not a Page
  available(): Observable<SupportAgent[]> { return this.api.get<SupportAgent[]>(`${this.endpoint}/available`); }
  getById(id: number): Observable<SupportAgent> { return this.api.get<SupportAgent>(`${this.endpoint}/${id}`); }
  getByUserId(userId: number): Observable<SupportAgent> { return this.api.get<SupportAgent>(`${this.endpoint}/user/${userId}`); }
  getByStatus(status: string, page = 0): Observable<PagedResponse<SupportAgent>> { return this.api.getPaged<SupportAgent>(`${this.endpoint}/status/${status}`, page, 20); }
  create(payload: any): Observable<SupportAgent> { return this.api.post<SupportAgent>(this.endpoint, payload); }
  update(id: number, payload: any): Observable<SupportAgent> { return this.api.patch<SupportAgent>(`${this.endpoint}/${id}`, payload); }
  // Backend expects these as @RequestParam query params, and returns no body (ResponseEntity<Void>)
  updateStatus(id: number, status: string): Observable<void> { return this.api.patch<void>(`${this.endpoint}/${id}/status?status=${status}`, {}); }
  setAccepting(id: number, accepting: boolean): Observable<void> { return this.api.patch<void>(`${this.endpoint}/${id}/accepting-tickets?accepting=${accepting}`, {}); }
}
