import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ServiceCategory, ServiceCategoryRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ServiceCategoryService {
  private readonly endpoint = '/service-categories';

  constructor(private api: ApiService) {}

  // Backend returns a bare List (not a Page) of ACTIVE categories, ordered by sortOrder
  list(): Observable<ServiceCategory[]> {
    return this.api.get<ServiceCategory[]>(this.endpoint);
  }

  // Management listing including inactive categories (company owner/employee)
  listAll(): Observable<ServiceCategory[]> {
    return this.api.get<ServiceCategory[]>(`${this.endpoint}/all`);
  }

  // Active categories for dropdowns - same endpoint as list(), kept as a named
  // alias so lookup call-sites read clearly
  lookup(): Observable<ServiceCategory[]> {
    return this.list();
  }

  getById(id: number): Observable<ServiceCategory> {
    return this.api.get<ServiceCategory>(`${this.endpoint}/${id}`);
  }

  create(payload: ServiceCategoryRequest): Observable<ServiceCategory> {
    return this.api.post<ServiceCategory>(this.endpoint, payload);
  }

  update(id: number, payload: ServiceCategoryRequest): Observable<ServiceCategory> {
    return this.api.put<ServiceCategory>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<ServiceCategory> {
    return this.api.patch<ServiceCategory>(`${this.endpoint}/${id}/toggle`, {});
  }
}
