import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { CrmActivity } from '../models/crm.model';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly endpoint = '/crm/activities';

  constructor(private api: ApiService) {}

  timeline(params: { clientId?: number; opportunityId?: number }, page = 0, size = 20): Observable<PagedResponse<CrmActivity>> {
    return this.api.getPaged<CrmActivity>(this.endpoint, page, size, params);
  }

  log(payload: Partial<CrmActivity>): Observable<CrmActivity> {
    return this.api.post<CrmActivity>(this.endpoint, payload);
  }

  markCompleted(id: number): Observable<CrmActivity> {
    return this.api.patch<CrmActivity>(`${this.endpoint}/${id}/complete`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }

  summarise(params: { clientId?: number; opportunityId?: number }): Observable<{ summary: string }> {
    return this.api.get<{ summary: string }>(`${this.endpoint}/summary`, params);
  }
}
