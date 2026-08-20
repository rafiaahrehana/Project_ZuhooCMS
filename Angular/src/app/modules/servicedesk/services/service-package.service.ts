import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import {
  PackageSubscription,
  ServicePackage,
  ServicePackageRequest,
  SubscribeRequest,
} from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ServicePackageService {
  private readonly endpoint = '/packages';

  constructor(private api: ApiService) {}

  // PACKAGES
  list(page = 0, size = 20): Observable<PagedResponse<ServicePackage>> {
    return this.api.getPaged<ServicePackage>(this.endpoint, page, size);
  }

  // Backend returns a bare List (not a Page) of active packages
  listActive(): Observable<ServicePackage[]> {
    return this.api.get<ServicePackage[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<ServicePackage> {
    return this.api.get<ServicePackage>(`${this.endpoint}/${id}`);
  }

  create(payload: ServicePackageRequest): Observable<ServicePackage> {
    return this.api.post<ServicePackage>(this.endpoint, payload);
  }

  update(id: number, payload: ServicePackageRequest): Observable<ServicePackage> {
    return this.api.put<ServicePackage>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<ServicePackage> {
    return this.api.patch<ServicePackage>(`${this.endpoint}/${id}/toggle`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }

  // SUBSCRIPTIONS
  subscribe(payload: SubscribeRequest): Observable<PackageSubscription> {
    return this.api.post<PackageSubscription>(`${this.endpoint}/subscribe`, payload);
  }

  listSubscriptions(page = 0, size = 20): Observable<PagedResponse<PackageSubscription>> {
    return this.api.getPaged<PackageSubscription>(`${this.endpoint}/subscriptions`, page, size);
  }

  mySubscriptions(page = 0, size = 20): Observable<PagedResponse<PackageSubscription>> {
    return this.api.getPaged<PackageSubscription>(`${this.endpoint}/subscriptions/my`, page, size);
  }

  getSubscriptionById(id: number): Observable<PackageSubscription> {
    return this.api.get<PackageSubscription>(`${this.endpoint}/subscriptions/${id}`);
  }

  activateSubscription(id: number): Observable<PackageSubscription> {
    return this.api.patch<PackageSubscription>(`${this.endpoint}/subscriptions/${id}/activate`, {});
  }

  // Backend takes the reason as an optional ?reason= query param on both of these -
  // a JSON body is silently ignored by @RequestParam binding.
  suspendSubscription(id: number, reason?: string): Observable<PackageSubscription> {
    const q = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return this.api.patch<PackageSubscription>(`${this.endpoint}/subscriptions/${id}/suspend${q}`, {});
  }

  reactivateSubscription(id: number): Observable<PackageSubscription> {
    return this.api.patch<PackageSubscription>(`${this.endpoint}/subscriptions/${id}/reactivate`, {});
  }

  cancelSubscription(id: number, reason?: string): Observable<PackageSubscription> {
    const q = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return this.api.patch<PackageSubscription>(`${this.endpoint}/subscriptions/${id}/cancel${q}`, {});
  }
}
