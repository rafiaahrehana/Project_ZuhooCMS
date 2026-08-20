import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { PerformanceReview, PerformanceReviewRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class PerformanceReviewService {
  private readonly endpoint = '/hr/performance';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<PerformanceReview>> {
    return this.api.getPaged<PerformanceReview>(this.endpoint, page, size);
  }

  listForEmployee(employeeId: number, page = 0, size = 20): Observable<PagedResponse<PerformanceReview>> {
    return this.api.getPaged<PerformanceReview>(`${this.endpoint}/employee/${employeeId}`, page, size);
  }

  getById(id: number): Observable<PerformanceReview> {
    return this.api.get<PerformanceReview>(`${this.endpoint}/${id}`);
  }

  create(payload: PerformanceReviewRequest): Observable<PerformanceReview> {
    return this.api.post<PerformanceReview>(this.endpoint, payload);
  }

  update(id: number, payload: PerformanceReviewRequest): Observable<PerformanceReview> {
    return this.api.patch<PerformanceReview>(`${this.endpoint}/${id}`, payload);
  }

  finalise(id: number): Observable<PerformanceReview> {
    return this.api.patch<PerformanceReview>(`${this.endpoint}/${id}/finalise`, {});
  }

  summarise(id: number): Observable<PerformanceReview> {
    return this.api.get<PerformanceReview>(`${this.endpoint}/${id}/summary`);
  }

  /** Signs off the current approval stage and moves to the next. */
  advanceStage(id: number): Observable<PerformanceReview> {
    return this.api.post<PerformanceReview>(`${this.endpoint}/${id}/advance`, {});
  }

  /**
   * Objective KPIs recomputed live from attendance, leave, tasks, service
   * requests and client reviews. Not stored on the review.
   */
  kpis(employeeId: number, from: string, to: string): Observable<PerformanceKpis> {
    return this.api.get<PerformanceKpis>(
      `${this.endpoint}/employee/${employeeId}/kpis?from=${from}&to=${to}`);
  }

  listAttachments(reviewId: number): Observable<PerformanceAttachment[]> {
    return this.api.get<PerformanceAttachment[]>(`${this.endpoint}/${reviewId}/attachments`);
  }

  addAttachment(reviewId: number, payload: Partial<PerformanceAttachment>): Observable<PerformanceAttachment> {
    return this.api.post<PerformanceAttachment>(`${this.endpoint}/${reviewId}/attachments`, payload);
  }

  deleteAttachment(reviewId: number, attachmentId: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${reviewId}/attachments/${attachmentId}`);
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}

/** Mirrors PerformanceKpiResponse. Nullable fields mean "no data", not zero. */
export interface PerformanceKpis {
  employeeId: number;
  periodStart: string;
  periodEnd: string;
  daysPresent: number;
  daysAbsent: number;
  workingDaysRecorded: number;
  attendancePercent?: number | null;
  lateArrivals: number;
  leaveDaysTaken: number;
  tasksCompleted: number;
  projectsCompleted: number;
  customerSatisfaction?: number | null;
}

export interface PerformanceAttachment {
  id: number;
  reviewId: number;
  fileName: string;
  fileUrl: string;
  fileType?: string;
  fileSizeBytes?: number;
  label?: string;
  uploadedByName?: string;
  createdAt: string;
}
