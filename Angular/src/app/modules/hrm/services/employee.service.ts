import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Employee, CreateEmployeeRequest, UpdateEmployeeRequest, SelfUpdateEmployeeRequest, EmploymentStatus } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  // environment.apiUrl already ends with /api -> resolves to /api/employees
  private readonly endpoint = '/employees';

  constructor(private api: ApiService) {}

  list(
    page = 0,
    size = 20,
    departmentId?: number,
    status?: EmploymentStatus,
    excludeOwner = true,
    search?: string
  ): Observable<PagedResponse<Employee>> {
    const params: any = {};
    if (departmentId) params.departmentId = departmentId;
    if (status) params.status = status;
    if (excludeOwner) params.excludeOwner = true;
    if (search && search.trim()) params.search = search.trim();
    return this.api.getPaged<Employee>(this.endpoint, page, size, params);
  }

  getById(id: number): Observable<Employee> {
    return this.api.get<Employee>(`${this.endpoint}/${id}`);
  }

  getMyProfile(): Observable<Employee> {
    return this.api.get<Employee>(`${this.endpoint}/me`);
  }

  updateMyProfile(payload: SelfUpdateEmployeeRequest): Observable<Employee> {
    return this.api.patch<Employee>(`${this.endpoint}/me`, payload);
  }

  create(payload: CreateEmployeeRequest): Observable<Employee> {
    return this.api.post<Employee>(this.endpoint, payload);
  }

  update(id: number, payload: UpdateEmployeeRequest): Observable<Employee> {
    return this.api.patch<Employee>(`${this.endpoint}/${id}`, payload);
  }

  terminate(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }

  downloadPdf(departmentId?: number, status?: EmploymentStatus, search?: string, excludeOwner = true): Observable<Blob> {
    const params: string[] = [];
    if (departmentId) params.push(`departmentId=${departmentId}`);
    if (status) params.push(`status=${status}`);
    if (search && search.trim()) params.push(`search=${encodeURIComponent(search.trim())}`);
    params.push(`excludeOwner=${excludeOwner}`);
    return this.api.getBlob(`${this.endpoint}/pdf?${params.join('&')}`);
  }
}
