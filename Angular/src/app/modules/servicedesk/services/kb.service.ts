import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { KbArticle } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class KbService {
  private readonly endpoint = '/kb/articles';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20, params?: any): Observable<PagedResponse<KbArticle>> {
    return this.api.getPaged<KbArticle>(this.endpoint, page, size, params);
  }

  getById(id: number): Observable<KbArticle> {
    return this.api.get<KbArticle>(`${this.endpoint}/${id}`);
  }

  create(payload: Partial<KbArticle>): Observable<KbArticle> {
    return this.api.post<KbArticle>(this.endpoint, payload);
  }

  update(id: number, payload: Partial<KbArticle>): Observable<KbArticle> {
    return this.api.patch<KbArticle>(`${this.endpoint}/${id}`, payload);
  }

  publish(id: number): Observable<KbArticle> {
    return this.api.patch<KbArticle>(`${this.endpoint}/${id}/publish`, {});
  }

  archive(id: number): Observable<KbArticle> {
    return this.api.patch<KbArticle>(`${this.endpoint}/${id}/archive`, {});
  }

  markHelpful(id: number): Observable<KbArticle> {
    return this.api.post<KbArticle>(`${this.endpoint}/${id}/helpful`, {});
  }
}
