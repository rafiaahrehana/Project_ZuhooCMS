import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { PlatformUser, PlatformUserRequest } from '../models/platform-admin.model';

@Injectable({ providedIn: 'root' })
export class PlatformUserService {
  private readonly endpoint = '/platform/users';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<PlatformUser>> {
    return this.api.getPaged<PlatformUser>(this.endpoint, page, size);
  }

  getById(id: number): Observable<PlatformUser> {
    return this.api.get<PlatformUser>(`${this.endpoint}/${id}`);
  }

  create(payload: PlatformUserRequest): Observable<PlatformUser> {
    return this.api.post<PlatformUser>(this.endpoint, payload);
  }

  // Password is optional here - backend only updates it if a non-blank value is sent
  update(id: number, payload: PlatformUserRequest): Observable<PlatformUser> {
    return this.api.patch<PlatformUser>(`${this.endpoint}/${id}`, payload);
  }

  deactivate(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }
}
