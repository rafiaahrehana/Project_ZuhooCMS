import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { CustomRole, CustomRoleRequest, Permission } from '../models/roles-permissions.model';

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

  getPermissions(roleId: number): Observable<string[]> {
    return this.api.get<string[]>(`${this.endpoint}/${roleId}/permissions`);
  }

  setPermissions(roleId: number, permissionCodes: string[]): Observable<string[]> {
    return this.api.put<string[]>(`${this.endpoint}/${roleId}/permissions`, permissionCodes);
  }

  assignEmployee(roleId: number, employeeId: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${roleId}/employees/${employeeId}`, {});
  }

  unassignEmployee(employeeId: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/employees/${employeeId}`);
  }

  listPermissionCatalog(): Observable<Permission[]> {
    return this.api.get<Permission[]>('/permissions');
  }
}
