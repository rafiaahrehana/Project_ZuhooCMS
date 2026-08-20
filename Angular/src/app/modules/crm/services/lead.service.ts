import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { CrmActivity, ConvertToOpportunityRequest, Opportunity, Lead } from '../models/crm.model';

@Injectable({ providedIn: 'root' })
export class LeadService {
  private readonly endpoint = '/crm/leads';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20, params?: any): Observable<PagedResponse<Lead>> {
    return this.api.getPaged<Lead>(this.endpoint, page, size, params);
  }

  getById(id: number): Observable<Lead> {
    return this.api.get<Lead>(`${this.endpoint}/${id}`);
  }

  summarise(id: number): Observable<Lead> {
    return this.api.get<Lead>(`${this.endpoint}/${id}/summary`);
  }

  create(payload: Partial<Lead>): Observable<Lead> {
    return this.api.post<Lead>(this.endpoint, payload);
  }

  // Combined criteria filtering (keyword, status, source, priority, flags, sorting)
  // via POST /crm/leads/filter - the plain GET list only supports a status param.
  filter(criteria: any, page = 0, size = 20): Observable<PagedResponse<Lead>> {
    return this.api.post<PagedResponse<Lead>>(
      `${this.endpoint}/filter?page=${page}&size=${size}`, criteria);
  }

  // Dedicated quick-view endpoints (server-side definitions of each view)
  my(page = 0, size = 20): Observable<PagedResponse<Lead>> {
    return this.api.getPaged<Lead>(`${this.endpoint}/my`, page, size);
  }

  unassigned(page = 0, size = 20): Observable<PagedResponse<Lead>> {
    return this.api.getPaged<Lead>(`${this.endpoint}/unassigned`, page, size);
  }

  highPriority(page = 0, size = 20): Observable<PagedResponse<Lead>> {
    return this.api.getPaged<Lead>(`${this.endpoint}/high-priority`, page, size);
  }

  neverContacted(page = 0, size = 20): Observable<PagedResponse<Lead>> {
    return this.api.getPaged<Lead>(`${this.endpoint}/never-contacted`, page, size);
  }

  stale(page = 0, size = 20): Observable<PagedResponse<Lead>> {
    return this.api.getPaged<Lead>(`${this.endpoint}/stale`, page, size);
  }

  countActive(): Observable<number> {
    return this.api.get<number>(`${this.endpoint}/stats/active`);
  }

  countMyActive(): Observable<number> {
    return this.api.get<number>(`${this.endpoint}/stats/my-active`);
  }

  // Converts a Qualified lead into an Opportunity - no Client is created at this point
  // (that happens when the Opportunity reaches Won). Returns the created Opportunity.
  convertToOpportunity(id: number, payload: ConvertToOpportunityRequest): Observable<Opportunity> {
    return this.api.patch<Opportunity>(`${this.endpoint}/${id}/convert-to-opportunity`, payload);
  }

  update(id: number, payload: Partial<Lead>): Observable<Lead> {
    return this.api.patch<Lead>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }

  // Lead-scoped activities. The generic /crm/activities endpoint requires a
  // clientId or opportunityId, so lead activities must use these endpoints,
  // which attach the lead server-side.
  listActivities(leadId: number, page = 0, size = 20): Observable<PagedResponse<CrmActivity>> {
    return this.api.getPaged<CrmActivity>(`${this.endpoint}/${leadId}/activities`, page, size);
  }

  addActivity(leadId: number, payload: Partial<CrmActivity>): Observable<CrmActivity> {
    return this.api.post<CrmActivity>(`${this.endpoint}/${leadId}/activities`, payload);
  }

  deleteActivity(leadId: number, activityId: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${leadId}/activities/${activityId}`);
  }

  downloadPdf(status?: string): Observable<Blob> {
    return this.api.getBlob(`${this.endpoint}/pdf${status ? '?status=' + status : ''}`);
  }

  /** Bulk CSV import; the response lists what was created and each skipped line with why. */
  importCsv(file: File): Observable<{ created: number; skipped: string[] }> {
    return this.api.postFile(`${this.endpoint}/import`, file);
  }
}
