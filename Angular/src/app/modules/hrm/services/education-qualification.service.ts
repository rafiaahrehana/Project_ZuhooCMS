import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { EducationQualification, EducationQualificationRequest } from '../models/hrm.model';

@Injectable({ providedIn: 'root' })
export class EducationQualificationService {
  private readonly endpoint = '/hr/education-qualifications';

  constructor(private api: ApiService) {}

  listForEmployee(employeeId: number): Observable<EducationQualification[]> {
    return this.api.get<EducationQualification[]>(`${this.endpoint}/employee/${employeeId}`);
  }

  create(payload: EducationQualificationRequest): Observable<EducationQualification> {
    return this.api.post<EducationQualification>(this.endpoint, payload);
  }

  update(id: number, payload: EducationQualificationRequest): Observable<EducationQualification> {
    return this.api.put<EducationQualification>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
