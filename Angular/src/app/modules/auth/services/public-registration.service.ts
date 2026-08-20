import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface PublicCompanyOption {
  id: number;
  companyName: string;
  subdomain: string;
  logo?: string;
}

export interface ClientRegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
  companyId: number;
  clientCompanyName?: string;
  industry?: string;
  website?: string;
}

@Injectable({
  providedIn: 'root',
})
export class PublicRegistrationService {
  private readonly API_URL = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  /** Companies a prospective client can register under (public, unauthenticated). */
  getCompanies(): Observable<PublicCompanyOption[]> {
    return this.http.get<PublicCompanyOption[]>(`${this.API_URL}/companies/public/list`);
  }

  registerClient(request: ClientRegisterRequest): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/clients/public/register`, request);
  }
}
