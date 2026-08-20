import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { BiometricEnrollment, BiometricEnrollmentRequest } from '../models/attendance.model';

@Injectable({ providedIn: 'root' })
export class BiometricDataService {
  private readonly endpoint = '/company/biometric';
  constructor(private api: ApiService) {}

  enroll(payload: BiometricEnrollmentRequest): Observable<BiometricEnrollment> {
    return this.api.post<BiometricEnrollment>(`${this.endpoint}/enroll`, payload);
  }

  getByEmployee(employeeId: number): Observable<BiometricEnrollment[]> {
    return this.api.get<BiometricEnrollment[]>(`${this.endpoint}/employee/${employeeId}`);
  }

  getById(id: number): Observable<BiometricEnrollment> {
    return this.api.get<BiometricEnrollment>(`${this.endpoint}/${id}`);
  }

  verify(id: number, template: string, threshold = 95): Observable<boolean> {
    return this.api.post<boolean>(`${this.endpoint}/${id}/verify?template=${encodeURIComponent(template)}&threshold=${threshold}`, {});
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
