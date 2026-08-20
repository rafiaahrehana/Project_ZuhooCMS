import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ServicePrerequisite, ServicePrerequisiteRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ServicePrerequisiteService {
  constructor(private api: ApiService) {}

  private endpoint(serviceId: number): string {
    return `/v1/services/${serviceId}/prerequisites`;
  }

  list(serviceId: number): Observable<ServicePrerequisite[]> {
    return this.api.get<ServicePrerequisite[]>(this.endpoint(serviceId));
  }

  create(serviceId: number, payload: ServicePrerequisiteRequest): Observable<ServicePrerequisite> {
    return this.api.post<ServicePrerequisite>(this.endpoint(serviceId), payload);
  }

  delete(serviceId: number, id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint(serviceId)}/${id}`);
  }
}
