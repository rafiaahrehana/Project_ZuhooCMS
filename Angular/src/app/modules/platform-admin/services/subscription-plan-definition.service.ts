import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SubscriptionPlanDefinition, SubscriptionPlanRequest } from '../models/platform-admin.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionPlanDefinitionService {
  private readonly endpoint = '/subscription-plans';

  constructor(private api: ApiService) {}

  // Admin management view needs every plan, including disabled ones.
  list(activeOnly = false): Observable<SubscriptionPlanDefinition[]> {
    return this.api.get<SubscriptionPlanDefinition[]>(this.endpoint, { activeOnly });
  }

  create(payload: SubscriptionPlanRequest): Observable<SubscriptionPlanDefinition> {
    return this.api.post<SubscriptionPlanDefinition>(this.endpoint, payload);
  }

  update(id: number, payload: SubscriptionPlanRequest): Observable<SubscriptionPlanDefinition> {
    return this.api.patch<SubscriptionPlanDefinition>(`${this.endpoint}/${id}`, payload);
  }

  toggleActive(id: number): Observable<SubscriptionPlanDefinition> {
    return this.api.patch<SubscriptionPlanDefinition>(`${this.endpoint}/${id}/toggle-active`, {});
  }
}
