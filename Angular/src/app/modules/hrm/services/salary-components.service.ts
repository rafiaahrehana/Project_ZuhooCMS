import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

export type ComponentType = 'EARNING' | 'DEDUCTION' | 'EMPLOYER_CONTRIBUTION';

export interface SalaryComponent {
  id: number;
  name: string;
  type: ComponentType;
  calculationType: 'FIXED' | 'PERCENTAGE';
  taxable: boolean;
  active: boolean;
  sortOrder: number;
}

export interface SalaryStructureTemplate {
  id?: number;
  structureName: string;
  /** The grade's standard package - fills Gross when the form has none. */
  defaultGross?: number | null;
  basicPercentage: number;
  hraPercentage: number;
  medicalAmount: number;
  transportAmount: number;
  internetAmount: number;
  mobileAmount: number;
  mealAmount: number;
  active?: boolean;
}

/** The computed split a template produces for a given gross. */
export interface TemplateBreakdown {
  grossSalary: number;
  basicSalary: number;
  houseRent: number;
  medicalAllowance: number;
  transportAllowance: number;
  internetAllowance: number;
  mobileAllowance: number;
  foodAllowance: number;
  specialAllowance: number;
}

export interface StructureExtra {
  id?: number;
  componentId: number;
  componentName?: string;
  type?: ComponentType;
  amount: number;
}

@Injectable({ providedIn: 'root' })
export class SalaryComponentsService {
  private readonly endpoint = '/hr/salary-components';

  constructor(private api: ApiService) {}

  // Catalog
  catalog(): Observable<SalaryComponent[]> {
    return this.api.get<SalaryComponent[]>(this.endpoint);
  }

  createComponent(c: Partial<SalaryComponent>): Observable<SalaryComponent> {
    return this.api.post<SalaryComponent>(this.endpoint, c);
  }

  updateComponent(id: number, c: Partial<SalaryComponent>): Observable<SalaryComponent> {
    return this.api.put<SalaryComponent>(`${this.endpoint}/${id}`, c);
  }

  // Templates
  templates(): Observable<SalaryStructureTemplate[]> {
    return this.api.get<SalaryStructureTemplate[]>(`${this.endpoint}/templates`);
  }

  createTemplate(t: SalaryStructureTemplate): Observable<SalaryStructureTemplate> {
    return this.api.post<SalaryStructureTemplate>(`${this.endpoint}/templates`, t);
  }

  updateTemplate(id: number, t: SalaryStructureTemplate): Observable<SalaryStructureTemplate> {
    return this.api.put<SalaryStructureTemplate>(`${this.endpoint}/templates/${id}`, t);
  }

  deleteTemplate(id: number): Observable<void> {
    return this.api.delete<void>(`${this.endpoint}/templates/${id}`);
  }

  breakdown(templateId: number, gross: number): Observable<TemplateBreakdown> {
    return this.api.get<TemplateBreakdown>(`${this.endpoint}/templates/${templateId}/breakdown?gross=${gross}`);
  }

  // Per-structure extra components
  extras(structureId: number): Observable<StructureExtra[]> {
    return this.api.get<StructureExtra[]>(`${this.endpoint}/structure/${structureId}`);
  }

  setExtras(structureId: number, lines: { componentId: number; amount: number }[]): Observable<StructureExtra[]> {
    return this.api.put<StructureExtra[]>(`${this.endpoint}/structure/${structureId}`, lines);
  }
}
