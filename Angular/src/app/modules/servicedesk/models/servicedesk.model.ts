export interface ServiceRequest {
  id: number;
  title: string;
  description?: string;
  status: string;
  priority: string;
  agreedPrice?: number;
  slaDeadline?: string;
  slaBreach: boolean;
  assignedAt?: string;
  completedAt?: string;
  companyId?: number;
  clientId?: number;
  clientName?: string;
  hubServiceId?: number;
  hubServiceName?: string;
  assignedEmployeeId?: number;
  assignedEmployeeName?: string;
  taskCount: number;
  completedTaskCount: number;
  subscriptionId?: number;
  packageName?: string;
  resubmitCount: number;
  permanentlyClosed: boolean;
  paymentRedirectUrl?: string;
  createdAt: string;
  updatedAt?: string;
  // Invoice generated when a quotation is accepted
  invoiceId?: number;
  // Filing reference once staff submits this request to a government authority
  govRefNumber?: string;
  govRefType?: string;
  // Quotation fields embedded directly on the service request (there is no
  // standalone Quotation entity/endpoint - QuotationController was removed
  // and this data now lives on ServiceRequestResponse)
  quotationAmount?: number;
  quotationCurrency?: string;
  quotationNotes?: string;
  quotationValidUntil?: string;
  quotationStatus?: QuotationStatus;
  // Client answers to the service's dynamic form fields, keyed by field id
  formData?: Record<string, string>;
  aiSummary?: string;
}

// Pre-sales proposal: what staff intend to build (stack, timeline, mockups)
// for a customized project, ahead of the binding Quotation.
export type ProposalStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'CHANGES_REQUESTED';

export interface ProposalAttachment {
  id: number;
  proposalId: number;
  fileName: string;
  fileUrl: string;
  label?: string;
}

export interface ProposalAttachmentRequest {
  fileName: string;
  fileUrl: string;
  label?: string;
}

export interface Proposal {
  id: number;
  serviceRequestId: number;
  title: string;
  techStack?: string;
  timeline?: string;
  summary?: string;
  estimatedBudget?: string;
  status: ProposalStatus;
  clientFeedback?: string;
  createdByName?: string;
  sentAt?: string;
  respondedAt?: string;
  createdAt: string;
  updatedAt?: string;
  attachments: ProposalAttachment[];
}

export interface ProposalRequest {
  title: string;
  techStack?: string;
  timeline?: string;
  summary?: string;
  estimatedBudget?: string;
}

export interface StageApproval {
  id: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  approverRole?: string;
  decisionNotes?: string;
  decidedAt?: string;
  serviceRequestId: number;
  serviceRequestTitle?: string;
  workflowStageId: number;
  workflowStageName?: string;
  stageOrder?: number;
  requestedByName?: string;
  decidedByName?: string;
  createdAt: string;
}

export interface KbArticle {
  id: number;
  title: string;
  summary?: string;
  content: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  clientVisible: boolean;
  keywords?: string;
  viewCount: number;
  helpfulCount: number;
  publishedAt?: string;
  categoryId?: number;
  categoryName?: string;
  relatedServiceId?: number;
  relatedServiceName?: string;
  authorName?: string;
  createdAt: string;
}

export interface RequestComment {
  id: number;
  content: string;
  visibility: CommentVisibility;
  attachmentUrl?: string;
  authorName?: string;
  createdAt: string;
  authorId?: number;
}

export interface RequestStatusHistoryResponse {
  id: number;
  oldStatus?: string;
  newStatus: string;
  reason?: string;
  changedById?: number;
  changedByName?: string;
  changedAt: string;
}

// Shared enums used by the service-desk configuration surface
export type BillingCycle = 'MONTHLY' | 'QUARTERLY' | 'YEARLY' | 'ONE_TIME';
export const BILLING_CYCLES: BillingCycle[] = ['MONTHLY', 'QUARTERLY', 'YEARLY', 'ONE_TIME'];

export type QuotationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';
export const QUOTATION_STATUSES: QuotationStatus[] = ['PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED'];

export type ServiceRequestPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export const SERVICE_REQUEST_PRIORITIES: ServiceRequestPriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];

export type ServicePriceType = 'FIXED' | 'HOURLY' | 'DAILY' | 'MONTHLY' | 'YEARLY' | 'CUSTOM';
export const SERVICE_PRICE_TYPES: ServicePriceType[] = ['FIXED', 'HOURLY', 'DAILY', 'MONTHLY', 'YEARLY', 'CUSTOM'];

export type ServiceVisibility = 'PUBLIC' | 'PRIVATE' | 'DRAFT';
export const SERVICE_VISIBILITIES: ServiceVisibility[] = ['PUBLIC', 'PRIVATE', 'DRAFT'];

export type SubscriptionStatus = 'ACTIVE' | 'PENDING_PAYMENT' | 'EXPIRED' | 'SUSPENDED' | 'CANCELLED';
export const SUBSCRIPTION_STATUSES: SubscriptionStatus[] = ['ACTIVE', 'PENDING_PAYMENT', 'EXPIRED', 'SUSPENDED', 'CANCELLED'];

