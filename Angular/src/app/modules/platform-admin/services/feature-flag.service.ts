import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { FeatureFlag } from '../models/platform-admin.model';

@Injectable({ providedIn: 'root' })
export class FeatureFlagService {
  private readonly endpoint = '/feature-flags';

  constructor(private api: ApiService) {}

  list(): Observable<FeatureFlag[]> {
    return this.api.get<FeatureFlag[]>(this.endpoint);
  }

  getByKey(key: string): Observable<FeatureFlag> {
    return this.api.get<FeatureFlag>(`${this.endpoint}/${key}`);
  }

  toggle(key: string): Observable<FeatureFlag> {
    return this.api.patch<FeatureFlag>(`${this.endpoint}/${key}/toggle`, {});
  }

  // Backend returns a plain-text summary message, not the flag list
  seed(): Observable<string> {
    return this.api.postText(`${this.endpoint}/seed`, {});
  }
}
