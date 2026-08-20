export type AiProviderType = 'GEMINI' | 'CLAUDE' | 'OPENAI' | 'GROQ' | 'MOCK';
export type AiModel =
  | 'GEMINI_2_5_FLASH'
  | 'GEMINI_2_5_PRO'
  | 'GPT_4O'
  | 'GPT_4O_MINI'
  | 'CLAUDE_SONNET'
  | 'CLAUDE_OPUS'
  | 'GROQ_LLAMA_3_3_70B'
  | 'GROQ_LLAMA_3_1_8B';
export type AiFeature =
  | 'EMPLOYMENT_LETTER'
  | 'LEAVE_POLICY'
  | 'PERFORMANCE_REVIEW'
  | 'CRM_LEAD_SUMMARY'
  | 'CRM_ACTIVITY_SUMMARY'
  | 'INVOICE_SUMMARY'
  | 'SERVICE_REQUEST_SUMMARY'
  | 'ANNOUNCEMENT_DRAFT'
  | 'HOLIDAY_DRAFT'
  | 'WORKFLOW_SUGGESTION'
  | 'SEARCH_ANSWER'
  | 'BUSINESS_INSIGHTS'
  | 'GENERAL'
  | 'AGENT_TASK';

export interface AiProviderConfig {
  id?: number;
  provider: AiProviderType;
  model: AiModel;
  temperature: number;
  maxTokens: number;
  apiKey?: string;
  active?: boolean;
  createdAt?: string;
}

export interface AiProviderConfigRequest {
  aiProviderType: AiProviderType;
  model: AiModel;
  temperature: number;
  maxTokens: number;
  apiKey?: string;
}

export interface AiUsageSummary {
  date?: string;
  totalRequests: number;
  totalTokens?: number;
  avgResponseTimeMs?: number;
  requestsByFeature?: { [key: string]: number };
  tokensByFeature?: { [key: string]: number };
}

export interface AiPromptTemplate {
  id: number;
  feature: AiFeature;
  name: string;
  template: string;
  version: number;
  changeNotes?: string;
  active?: boolean;
  companyId?: number;
  updatedById?: number;
  updatedByName?: string;
  updatedAt?: string;
}

export interface AiPromptTemplateRequest {
  feature: AiFeature;
  name: string;
  template: string;
  changeNotes?: string;
}
