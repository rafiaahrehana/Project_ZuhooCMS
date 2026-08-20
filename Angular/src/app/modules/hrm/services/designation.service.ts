import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Designation, DesignationRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class DesignationService {
  private readonly endpoint = '/hr/designations';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<Designation>> {
    return this.api.getPaged<Designation>(this.endpoint, page, size);
  }

  listActive(): Observable<Designation[]> {
    return this.api.get<Designation[]>(`${this.endpoint}/active`);
  }

  create(payload: DesignationRequest): Observable<Designation> {
    return this.api.post<Designation>(this.endpoint, payload);
  }

  update(id: number, payload: DesignationRequest): Observable<Designation> {
    return this.api.put<Designation>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<Designation> {
    return this.api.patch<Designation>(`${this.endpoint}/${id}/toggle`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
