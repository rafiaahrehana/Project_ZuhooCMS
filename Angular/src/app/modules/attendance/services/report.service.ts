import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly endpoint = '/company/attendance/reports';
  constructor(private api: ApiService) {}

  daily(date?: string): Observable<any> { 
    return this.api.get(`${this.endpoint}/daily`, date ? { date } : undefined); }

  monthly(month?: string, year?: number): Observable<any> {
     return this.api.get(`${this.endpoint}/monthly`, { month, year }); }

  lateAbsent(date?: string): Observable<any> { 
    return this.api.get(`${this.endpoint}/late-absent`, date ? { date } : undefined); }
}
