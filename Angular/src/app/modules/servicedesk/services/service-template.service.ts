import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { ServiceTemplate, ServiceTemplateRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ServiceTemplateService {
  private readonly endpoint = '/v1/service-templates';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<ServiceTemplate>> {
    return this.api.getPaged<ServiceTemplate>(this.endpoint, page, size);
  }

  // Backend returns a bare List here (not a Page)
  listByCategory(categoryId: number): Observable<ServiceTemplate[]> {
    return this.api.get<ServiceTemplate[]>(`${this.endpoint}/category/${categoryId}`);
  }

  getById(id: number): Observable<ServiceTemplate> {
    return this.api.get<ServiceTemplate>(`${this.endpoint}/${id}`);
  }

  create(payload: ServiceTemplateRequest): Observable<ServiceTemplate> {
    return this.api.post<ServiceTemplate>(this.endpoint, payload);
  }

  update(id: number, payload: ServiceTemplateRequest): Observable<ServiceTemplate> {
    return this.api.put<ServiceTemplate>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
