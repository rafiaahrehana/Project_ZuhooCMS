import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { AverageRating, ServiceReview, ServiceReviewRequest } from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class ServiceReviewService {
  private readonly endpoint = '/reviews';

  constructor(private api: ApiService) {}

  list(page = 0, size = 20): Observable<PagedResponse<ServiceReview>> {
    return this.api.getPaged<ServiceReview>(this.endpoint, page, size);
  }

  listForService(hubServiceId: number, page = 0, size = 20): Observable<PagedResponse<ServiceReview>> {
    return this.api.getPaged<ServiceReview>(`${this.endpoint}/service/${hubServiceId}`, page, size);
  }

  getById(id: number): Observable<ServiceReview> {
    return this.api.get<ServiceReview>(`${this.endpoint}/${id}`);
  }

  create(payload: ServiceReviewRequest): Observable<ServiceReview> {
    return this.api.post<ServiceReview>(this.endpoint, payload);
  }

  averageRating(): Observable<AverageRating> {
    return this.api.get<number>(`${this.endpoint}/average-rating`).pipe(
      map(val => ({ average: val || 0 }))
    );
  }

  averageRatingForService(hubServiceId: number): Observable<AverageRating> {
    return this.api.get<number>(`${this.endpoint}/service/${hubServiceId}/average-rating`).pipe(
      map(val => ({ average: val || 0 }))
    );
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/${id}`);
  }
}