// Comment visibility: PUBLIC is visible to the client, INTERNAL is staff-only
export type CommentVisibility = 'PUBLIC' | 'INTERNAL' | 'CLIENT';
export const COMMENT_VISIBILITIES: CommentVisibility[] = ['PUBLIC', 'INTERNAL', 'CLIENT'];

// Task status lifecycle
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export const TASK_STATUSES: TaskStatus[] = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

export interface ServiceCategory {
  id: number;
  name: string;
  nameBn?: string;
  description?: string;
  iconUrl?: string;
  sortOrder: number;
  active: boolean;
}

export interface ServiceCategoryRequest {
  name: string;
  nameBn?: string;
  description?: string;
  iconUrl?: string;
  sortOrder?: number;
}

export interface CompanyService {
  id: number;
  name: string;
  nameBn?: string;
  description?: string;
  descriptionBn?: string;
  price?: number;
  priceType?: ServicePriceType;
  estimatedDays?: number;
  defaultPriority?: ServiceRequestPriority;
  active: boolean;
  categoryId?: number;
  categoryName?: string;
  workflowTemplateId?: number;
  workflowTemplateName?: string;
  serviceTemplateId?: number;
  serviceTemplateName?: string;
  currency?: string;
  featured: boolean;
  remote: boolean;
  onSite: boolean;
  online: boolean;
  maximumOrders?: number;
  autoApproval: boolean;
  requiresQuotation?: boolean;
  createdAt: string;
  requiresDocuments?: boolean;
  supportsCustomWorkflow?: boolean;
  aiAssisted?: boolean;
  visibility?: string;
}

export interface CompanyServiceRequest {
  name: string;
  nameBn?: string;
  description?: string;
  descriptionBn?: string;
  price?: number;
  priceType?: ServicePriceType;
  estimatedDays?: number;
  defaultPriority?: ServiceRequestPriority;
  categoryId?: number;
  workflowTemplateId?: number;
  serviceTemplateId?: number;
  currency?: string;
  featured?: boolean;
  remote?: boolean;
  onSite?: boolean;
  online?: boolean;
  maximumOrders?: number;
  autoApproval?: boolean;
  requiresQuotation?: boolean;
  requiresDocuments?: boolean;
  supportsCustomWorkflow?: boolean;
  aiAssisted?: boolean;
  visibility?: ServiceVisibility;
}

export interface ServicePackage {
  id: number;
  name: string;
  nameBn?: string;
  description?: string;
  descriptionBn?: string;
  iconUrl?: string;
  packagePrice?: number;
  discountPercent?: number;
  billingCycle?: BillingCycle;
  requestQuota?: number;
  autoRenew: boolean;
  active: boolean;
  deliveryDays?: number;
  terms?: string;
  featured: boolean;
  popular: boolean;
  effectivePrice?: number;
  totalServicePrice?: number;
  services?: CompanyService[];
  createdAt: string;
  updatedAt?: string;
}

export interface ServicePackageRequest {
  name: string;
  nameBn?: string;
  description?: string;
  descriptionBn?: string;
  iconUrl?: string;
  packagePrice?: number;
  discountPercent?: number;
  billingCycle?: BillingCycle;
  requestQuota?: number;
  autoRenew?: boolean;
  deliveryDays?: number;
  terms?: string;
  featured?: boolean;
  popular?: boolean;
  serviceIds?: number[];
}

export interface SubscribeRequest {
  packageId: number;
  // Optional: only set when staff subscribes on behalf of a client. When the
  // caller is CLIENT, the backend resolves clientId from the JWT itself.
  clientId?: number;
  autoRenew?: boolean;
}

export interface PackageSubscription {
  id: number;
  packageId: number;
  packageName?: string;
  clientId: number;
  clientName?: string;
  status: SubscriptionStatus;
  billingCycle?: BillingCycle;
  startDate?: string;
  endDate?: string;
  nextBillingDate?: string;
  pricePaid?: number;
  requestQuota?: number;
  requestsUsed: number;
  remainingRequests: number;
  autoRenew: boolean;
  activatedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  createdAt: string;
}

export interface WorkflowStage {
  id: number;
  name: string;
  description?: string;
  stageOrder?: number;
  estimatedDays?: number;
  slaHours?: number;
  requiresApproval?: boolean;
  assigneeRole?: string;
  // Milestone billing: when this stage completes, the client is notified that
  // paymentPercent% of the request's agreed price is due (see advanceStage()).
  requiresPayment?: boolean;
  paymentPercent?: number;
  createdAt?: string;
}

export interface WorkflowStageRequest {
  name: string;
  description?: string;
  stageOrder?: number;
  estimatedDays?: number;
  slaHours?: number;
  requiresApproval?: boolean;
  assigneeRole?: string;
  requiresPayment?: boolean;
  paymentPercent?: number;
}

