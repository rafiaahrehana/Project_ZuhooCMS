import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { OffboardingChecklist, OffboardingChecklistRequest } from '../models/itam.model';

@Injectable({ providedIn: 'root' })
export class OffboardingService {
  private readonly endpoint = '/v1/company/offboarding/checklist';
  constructor(private api: ApiService) {}

  create(payload: OffboardingChecklistRequest): Observable<OffboardingChecklist> {
    return this.api.post<OffboardingChecklist>(this.endpoint, payload);
  }

  getById(id: number): Observable<OffboardingChecklist> {
    return this.api.get<OffboardingChecklist>(`${this.endpoint}/${id}`);
  }

  getByEmployee(employeeId: number): Observable<OffboardingChecklist> {
    return this.api.get<OffboardingChecklist>(`${this.endpoint}/employee/${employeeId}`);
  }

  list(page = 0, size = 20): Observable<PagedResponse<OffboardingChecklist>> {
    return this.api.getPaged<OffboardingChecklist>(this.endpoint, page, size);
  }

  getPending(): Observable<OffboardingChecklist[]> {
    return this.api.get<OffboardingChecklist[]>(`${this.endpoint}/pending`);
  }

  markHardwareCollected(id: number): Observable<void> {
    return this.api.patch<void>(`${this.endpoint}/${id}/hardware-collected`, {});
  }

  markLicensesRevoked(id: number): Observable<void> {
    return this.api.patch<void>(`${this.endpoint}/${id}/licenses-revoked`, {});
  }

  markAccessRevoked(id: number): Observable<void> {
    return this.api.patch<void>(`${this.endpoint}/${id}/access-revoked`, {});
  }

  markDataHandedOver(id: number): Observable<void> {
    return this.api.patch<void>(`${this.endpoint}/${id}/data-handed-over`, {});
  }

  markExitInterviewCompleted(id: number): Observable<void> {
    return this.api.patch<void>(`${this.endpoint}/${id}/exit-interview`, {});
  }

  delete(id: number): Observable<OffboardingChecklist> {
    return this.api.delete<OffboardingChecklist>(`${this.endpoint}/${id}`);
  }
}
