import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { JournalEntry, JournalEntryRequest } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class JournalEntryService {
  private readonly endpoint = '/company/finance/journal-entries';
  constructor(private api: ApiService) {}
  list(page = 0, size = 20): Observable<PagedResponse<JournalEntry>> {
    return this.api.getPaged<JournalEntry>(this.endpoint, page, size);
  }
  getById(id: number): Observable<JournalEntry> {
    return this.api.get<JournalEntry>(`${this.endpoint}/${id}`);
  }
  create(payload: JournalEntryRequest): Observable<JournalEntry> {
    return this.api.post<JournalEntry>(this.endpoint, payload);
  }
  approve(id: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/approve`, {});
  }
  post(id: number): Observable<void> {
    return this.api.post<void>(`${this.endpoint}/${id}/post`, {});
  }
  reverse(id: number): Observable<JournalEntry> {
    return this.api.post<JournalEntry>(`${this.endpoint}/${id}/reverse`, {});
  }
  delete(id: number): Observable<JournalEntry> {
    return this.api.delete<JournalEntry>(`${this.endpoint}/${id}`);
  }
}
