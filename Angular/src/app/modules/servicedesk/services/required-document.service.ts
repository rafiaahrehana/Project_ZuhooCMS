import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { RequiredDocument, RequiredDocumentRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class RequiredDocumentService {
  constructor(private api: ApiService) {}

  private endpoint(serviceId: number): string {
    return `/v1/services/${serviceId}/required-documents`;
  }

  list(serviceId: number): Observable<RequiredDocument[]> {
    return this.api.get<RequiredDocument[]>(this.endpoint(serviceId));
  }

  create(serviceId: number, payload: RequiredDocumentRequest): Observable<RequiredDocument> {
    return this.api.post<RequiredDocument>(this.endpoint(serviceId), payload);
  }

  update(serviceId: number, id: number, payload: RequiredDocumentRequest): Observable<RequiredDocument> {
    return this.api.put<RequiredDocument>(`${this.endpoint(serviceId)}/${id}`, payload);
  }

  delete(serviceId: number, id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint(serviceId)}/${id}`);
  }
}
