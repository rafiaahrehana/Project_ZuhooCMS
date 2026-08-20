import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { AttendanceRecord, ManualAttendanceRequest, MyAttendanceMonthlySummary } from '../models/attendance.model';

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly endpoint = '/company/attendance';
  constructor(private api: ApiService) {}

  list(
    page = 0,
    size = 20,
    status?: string,
    date?: string,
    startDate?: string,
    endDate?: string,
    search?: string
  ): Observable<PagedResponse<AttendanceRecord>> {
    const params: any = {};
    if (status) params.status = status;
    if (date) params.date = date;
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    if (search && search.trim()) params.search = search.trim();
    return this.api.getPaged(this.endpoint, page, size, params);
  }

  getByEmployee(employeeId: number, page = 0): Observable<PagedResponse<AttendanceRecord>> {
    return this.api.getPaged(`${this.endpoint}/employee/${employeeId}`, page, 20); }

  listByStatus(status: string, page = 0): Observable<PagedResponse<AttendanceRecord>> {
    return this.api.getPaged(`${this.endpoint}/status/${status}`, page, 20); }

  myToday(): Observable<AttendanceRecord> {
    return this.api.get(`${this.endpoint}/my/today`);
  }

  myRecords(page = 0, size = 20): Observable<PagedResponse<AttendanceRecord>> {
    return this.api.getPaged(`${this.endpoint}/my`, page, size);
  }

  myMonthlySummary(year?: number, month?: number): Observable<MyAttendanceMonthlySummary> {
    const params: any = {};
    if (year) params.year = year;
    if (month) params.month = month;
    return this.api.get(`${this.endpoint}/my/monthly-summary`, params);
  }

  checkIn(data: any): Observable<AttendanceRecord> {
    return this.api.post(`${this.endpoint}/check-in`, data); }

  checkOut(id: number, data?: any): Observable<AttendanceRecord> {
     return this.api.post(`${this.endpoint}/${id}/check-out`, data || {}); }

  approve(id: number): Observable<AttendanceRecord> {
    return this.api.patch(`${this.endpoint}/${id}/approve`, {}); }

  // HR/Admin: manually record or backdate an employee's attendance
  createManual(data: ManualAttendanceRequest): Observable<AttendanceRecord> {
    return this.api.post(`${this.endpoint}/manual`, data); }

  // Owner: fill in ABSENT records for the company across a date range (e.g. days
  // the nightly scheduler was offline). Idempotent and scoped to the caller's company.
  backfillAbsentees(startDate: string, endDate: string): Observable<BackfillResult> {
    const q = `?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`;
    return this.api.post(`${this.endpoint}/backfill-absentees${q}`, {});
  }
}

export interface BackfillResult {
  created: number;
  startDate: string;
  endDate: string;
}
