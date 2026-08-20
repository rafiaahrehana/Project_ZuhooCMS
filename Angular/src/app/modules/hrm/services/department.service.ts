import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Department, DepartmentRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private readonly endpoint = '/departments';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<Department>> {
    return this.api.getPaged<Department>(this.endpoint, page, size);
  }

  listActive(): Observable<Department[]> {
    return this.api.get<Department[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<Department> {
    return this.api.get<Department>(`${this.endpoint}/${id}`);
  }

  create(payload: DepartmentRequest): Observable<Department> {
    return this.api.post<Department>(this.endpoint, payload);
  }

  update(id: number, payload: DepartmentRequest): Observable<Department> {
    return this.api.put<Department>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<Department> {
    return this.api.patch<Department>(`${this.endpoint}/${id}/toggle`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
