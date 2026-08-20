import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { CompanyService, CompanyServiceRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class CompanyServiceService {
  private readonly endpoint = '/services';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<CompanyService>> {
    return this.api.getPaged<CompanyService>(this.endpoint, page, size);
  }

  // Backend returns a bare List (not a Page) of active services
  listActive(): Observable<CompanyService[]> {
    return this.api.get<CompanyService[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<CompanyService> {
    return this.api.get<CompanyService>(`${this.endpoint}/${id}`);
  }

  create(payload: CompanyServiceRequest): Observable<CompanyService> {
    return this.api.post<CompanyService>(this.endpoint, payload);
  }

  update(id: number, payload: CompanyServiceRequest): Observable<CompanyService> {
    return this.api.put<CompanyService>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<CompanyService> {
    return this.api.patch<CompanyService>(`${this.endpoint}/${id}/toggle`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
