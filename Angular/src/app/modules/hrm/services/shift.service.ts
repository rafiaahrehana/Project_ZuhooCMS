import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Shift, ShiftRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class ShiftService {
  private readonly endpoint = '/hr/shifts';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<Shift>> {
    return this.api.getPaged<Shift>(this.endpoint, page, size);
  }

  listActive(): Observable<Shift[]> {
    return this.api.get<Shift[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<Shift> {
    return this.api.get<Shift>(`${this.endpoint}/${id}`);
  }

  create(payload: ShiftRequest): Observable<Shift> {
    return this.api.post<Shift>(this.endpoint, payload);
  }

  update(id: number, payload: ShiftRequest): Observable<Shift> {
    return this.api.put<Shift>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<Shift> {
    return this.api.patch<Shift>(`${this.endpoint}/${id}/toggle`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
