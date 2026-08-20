import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { SupportAuditLog } from '../models/support.model';

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly endpoint = '/support/audit-logs';
  constructor(private api: ApiService) {}

  getById(id: number): Observable<SupportAuditLog> { return this.api.get<SupportAuditLog>(`${this.endpoint}/${id}`); }
  list(page = 0, size = 20): Observable<PagedResponse<SupportAuditLog>> { return this.api.getPaged<SupportAuditLog>(this.endpoint, page, size); }
  byActionType(actionType: string, page = 0, size = 20): Observable<PagedResponse<SupportAuditLog>> {
    return this.api.getPaged<SupportAuditLog>(`${this.endpoint}/action/${actionType}`, page, size);
  }
  byResourceId(resourceId: number): Observable<SupportAuditLog[]> {
    return this.api.get<SupportAuditLog[]>(`${this.endpoint}/resource/${resourceId}`);
  }
  // start/end must be ISO date strings (yyyy-MM-dd) - matches @DateTimeFormat(ISO.DATE) on the backend
  byDateRange(start: string, end: string, page = 0, size = 20): Observable<PagedResponse<SupportAuditLog>> {
    return this.api.getPaged<SupportAuditLog>(`${this.endpoint}/date-range`, page, size, { start, end });
  }
  byUser(userId: number, page = 0, size = 20): Observable<PagedResponse<SupportAuditLog>> {
    return this.api.getPaged<SupportAuditLog>(`${this.endpoint}/user/${userId}`, page, size);
  }
}
