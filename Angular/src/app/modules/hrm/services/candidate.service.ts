import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Candidate, CandidateRequest, JobApplication } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class CandidateService {
  private readonly endpoint = '/recruitment/candidates';

  constructor(private api: ApiService) {}

  // ALL CANDIDATES — q searches name/email/skills
  list(page = 0, size = 20, q?: string): Observable<PagedResponse<Candidate>> {
    const params: Record<string, string | number> = {};
    if (q) params['q'] = q;
    return this.api.getPaged<Candidate>(this.endpoint, page, size, params);
  }

  // SINGLE CANDIDATE
  getById(id: number): Observable<Candidate> {
    return this.api.get<Candidate>(`${this.endpoint}/${id}`);
  }

  // ALL APPLICATIONS FOR ONE CANDIDATE — one person, several jobs
  applications(id: number): Observable<JobApplication[]> {
    return this.api.get<JobApplication[]>(`${this.endpoint}/${id}/applications`);
  }

  update(id: number, payload: CandidateRequest): Observable<Candidate> {
    return this.api.put<Candidate>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
