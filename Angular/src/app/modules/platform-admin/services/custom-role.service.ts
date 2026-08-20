import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { CustomRole, CustomRoleRequest } from '../models/platform-admin.model';

@Injectable({ providedIn: 'root' })
export class CustomRoleService {
  private readonly endpoint = '/custom-roles';

  constructor(private api: ApiService) {}

  list(): Observable<CustomRole[]> {
    return this.api.get<CustomRole[]>(this.endpoint);
  }

  getById(id: number): Observable<CustomRole> {
    return this.api.get<CustomRole>(`${this.endpoint}/${id}`);
  }

  create(payload: CustomRoleRequest): Observable<CustomRole> {
    return this.api.post<CustomRole>(this.endpoint, payload);
  }

  update(id: number, payload: CustomRoleRequest): Observable<CustomRole> {
    return this.api.put<CustomRole>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
