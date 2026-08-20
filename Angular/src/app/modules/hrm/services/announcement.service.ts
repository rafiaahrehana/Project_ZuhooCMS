import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Announcement, AnnouncementRequest, AnnouncementDraftResponse } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class AnnouncementService {
  private readonly endpoint = '/announcements';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<Announcement>> {
    return this.api.getPaged<Announcement>(this.endpoint, page, size);
  }

  listActive(): Observable<Announcement[]> {
    return this.api.get<Announcement[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<Announcement> {
    return this.api.get<Announcement>(`${this.endpoint}/${id}`);
  }

  create(payload: AnnouncementRequest): Observable<Announcement> {
    return this.api.post<Announcement>(this.endpoint, payload);
  }

  draftWithAi(instructions: string): Observable<AnnouncementDraftResponse> {
    return this.api.post<AnnouncementDraftResponse>(`${this.endpoint}/ai-draft`, { instructions });
  }

  update(id: number, payload: AnnouncementRequest): Observable<Announcement> {
    return this.api.put<Announcement>(`${this.endpoint}/${id}`, payload);
  }

  publish(id: number): Observable<Announcement> {
    return this.api.patch<Announcement>(`${this.endpoint}/${id}/publish`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
