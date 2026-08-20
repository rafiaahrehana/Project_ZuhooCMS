import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { ApplicationStatus, Employee, EvaluateCandidateRequest, HireApplicationRequest, JobApplication, JobApplicationRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class RecruitmentService {
  private readonly endpoint = '/recruitment';

  /** All offers made against an application - the ACCEPTED one pre-fills the hire wizard. */
  offersForApplication(applicationId: number) {
    return this.api.get<any[]>(`${this.endpoint}/offers/application/${applicationId}`);
  }

  constructor(private api: ApiService) {}

  // APPLY TO A JOB POSTING
  apply(jobPostingId: number, payload: JobApplicationRequest): Observable<JobApplication> {
    return this.api.post<JobApplication>(`${this.endpoint}/jobs/${jobPostingId}/apply`, payload);
  }

  // ALL APPLICATIONS
  list(page = 0, size = 20, status?: ApplicationStatus): Observable<PagedResponse<JobApplication>> {
    const params: Record<string, string | number> = {};
    if (status) params['status'] = status;
    return this.api.getPaged<JobApplication>(`${this.endpoint}/applications`, page, size, params);
  }

  // APPLICATIONS FOR A JOB POSTING
  listForJob(jobPostingId: number, page = 0, size = 20): Observable<PagedResponse<JobApplication>> {
    return this.api.getPaged<JobApplication>(`${this.endpoint}/jobs/${jobPostingId}/applications`, page, size);
  }

  // SINGLE APPLICATION
  getById(id: number): Observable<JobApplication> {
    return this.api.get<JobApplication>(`${this.endpoint}/applications/${id}`);
  }

  // UPDATE APPLICATION STATUS
  updateStatus(id: number, status: ApplicationStatus, notes?: string): Observable<JobApplication> {
    return this.api.patch<JobApplication>(`${this.endpoint}/applications/${id}/status`, { status, notes });
  }

  // EVALUATE CANDIDATE — sets whichever scores are provided, backend recomputes overallScore
  evaluate(id: number, payload: EvaluateCandidateRequest): Observable<JobApplication> {
    return this.api.patch<JobApplication>(`${this.endpoint}/applications/${id}/evaluate`, payload);
  }

  // HIRE AN OFFERED CANDIDATE — creates the Employee (+ portal user) and marks the application HIRED
  hire(id: number, payload: HireApplicationRequest): Observable<Employee> {
    return this.api.post<Employee>(`${this.endpoint}/applications/${id}/hire`, payload);
  }

  // DELETE APPLICATION
  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/applications/${id}`);
  }
}
