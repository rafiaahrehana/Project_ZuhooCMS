import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Holiday, HolidayRequest, HolidayDraftResponse } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class HolidayService {
  private readonly endpoint = '/hr/holidays';

  constructor(private api: ApiService) {}

  list(page = 0, size = 50): Observable<PagedResponse<Holiday>> {
    return this.api.getPaged<Holiday>(this.endpoint, page, size);
  }

  listByYear(year: number): Observable<Holiday[]> {
    return this.api.get<Holiday[]>(`${this.endpoint}/year/${year}`);
  }

  listCurrentYear(): Observable<Holiday[]> {
    return this.api.get<Holiday[]>(`${this.endpoint}/current-year`);
  }

  getById(id: number): Observable<Holiday> {
    return this.api.get<Holiday>(`${this.endpoint}/${id}`);
  }

  create(payload: HolidayRequest): Observable<Holiday> {
    return this.api.post<Holiday>(this.endpoint, payload);
  }

  update(id: number, payload: HolidayRequest): Observable<Holiday> {
    return this.api.put<Holiday>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }

  draftWithAi(instructions: string): Observable<HolidayDraftResponse> {
    return this.api.post<HolidayDraftResponse>(`${this.endpoint}/ai-draft`, { instructions });
  }
}
