import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { EmployeeShiftAssignment, EmployeeShiftAssignmentRequest } from '../../hrm/models/hrm.model';

@Injectable({ providedIn: 'root' })
export class ShiftAssignmentService {
  private readonly endpoint = '/hrm/attendance/shift-assignments';
  constructor(private api: ApiService) {}

  create(payload: EmployeeShiftAssignmentRequest): Observable<EmployeeShiftAssignment> {
    return this.api.post<EmployeeShiftAssignment>(this.endpoint, payload);
  }

  getById(id: number): Observable<EmployeeShiftAssignment> {
    return this.api.get<EmployeeShiftAssignment>(`${this.endpoint}/${id}`);
  }

  list(page = 0, size = 20): Observable<PagedResponse<EmployeeShiftAssignment>> {
    return this.api.getPaged<EmployeeShiftAssignment>(this.endpoint, page, size);
  }

  getByEmployee(employeeId: number): Observable<EmployeeShiftAssignment> {
    return this.api.get<EmployeeShiftAssignment>(`${this.endpoint}/employee/${employeeId}`);
  }

  getByShift(shiftId: number, page = 0, size = 20): Observable<PagedResponse<EmployeeShiftAssignment>> {
    return this.api.getPaged<EmployeeShiftAssignment>(`${this.endpoint}/shift/${shiftId}`, page, size);
  }

  update(id: number, payload: EmployeeShiftAssignmentRequest): Observable<EmployeeShiftAssignment> {
    return this.api.put<EmployeeShiftAssignment>(`${this.endpoint}/${id}`, payload);
  }

  endAssignment(id: number): Observable<void> {
    return this.api.put<void>(`${this.endpoint}/${id}/end`, {});
  }

  delete(id: number): Observable<EmployeeShiftAssignment> {
    return this.api.delete<EmployeeShiftAssignment>(`${this.endpoint}/${id}`);
  }
}
