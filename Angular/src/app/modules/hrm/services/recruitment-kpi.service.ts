import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { RecruitmentKpiSummary } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class RecruitmentKpiService {
  private readonly endpoint = '/recruitment/kpis';

  constructor(private api: ApiService) {}

  // from/to are 'yyyy-MM-dd' strings from a native date input - both optional, all-time when omitted.
  // minScore (0-100) only narrows the Top Evaluated Candidates list, not the rest of the report.
  getSummary(from?: string, to?: string, minScore?: number): Observable<RecruitmentKpiSummary> {
    const params: Record<string, string | number> = {};
    if (from) params['from'] = from;
    if (to) params['to'] = to;
    if (minScore != null) params['minScore'] = minScore;
    return this.api.get<RecruitmentKpiSummary>(this.endpoint, params);
  }
}
