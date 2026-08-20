import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { ChangeStageRequest, DuplicateMatch, Opportunity, OpportunityStage, PipelineSummary } from '../models/crm.model';

@Injectable({ providedIn: 'root' })
export class OpportunityService {
  private readonly endpoint = '/crm/opportunities';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20, params?: any): Observable<PagedResponse<Opportunity>> {
    return this.api.getPaged<Opportunity>(this.endpoint, page, size, params);
  }

  getById(id: number): Observable<Opportunity> {
    return this.api.get<Opportunity>(`${this.endpoint}/${id}`);
  }

  create(payload: Partial<Opportunity>): Observable<Opportunity> {
    return this.api.post<Opportunity>(this.endpoint, payload);
  }

  createFromLead(leadId: number, payload: Partial<Opportunity>): Observable<Opportunity> {
    return this.api.post<Opportunity>(`${this.endpoint}/from-lead/${leadId}`, payload);
  }

  update(id: number, payload: Partial<Opportunity>): Observable<Opportunity> {
    return this.api.patch<Opportunity>(`${this.endpoint}/${id}`, payload);
  }

  changeStage(id: number, stage: OpportunityStage, options?: Partial<ChangeStageRequest>): Observable<Opportunity> {
    return this.api.patch<Opportunity>(`${this.endpoint}/${id}/stage`, { stage, ...options });
  }

  // Called before committing a WON stage change on a client-less opportunity, so the UI
  // can confirm link-existing vs create-new before the transition happens.
  previewWonDuplicate(id: number): Observable<DuplicateMatch | null> {
    return this.api.get<DuplicateMatch | null>(`${this.endpoint}/${id}/won-duplicate-check`);
  }

  pipelineSummary(): Observable<PipelineSummary> {
    return this.api.get<PipelineSummary>(`${this.endpoint}/pipeline-summary`);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
