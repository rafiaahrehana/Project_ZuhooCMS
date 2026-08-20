import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { SupportContextSwitch, SupportContextSwitchRequest } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class ContextSwitchService {
  private readonly endpoint = '/support/context-switches';
  constructor(private api: ApiService) {}

  switchContext(payload: SupportContextSwitchRequest): Observable<SupportContextSwitch> {
    return this.api.post<SupportContextSwitch>(`${this.endpoint}/switch`, payload);
  }
  endContextSwitch(id: number): Observable<void> { return this.api.post<void>(`${this.endpoint}/${id}/end`, {}); }
  activeForAgent(supportAgentId: number): Observable<SupportContextSwitch> {
    return this.api.get<SupportContextSwitch>(`${this.endpoint}/active/agent/${supportAgentId}`);
  }
  historyForAgent(supportAgentId: number, page = 0, size = 20): Observable<PagedResponse<SupportContextSwitch>> {
    return this.api.getPaged<SupportContextSwitch>(`${this.endpoint}/history/agent/${supportAgentId}`, page, size);
  }
  active(): Observable<SupportContextSwitch[]> { return this.api.get<SupportContextSwitch[]>(`${this.endpoint}/active`); }
  getById(id: number): Observable<SupportContextSwitch> { return this.api.get<SupportContextSwitch>(`${this.endpoint}/${id}`); }
}
