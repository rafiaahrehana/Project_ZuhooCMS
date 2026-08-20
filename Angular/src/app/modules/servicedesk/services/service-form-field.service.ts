import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ServiceFormField, ServiceFormFieldRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ServiceFormFieldService {
  constructor(private api: ApiService) {}

  private endpoint(serviceId: number): string {
    return `/v1/services/${serviceId}/form-fields`;
  }

  list(serviceId: number): Observable<ServiceFormField[]> {
    return this.api.get<ServiceFormField[]>(this.endpoint(serviceId));
  }

  create(serviceId: number, payload: ServiceFormFieldRequest): Observable<ServiceFormField> {
    return this.api.post<ServiceFormField>(this.endpoint(serviceId), payload);
  }

  update(serviceId: number, id: number, payload: ServiceFormFieldRequest): Observable<ServiceFormField> {
    return this.api.put<ServiceFormField>(`${this.endpoint(serviceId)}/${id}`, payload);
  }

  delete(serviceId: number, id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint(serviceId)}/${id}`);
  }
}
