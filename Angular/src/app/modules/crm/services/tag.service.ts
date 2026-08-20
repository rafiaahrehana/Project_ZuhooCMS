import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { Tag } from '../models/crm.model';

@Injectable({ providedIn: 'root' })
export class TagService {
  private readonly endpoint = '/crm/tags';

  constructor(private api: ApiService) {}

  list(): Observable<Tag[]> {
    return this.api.get<Tag[]>(this.endpoint);
  }

  create(payload: Partial<Tag>): Observable<Tag> {
    return this.api.post<Tag>(this.endpoint, payload);
  }

  update(id: number, payload: Partial<Tag>): Observable<Tag> {
    return this.api.patch<Tag>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
