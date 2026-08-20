import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class RecruitmentSkillService {
  private readonly endpoint = '/recruitment/skills/suggestions';

  constructor(private api: ApiService) {}

  // Pooled from this company's own Candidate/TalentPool/JobPosting skill tags - see the backend controller.
  suggest(q: string): Observable<string[]> {
    return this.api.get<string[]>(this.endpoint, q ? { q } : {});
  }
}