export interface WorkflowTemplate {
  id: number;
  name: string;
  description?: string;
  version: number;
  active: boolean;
  stages?: WorkflowStage[];
  createdAt: string;
  updatedAt?: string;
}

export interface WorkflowTemplateRequest {
  name: string;
  description?: string;
}

export type FormFieldType =
  | 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'DROPDOWN' | 'CHECKBOX' | 'RADIO'
  | 'DATE' | 'FILE_UPLOAD' | 'EMAIL' | 'PHONE' | 'FORMULA';
export const FORM_FIELD_TYPES: FormFieldType[] = [
  'TEXT', 'TEXTAREA', 'NUMBER', 'DROPDOWN', 'CHECKBOX', 'RADIO',
  'DATE', 'FILE_UPLOAD', 'EMAIL', 'PHONE', 'FORMULA'
];

// Template nested items
export interface TemplateFormField {
  id?: number;
  serviceTemplateId?: number;
  label: string;
  fieldType: FormFieldType;
  required: boolean;
  validationRules?: string;
  sortOrder: number;
}

export interface TemplateRequiredDocument {
  id?: number;
  serviceTemplateId?: number;
  docName: string;
  description?: string;
  mandatory: boolean;
  maxAgeDays?: number;
  allowedFormats?: string;
  sortOrder: number;
}

export interface TemplateWorkflowStage {
  id?: number;
  serviceTemplateId?: number;
  stageName: string;
  stageDescription?: string;
  stageOrder: number;
  requiresClientAction: boolean;
  requiresPayment: boolean;
  isFinalStage: boolean;
}

export interface ServiceTemplate {
  id: number;
  name: string;
  description?: string;
  categoryId?: number;
  categoryName?: string;
  defaultPrice?: number;
  estimatedDays?: number;
  iconUrl?: string;
  active: boolean;
  formFields?: TemplateFormField[];
  requiredDocuments?: TemplateRequiredDocument[];
  workflowStages?: TemplateWorkflowStage[];
}

export interface ServiceTemplateRequest {
  name: string;
  description?: string;
  categoryId?: number;
  defaultPrice?: number;
  estimatedDays?: number;
  iconUrl?: string;
  active?: boolean;
  formFields?: TemplateFormField[];
  requiredDocuments?: TemplateRequiredDocument[];
  workflowStages?: TemplateWorkflowStage[];
}

// Service form fields (nested under a service, distinct from template form fields)
export interface ServiceFormField {
  id: number;
  serviceId: number;
  label: string;
  fieldType: FormFieldType;
  required: boolean;
  validationRules?: string;
  sortOrder: number;
}

export interface ServiceFormFieldRequest {
  label: string;
  fieldType: FormFieldType;
  required: boolean;
  validationRules?: string;
  sortOrder: number;
}

// Required documents (service-level, distinct from TemplateRequiredDocument -
// this is the checklist actually enforced by validateRequiredDocuments()
// before a request can be assigned/moved into progress).
export interface RequiredDocument {
  id: number;
  serviceId: number;
  docName: string;
  description?: string;
  mandatory: boolean;
  maxAgeDays?: number;
  allowedFormats?: string;
  sortOrder: number;
}

export interface RequiredDocumentRequest {
  docName: string;
  description?: string;
  mandatory: boolean;
  maxAgeDays?: number;
  allowedFormats?: string;
  sortOrder: number;
}

// A service that depends on another service having been completed first
// (e.g. Trade License Registration requires a completed Company Incorporation).
export interface ServicePrerequisite {
  id: number;
  serviceId: number;
  prerequisiteServiceId: number;
  prerequisiteServiceName?: string;
  mandatory: boolean;
  message?: string;
}

export interface ServicePrerequisiteRequest {
  prerequisiteServiceId: number;
  mandatory: boolean;
  message?: string;
}

// Reviews
export interface ServiceReview {
  id: number;
  rating: number;
  comment?: string;
  published: boolean;
  serviceRequestId?: number;
  hubServiceId?: number;
  hubServiceName?: string;
  clientId?: number;
  clientName?: string;
  createdAt: string;
}

export interface ServiceReviewRequest {
  serviceRequestId: number;
  rating: number;
  comment?: string;
}

export interface AverageRating {
  average: number;
  count?: number;
}

export interface TaskResponse {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: ServiceRequestPriority;
  dueDate?: string;
  slaDeadline?: string;
  completedAt?: string;
  serviceRequestId: number;
  assignedEmployeeId?: number;
  assignedEmployeeName?: string;
  createdById?: number;
  createdByName?: string;
  workflowStageId?: number;
  workflowStageName?: string;
  createdAt: string;
}

export interface StageItem {
  stageId: number;
  name: string;
  stageOrder: number;
  slaHours?: number;
  requiresApproval: boolean;
  completed: boolean;
  current: boolean;
  approvalStatus?: string;
  requiresPayment?: boolean;
  paymentPercent?: number;
}

export interface StageProgressResponse {
  serviceRequestId: number;
  currentStage: number;
  totalStages: number;
  stages: StageItem[];
}
