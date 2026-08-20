import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { ImpersonationResponse } from '../../../core/models/auth.model';
import {
  Company,
  CompanyStatus,
  RegisterCompanyRequest,
  SubscriptionPlan,
} from '../models/platform-admin.model';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly endpoint = '/companies';

  constructor(private api: ApiService) {}

  // LIST ALL COMPANIES (platform staff) - all filters optional and combinable
  list(page = 0, size = 20, status?: CompanyStatus, plan?: SubscriptionPlan, keyword?: string): Observable<PagedResponse<Company>> {
    const params: Record<string, string | number> = {};
    if (status) params['status'] = status;
    if (plan) params['plan'] = plan;
    if (keyword?.trim()) params['keyword'] = keyword.trim();
    return this.api.getPaged<Company>(this.endpoint, page, size, params);
  }

  getById(id: number): Observable<Company> {
    return this.api.get<Company>(`${this.endpoint}/${id}`);
  }

  // REGISTER A NEW COMPANY WITH ITS FIRST OWNER
  register(payload: RegisterCompanyRequest): Observable<Company> {
    return this.api.post<Company>(`${this.endpoint}/admin`, payload);
  }

  // CHANGE PLAN (BACKEND EXPECTS ?plan= QUERY PARAM; amountPaid/transactionRef are
  // optional and recorded into SubscriptionHistory for the platform revenue stats)
  changePlan(id: number, plan: SubscriptionPlan, amountPaid?: number, transactionRef?: string): Observable<Company> {
    let query = `plan=${plan}`;
    if (amountPaid != null) query += `&amountPaid=${amountPaid}`;
    if (transactionRef) query += `&transactionRef=${encodeURIComponent(transactionRef)}`;
    return this.api.patch<Company>(`${this.endpoint}/${id}/plan?${query}`, {});
  }

  // CHANGE STATUS (BACKEND EXPECTS ?status= QUERY PARAM)
  changeStatus(id: number, status: CompanyStatus): Observable<Company> {
    return this.api.patch<Company>(`${this.endpoint}/${id}/status?status=${status}`, {});
  }

  deactivate(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }

  // ACCESS COMPANY (impersonate) - works even for suspended/deactivated companies;
  // reason is required server-side for the impersonation audit log.
  impersonate(id: number, reason: string): Observable<ImpersonationResponse> {
    return this.api.post<ImpersonationResponse>(`/platform-admin/companies/${id}/impersonate`, { reason });
  }
}
