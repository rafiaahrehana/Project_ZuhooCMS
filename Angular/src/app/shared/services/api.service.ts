import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  get<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Observable<T> {
    let httpParams = new HttpParams();
    if (params) {
      for (const key of Object.keys(params)) {
        const val = params[key];
        if (val !== undefined && val !== null) {
          httpParams = httpParams.set(key, String(val));
        }
      }
    }
    return this.http.get<T>(`${this.baseUrl}${path}`, { params: httpParams });
  }

  getPaged<T>(
    path: string,
    page = 0,
    size = 20,
    params?: Record<string, string | number | boolean | undefined>
  ): Observable<PagedResponse<T>> {
    let httpParams = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));

    if (params) {
      for (const key of Object.keys(params)) {
        const val = params[key];
        if (val !== undefined && val !== null) {
          httpParams = httpParams.set(key, String(val));
        }
      }
    }
    return this.http.get<PagedResponse<T>>(`${this.baseUrl}${path}`, { params: httpParams });
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body);
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body);
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http.patch<T>(`${this.baseUrl}${path}`, body);
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`);
  }
}
