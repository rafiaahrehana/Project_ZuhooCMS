import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { JobPosting, JobPostingRequest, JobPostingStatus } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class JobPostingService {
  private readonly endpoint = '/recruitment/jobs';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20, status?: JobPostingStatus): Observable<PagedResponse<JobPosting>> {
    const params: Record<string, string | number> = {};
    if (status) params['status'] = status;
    return this.api.getPaged<JobPosting>(this.endpoint, page, size, params);
  }

  // Lightweight, ungated - use this for pickers/dropdowns outside the Job Postings admin page.
  listOpen(): Observable<JobPosting[]> {
    return this.api.get<JobPosting[]>(`${this.endpoint}/open`);
  }

  getById(id: number): Observable<JobPosting> {
    return this.api.get<JobPosting>(`${this.endpoint}/${id}`);
  }

  create(payload: JobPostingRequest): Observable<JobPosting> {
    return this.api.post<JobPosting>(this.endpoint, payload);
  }

  update(id: number, payload: JobPostingRequest): Observable<JobPosting> {
    return this.api.put<JobPosting>(`${this.endpoint}/${id}`, payload);
  }

  publish(id: number): Observable<JobPosting> {
    return this.api.patch<JobPosting>(`${this.endpoint}/${id}/publish`, {});
  }

  close(id: number): Observable<JobPosting> {
    return this.api.patch<JobPosting>(`${this.endpoint}/${id}/close`, {});
  }

  // Dedicated action rather than update() - reassigning shouldn't require resending the whole posting.
  assignRecruiter(id: number, recruiterId: number | null): Observable<JobPosting> {
    return this.api.patch<JobPosting>(`${this.endpoint}/${id}/assign-recruiter`, { recruiterId });
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
