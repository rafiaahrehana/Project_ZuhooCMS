import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import {
  WorkflowStage,
  WorkflowStageRequest,
  WorkflowTemplate,
  WorkflowTemplateRequest,
} from '../models/servicedesk.model';

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly endpoint = '/workflows';

  constructor(private api: ApiService) {}

  // TEMPLATES
  list(page = 0, size = 20): Observable<PagedResponse<WorkflowTemplate>> {
    return this.api.getPaged<WorkflowTemplate>(this.endpoint, page, size);
  }

  listActive(): Observable<WorkflowTemplate[]> {
    return this.api.get<WorkflowTemplate[]>(`${this.endpoint}/active`);
  }

  getById(id: number): Observable<WorkflowTemplate> {
    return this.api.get<WorkflowTemplate>(`${this.endpoint}/${id}`);
  }

  create(payload: WorkflowTemplateRequest): Observable<WorkflowTemplate> {
    return this.api.post<WorkflowTemplate>(this.endpoint, payload);
  }

  update(id: number, payload: WorkflowTemplateRequest): Observable<WorkflowTemplate> {
    return this.api.put<WorkflowTemplate>(`${this.endpoint}/${id}`, payload);
  }

  toggle(id: number): Observable<WorkflowTemplate> {
    return this.api.patch<WorkflowTemplate>(`${this.endpoint}/${id}/toggle`, {});
  }

  delete(id: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${id}`);
  }

  // STAGES
  addStage(templateId: number, payload: WorkflowStageRequest): Observable<WorkflowStage> {
    return this.api.post<WorkflowStage>(`${this.endpoint}/${templateId}/stages`, payload);
  }

  updateStage(templateId: number, stageId: number, payload: WorkflowStageRequest): Observable<WorkflowStage> {
    return this.api.put<WorkflowStage>(`${this.endpoint}/${templateId}/stages/${stageId}`, payload);
  }

  removeStage(templateId: number, stageId: number): Observable<string> {
    return this.api.deleteText(`${this.endpoint}/${templateId}/stages/${stageId}`);
  }

  suggest(goal: string): Observable<WorkflowSuggestion> {
    return this.api.post<WorkflowSuggestion>(`${this.endpoint}/suggest`, { goal });
  }
}

/** Structured when the backend could parse the model's JSON; raw `suggestion` text otherwise. */
export interface WorkflowSuggestion {
  suggestion?: string;
  name?: string;
  stages?: SuggestedStage[];
}

export interface SuggestedStage {
  name: string;
  purpose?: string;
  needsApproval?: boolean;
}
