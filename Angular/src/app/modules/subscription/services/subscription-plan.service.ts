import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

export type BillingCycle = 'MONTHLY' | 'YEARLY';

// Mirrors backend SubscriptionPlanDefinition (GET /api/subscription-plans) - the
// Super Admin-managed plan catalog, not a fixed set of tiers.
export interface SubscriptionPlanOption {
  id: number;
  code: string;
  name: string;
  description?: string;
  billingCycle: BillingCycle;
  price: number;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionPlanService {
  constructor(private api: ApiService) {}

  // Owner-facing picker only needs plans that can actually be purchased.
  list(): Observable<SubscriptionPlanOption[]> {
    return this.api.get<SubscriptionPlanOption[]>('/subscription-plans', { activeOnly: true });
  }
}
