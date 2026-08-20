import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService } from '../../../../core/services/ai.service';
import { extractErrorMessage } from '../../../../core/utils/http-error.util';
import {
  AiProviderType,
  AiModel,
  AiFeature,
  AiProviderConfig,
  AiProviderConfigRequest,
  AiUsageSummary,
  AiPromptTemplate,
  AiPromptTemplateRequest,
} from '../../models/ai.model';

@Component({
  selector: 'app-ai-settings',
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-settings.html',
})
export class AiSettings implements OnInit {
  // These three lists must stay in step with the backend enums AiProviderType,
  // AiModel and AiFeature - GROQ (and its two models) was fully supported by
  // GroqClient/GroqProviderAdapter but unreachable because it was missing here,
  // and HOLIDAY_DRAFT could not be given a prompt template for the same reason.
  providers: AiProviderType[] = ['GEMINI', 'CLAUDE', 'OPENAI', 'GROQ', 'MOCK'];
  models: AiModel[] = [
    'GEMINI_2_5_FLASH',
    'GEMINI_2_5_PRO',
    'GPT_4O',
    'GPT_4O_MINI',
    'CLAUDE_SONNET',
    'CLAUDE_OPUS',
    'GROQ_LLAMA_3_3_70B',
    'GROQ_LLAMA_3_1_8B',
  ];
  features: AiFeature[] = [
    'EMPLOYMENT_LETTER',
    'LEAVE_POLICY',
    'PERFORMANCE_REVIEW',
    'CRM_LEAD_SUMMARY',
    'CRM_ACTIVITY_SUMMARY',
    'INVOICE_SUMMARY',
    'SERVICE_REQUEST_SUMMARY',
    'ANNOUNCEMENT_DRAFT',
    'HOLIDAY_DRAFT',
    'WORKFLOW_SUGGESTION',
    'SEARCH_ANSWER',
    'BUSINESS_INSIGHTS',
    'GENERAL',
  ];

  // A company can save one config per provider (Claude, Gemini, ... side by
  // side) - configs is every saved one, at most one of them active at a time
  // (the one AiProviderResolver actually uses for generation).
  configs: AiProviderConfig[] = [];
  loadingConfigs = true;

  config: AiProviderConfigRequest = this.emptyConfigForm();
  savingConfig = false;
  configError = '';

  // Form visibility: null editingConfigId = adding a brand-new provider
  // (blank form); a set id = editing that specific saved config.
  showForm = false;
  editingConfigId: number | null = null;

  usageDate = '';
  usage?: AiUsageSummary;
  loadingUsage = false;
  usageError = '';

  templates: AiPromptTemplate[] = [];
  loadingTemplates = false;
  newTemplate: AiPromptTemplateRequest = {
    feature: 'GENERAL',
    name: '',
    template: '',
    changeNotes: '',
  };
  savingTemplate = false;
  templateError = '';

  objectKeys = Object.keys;

  constructor(private aiService: AiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadConfigs();
    this.loadTemplates();
  }

  private emptyConfigForm(): AiProviderConfigRequest {
    return { aiProviderType: 'GEMINI', model: 'GEMINI_2_5_FLASH', temperature: 0.7, maxTokens: 1024 };
  }

  loadConfigs(): void {
    this.loadingConfigs = true;
    this.aiService.listConfigs().subscribe({
      next: (res) => {
        this.configs = res;
        this.loadingConfigs = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.configs = [];
        this.loadingConfigs = false;
        this.cdr.markForCheck();
      },
    });
  }

  // Blank form - adds a genuinely new provider, doesn't touch any existing one.
  openAddNew(): void {
    this.editingConfigId = null;
    this.config = this.emptyConfigForm();
    this.configError = '';
    this.showForm = true;
    this.cdr.markForCheck();
  }

  // Pre-filled form for a specific saved config (API key left blank - "leave
  // blank to keep existing" on save).
  openEdit(c: AiProviderConfig): void {
    this.editingConfigId = c.id ?? null;
    this.config = { aiProviderType: c.provider, model: c.model, temperature: c.temperature, maxTokens: c.maxTokens };
    this.configError = '';
    this.showForm = true;
    this.cdr.markForCheck();
  }

  cancelConfigure(): void {
    this.showForm = false;
    this.configError = '';
    this.cdr.markForCheck();
  }

  saveConfig(): void {
    this.savingConfig = true;
    this.configError = '';
    this.aiService.saveConfig(this.config).subscribe({
      next: () => {
        this.savingConfig = false;
        this.showForm = false;
        this.cdr.markForCheck();
        this.loadConfigs();
      },
      error: (err) => {
        this.configError = extractErrorMessage(err, 'Failed to save provider config');
        this.savingConfig = false;
        this.cdr.markForCheck();
      },
    });
  }

  activate(c: AiProviderConfig): void {
    if (!c.id || c.active) return;
    this.aiService.activateConfig(c.id).subscribe({
      next: () => this.loadConfigs(),
      error: (err) => { this.configError = extractErrorMessage(err, 'Failed to activate provider'); this.cdr.markForCheck(); },
    });
  }

  deleteConfig(c: AiProviderConfig): void {
    if (!c.id) return;
    this.aiService.deleteConfig(c.id).subscribe({
      next: () => this.loadConfigs(),
      error: (err) => { this.configError = extractErrorMessage(err, 'Failed to delete provider'); this.cdr.markForCheck(); },
    });
  }

  loadUsage(): void {
    this.loadingUsage = true;
    this.usageError = '';
    this.aiService.getUsage(this.usageDate || undefined).subscribe({
      next: (res) => {
        this.usage = res;
        this.loadingUsage = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.usageError = err?.error?.message || 'Failed to load usage';
        this.loadingUsage = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadTemplates(): void {
    this.loadingTemplates = true;
    this.aiService.listTemplates().subscribe({
      next: (res) => {
        this.templates = res.content;
        this.loadingTemplates = false;
        this.cdr.markForCheck();
      },
      error: () => { this.loadingTemplates = false; this.cdr.markForCheck(); },
    });
  }

  saveTemplate(): void {
    if (!this.newTemplate.name.trim() || !this.newTemplate.template.trim()) return;
    this.savingTemplate = true;
    this.templateError = '';
    this.aiService.saveTemplate(this.newTemplate).subscribe({
      next: () => {
        this.savingTemplate = false;
        this.newTemplate = { feature: 'GENERAL', name: '', template: '', changeNotes: '' };
        this.cdr.markForCheck();
        this.loadTemplates();
      },
      error: (err) => {
        this.templateError = err?.error?.message || 'Failed to save template';
        this.savingTemplate = false;
        this.cdr.markForCheck();
      },
    });
  }

  deleteTemplate(t: AiPromptTemplate): void {
    this.aiService.deleteTemplate(t.id).subscribe({
      next: () => this.loadTemplates(),
      error: (err) => {
        this.templateError = extractErrorMessage(err, 'Failed to delete template');
        this.cdr.markForCheck();
      },
    });
  }
}
