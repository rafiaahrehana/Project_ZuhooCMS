import { Injectable } from '@angular/core';
import { HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from './api.service';
import { SKIP_ERROR_TOAST } from '../interceptors/http-context-tokens';
import {
  AiProviderConfig,
  AiProviderConfigRequest,
  AiUsageSummary,
  AiPromptTemplate,
  AiPromptTemplateRequest,
} from '../../modules/ai/models/ai.model';

export interface AiGenerateResponse {
  conversationUuid: string;
  feature: string;
  provider: string;
  model: string;
  result: string;
  executionTimeMs: number;
  threadId?: number;
  awaitingConfirmation?: boolean;
}

export interface AiThread {
  id: number;
  feature: string;
  title?: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class AiService {
  constructor(private api: ApiService) {}

  generate(feature: string, prompt: string): Observable<AiGenerateResponse> {
    return this.api.post<AiGenerateResponse>('/ai/generate', { feature, prompt });
  }

  conversations(feature?: string, page = 0): Observable<PagedResponse<AiGenerateResponse>> {
    return this.api.getPaged<AiGenerateResponse>('/ai/conversations', page, 20, feature ? { feature } : undefined);
  }

  getConfig(): Observable<AiProviderConfig> {
    // ai-settings.ts already treats a failed load as "no config yet" and keeps its
    // defaults - a fresh company legitimately has none, so this isn't worth alarming
    // the user with a global toast every time they open the page.
    return this.api.get<AiProviderConfig>('/ai/config', undefined, new HttpContext().set(SKIP_ERROR_TOAST, true));
  }

  saveConfig(config: AiProviderConfigRequest): Observable<AiProviderConfig> {
    return this.api.post<AiProviderConfig>('/ai/config', config);
  }

  // A company can save one config per provider - this lists all of them
  // (Claude, Gemini, etc. side by side), not just the currently active one.
  listConfigs(): Observable<AiProviderConfig[]> {
    return this.api.get<AiProviderConfig[]>('/ai/configs');
  }

  activateConfig(id: number): Observable<AiProviderConfig> {
    return this.api.patch<AiProviderConfig>(`/ai/config/${id}/activate`, {});
  }

  deleteConfig(id: number): Observable<void> {
    return this.api.delete<void>(`/ai/config/${id}`);
  }

  getUsage(date?: string): Observable<AiUsageSummary> {
    return this.api.get<AiUsageSummary>('/ai/usage', date ? { date } : undefined);
  }

  listTemplates(page = 0, size = 20): Observable<PagedResponse<AiPromptTemplate>> {
    return this.api.getPaged<AiPromptTemplate>('/ai/templates', page, size);
  }

  saveTemplate(template: AiPromptTemplateRequest): Observable<AiPromptTemplate> {
    return this.api.post<AiPromptTemplate>('/ai/templates', template);
  }

  deleteTemplate(id: number): Observable<void> {
    return this.api.delete<void>(`/ai/templates/${id}`);
  }

  // ── Conversation threads ──────────────────────────────────

  createThread(feature: string): Observable<AiThread> {
    return this.api.post<AiThread>('/ai/threads', { feature });
  }

  listThreads(page = 0, size = 30): Observable<PagedResponse<AiThread>> {
    return this.api.getPaged<AiThread>('/ai/threads', page, size);
  }

  threadMessages(threadId: number, page = 0, size = 50): Observable<PagedResponse<AiGenerateResponse>> {
    return this.api.getPaged<AiGenerateResponse>(`/ai/threads/${threadId}/messages`, page, size);
  }

  deleteThread(threadId: number): Observable<void> {
    return this.api.delete<void>(`/ai/threads/${threadId}`);
  }

  // ── Agent (tool-calling) ──────────────────────────────────

  agentTurn(threadId: number, message: string): Observable<AiGenerateResponse> {
    return this.api.post<AiGenerateResponse>('/ai/agent/turn', { threadId, message });
  }

  // ── Proactive daily briefing ──────────────────────────────

  dailyBriefing(): Observable<{ content: string }> {
    return this.api.get<{ content: string }>('/ai/daily-briefing');
  }
}
