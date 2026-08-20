import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { CompanyService as HubService } from '../servicedesk/models/servicedesk.model';
import { LocationResponse } from '../../shared/models/location.model';

// Public portal data - mirror of backend CompanyPublicResponse
export interface CompanyPublic {
  id: number;
  companyName: string;
  subdomain: string;
  logo?: string;
  primaryColor?: string;
  secondaryColor?: string;
  tagline?: string;
  portalAbout?: string;
  website?: string;
  location?: string;
  companyPhone?: string;
  companyEmail?: string;
}

// Owner's own company - subset of backend CompanyResponse used by the settings page
export interface MyCompany extends CompanyPublic {
  status: string;
  subscriptionPlan: string;
  locationDetail?: LocationResponse;
  taxRegistrationNumber?: string;
  bankName?: string;
  bankAccountName?: string;
  bankAccountNumber?: string;
  bankBranch?: string;
  fiscalYearStartMonth?: number;
  baseCurrency?: string;
}

export interface UpdateMyCompanyRequest {
  companyName?: string;
  companyPhone?: string;
  website?: string;
  portalAbout?: string;
  locationDetail?: {
    country?: string;
    level1?: string;
    level2?: string;
    level3?: string;
    level4?: string;
    postalCode?: string;
    streetAddress?: string;
    apartment?: string;
  };
  taxRegistrationNumber?: string;
  bankName?: string;
  bankAccountName?: string;
  bankAccountNumber?: string;
  bankBranch?: string;
  fiscalYearStartMonth?: number;
  baseCurrency?: string;
}

@Injectable({ providedIn: 'root' })
export class PortalService {
  constructor(private api: ApiService) {}

  // Public (no auth) - portal page data
  getPublic(subdomain: string): Observable<CompanyPublic> {
    return this.api.get<CompanyPublic>(`/companies/public/${subdomain}`);
  }

  getPublicServices(subdomain: string): Observable<HubService[]> {
    return this.api.get<HubService[]>(`/companies/public/${subdomain}/services`);
  }

  // Owner (COMPANY_OWNER) - website view / subdomain lookup
  getMyCompany(): Observable<MyCompany> {
    return this.api.get<MyCompany>('/companies/me');
  }

  updateMyCompany(payload: UpdateMyCompanyRequest): Observable<MyCompany> {
    return this.api.patch<MyCompany>('/companies/me', payload);
  }
}
