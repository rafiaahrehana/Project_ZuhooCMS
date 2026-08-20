export type OpportunityStage =
  | 'QUALIFICATION' | 'PRESENTATION' | 'PROPOSAL' | 'NEGOTIATION' | 'WON' | 'LOST';

export type LeadStatus = 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'DISQUALIFIED';

export type CrmActivityType =
  | 'CALL' | 'MEETING' | 'EMAIL' | 'NOTE' | 'TASK'
  | 'FOLLOW_UP' | 'STAGE_CHANGE' | 'STATUS_CHANGE' | 'DOCUMENT';

export const OPEN_STAGES: OpportunityStage[] =
  ['QUALIFICATION', 'PRESENTATION', 'PROPOSAL', 'NEGOTIATION'];

export interface Tag {
  id: number;
  name: string;
  color: string;
}

export interface DuplicateMatch {
  clientId: number;
  clientCompanyName: string;
  matchedOn: string;
}

export interface Lead {
  id: number;
  contactName: string;
  companyName?: string;
  email?: string;
  phone?: string;
  industry?: string;
  jobTitle?: string;
  notes?: string;
  description?: string;
  status: LeadStatus;
  source: string;
  sourceOther?: string;
  priority?: string;
  estimatedValue?: number;
  expectedCloseDate?: string;
  converted: boolean;
  assignedToId?: number;
  assignedToName?: string;
  createdAt: string;
  lastContactDate?: string;
  lastActivityAt?: string;
  companyServiceId?: number;
  companyServiceName?: string;
  assignedToEmail?: string;
  convertedClientId?: number;
  convertedClientName?: string;
  convertedAt?: string;
  activitiesCount?: number;
  updatedAt?: string;
  createdByName?: string;
  updatedByName?: string;
  aiSummary?: string;
  tags?: Tag[];
  possibleDuplicate?: DuplicateMatch;
}

export interface Client {
  id: number;
  userId?: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  image?: string;
  clientCompanyName?: string;
  industry?: string;
  website?: string;
  taxId?: string;
  status: string;
  portalAccessEnabled?: boolean;
  accountManagerId?: number;
  accountManagerName?: string;
  onboardedAt?: string;
  createdAt: string;
  billingAddress?: string;
  shippingAddress?: string;
  /** @deprecated legacy free-text tags - use tagList (shared taxonomy) instead */
  tags?: string;
  tagList?: Tag[];
  employeeCount?: number;
  annualRevenue?: number;
  lifetimeValue?: number;
  totalRequests?: number;
  possibleDuplicate?: DuplicateMatch;
  /**
   * Set only on the inviteToPortal() response: whether the invite email actually
   * left the server. The login is created either way, so false means "account
   * ready, client not yet notified".
   */
  inviteEmailSent?: boolean;
  inviteEmailError?: string;
}

export interface ClientContact {
  id: number;
  clientId: number;
  clientCompanyName?: string;
  fullName: string;
  email?: string;
  phone?: string;
  jobTitle?: string;
  department?: string;
  primaryContact: boolean;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Opportunity {
  id: number;
  name: string;
  description?: string;
  stage: OpportunityStage;
  source?: string;
  amount?: number;
  probability: number;
  weightedAmount?: number;
  expectedCloseDate?: string;
  actualCloseDate?: string;
  nextStep?: string;
  lostReason?: string;
  /** Picklist code; null on rows closed before the picklist existed. */
  lostReasonCode?: LostReason;
  /** Null until the opportunity reaches Won and a Client is created/linked. */
  clientId?: number;
  clientCompanyName?: string;
  contactId?: number;
  contactName?: string;
  ownerId?: number;
  ownerName?: string;
  lastActivityAt?: string;
  stageChangedAt?: string;
  createdAt: string;
  sourceLeadId?: number;
  updatedAt?: string;
  tags?: Tag[];
  possibleDuplicate?: DuplicateMatch;
}

export interface ConvertToOpportunityRequest {
  opportunityName: string;
  expectedValue: number;
  expectedCloseDate: string;
}

export interface ChangeStageRequest {
  stage: OpportunityStage;
  /** Required by the backend when stage is LOST. */
  lostReasonCode?: LostReason;
  /** Free-text detail; the backend requires it when the code is OTHER. */
  lostReason?: string;
  linkToExistingClientId?: number;
  forceCreateNewClient?: boolean;
}

export type LostReason =
  | 'PRICE' | 'COMPETITOR' | 'NO_BUDGET' | 'NO_RESPONSE'
  | 'BAD_TIMING' | 'REQUIREMENTS_MISMATCH' | 'OTHER';

/** Display order doubles as the picklist order in the Lost dialog. */
export const LOST_REASONS: { value: LostReason; label: string }[] = [
  { value: 'PRICE', label: 'Price too high' },
  { value: 'COMPETITOR', label: 'Chose a competitor' },
  { value: 'NO_BUDGET', label: 'No budget' },
  { value: 'NO_RESPONSE', label: 'Went silent / no response' },
  { value: 'BAD_TIMING', label: 'Bad timing' },
  { value: 'REQUIREMENTS_MISMATCH', label: "Requirements didn't fit" },
  { value: 'OTHER', label: 'Other' },
];

export interface PipelineStageSummary {
  stage: OpportunityStage;
  dealCount: number;
  totalAmount: number;
  weightedAmount: number;
}

export interface PipelineSummary {
  stages: PipelineStageSummary[];
  openPipelineValue: number;
  weightedForecast: number;
  wonValue: number;
  totalOpenDeals: number;
}

export interface CrmActivity {
  id: number;
  type: CrmActivityType;
  subject: string;
  description?: string;
  activityDate: string;
  scheduledAt?: string;
  completed: boolean;
  systemGenerated: boolean;
  clientId?: number;
  opportunityId?: number;
  opportunityName?: string;
  performedByName?: string;
  createdAt: string;
  performedById?: number;
}

export interface CrmDashboardSummary {
  pipelineValue: number;
  wonThisMonth: number;
  qualifiedLeadsCount: number;
  conversionRate: number;
  upcomingFollowUps: CrmUpcomingFollowUp[];
  totalClients: number;
  totalLeads: number;
  totalOpportunities: number;
  openOpportunitiesCount: number;
  wonCount: number;
  wonValue: number;
  lostCount: number;
  lostValue: number;
  stageFunnel: { stage: string; count: number; value: number; percent: number }[];
  leadSources: { source: string; count: number }[];
  recentDeals: { id: number; name: string; clientName?: string; amount: number; stage: string }[];
}

export interface CrmUpcomingFollowUp {
  activityId: number;
  subject: string;
  followUpAt: string;
  relatedName?: string;
}
