import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Timesheet, TimesheetRequest } from '../models/attendance.model';

@Injectable({ providedIn: 'root' })
export class TimesheetService {
  private readonly endpoint = '/hr/timesheets';
  constructor(private api: ApiService) {}

  log(payload: TimesheetRequest): Observable<Timesheet> {
    return this.api.post<Timesheet>(this.endpoint, payload);
  }

  listMine(page = 0, size = 31): Observable<PagedResponse<Timesheet>> {
    return this.api.getPaged<Timesheet>(`${this.endpoint}/my`, page, size);
  }

  listForEmployee(employeeId: number, page = 0, size = 31): Observable<PagedResponse<Timesheet>> {
    return this.api.getPaged<Timesheet>(`${this.endpoint}/employee/${employeeId}`, page, size);
  }

  listByRange(employeeId: number, from: string, to: string): Observable<Timesheet[]> {
    return this.api.get<Timesheet[]>(`${this.endpoint}/employee/${employeeId}/range`, { from, to });
  }

  getById(id: number): Observable<Timesheet> {
    return this.api.get<Timesheet>(`${this.endpoint}/${id}`);
  }

  update(id: number, payload: TimesheetRequest): Observable<Timesheet> {
    return this.api.patch<Timesheet>(`${this.endpoint}/${id}`, payload);
  }

  submitForReview(): Observable<{ submitted: number }> {
    return this.api.post<{ submitted: number }>(`${this.endpoint}/submit`, {});
  }

  composeEntry(payload: { projectName?: string; roughNotes: string }): Observable<{ taskDescription: string; description: string }> {
    return this.api.post<{ taskDescription: string; description: string }>(`${this.endpoint}/ai-compose`, payload);
  }

  approve(id: number): Observable<Timesheet> {
    return this.api.patch<Timesheet>(`${this.endpoint}/${id}/approve`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
