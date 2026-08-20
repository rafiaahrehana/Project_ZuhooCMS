export interface SupportTicket {
  id: number;
  companyId: number;
  ticketNumber: string;
  // PLATFORM_SUPPORT (tenant -> BusinessOS, the default) or CUSTOMER_SUPPORT
  // (a portal client -> this tenant's own staff).
  ticketType?: 'PLATFORM_SUPPORT' | 'CUSTOMER_SUPPORT';
  assignedEmployeeName?: string;
  title: string;
  description?: string;
  attachmentUrl?: string;
  attachmentFileName?: string;
  categoryName?: string;
  status: string;
  priority: string;
  source: string;
  assignedToAgentId?: number;
  assignedToAgentName?: string;
  assignedDate?: string;
  firstResponseDeadline?: string;
  resolutionDeadline?: string;
  slaBreached: boolean;
  resolutionNotes?: string;
  closedDate?: string;
  satisfactionRating?: number;
  escalationLevel: number;
  createdByName?: string;
  createdAt: string;
  firstResponseTime?: string;
  resolutionTime?: string;
  satisfactionFeedback?: string;
  escalatedDate?: string;
  updatedAt?: string;
}

export interface SupportAgent {
  id: number;
  userId: number;
  fullName: string;
  email?: string;
  department?: string;
  specialization?: string;
  status: string;
  totalTicketsHandled: number;
  avgResponseTimeMinutes: number;
  satisfactionScore: number;
  acceptingTickets: boolean;
  maxConcurrentTickets: number;
  notes?: string;
  createdAt: string;
  userName?: string;
  avgResolutionTimeMinutes?: number;
  lastActiveTime?: string;
  updatedAt?: string;
}

export interface SLAPolicy {
  id: number;
  policyName: string;
  description?: string;
  applicablePriority: string;
  firstResponseTimeHours: number;
  resolutionTimeHours: number;
  businessHoursOnly: boolean;
  active: boolean;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface SLAPolicyRequest {
  policyName: string;
  description?: string;
  applicablePriority: string;
  firstResponseTimeHours: number;
  resolutionTimeHours: number;
  businessHoursOnly: boolean;
  active: boolean;
  notes?: string;
}

export interface SupportAuditLog {
  id: number;
  companyId: number;
  actionByUserId: number;
  actionByUserName?: string;
  actionType: string;
  resourceId: number;
  resourceType?: string;
  description?: string;
  changes?: string;
  ipAddress?: string;
  userAgent?: string;
  contextSwitchToCompanyId?: number;
  contextSwitchToCompanyName?: string;
  createdAt: string;
}

export interface SupportContextSwitch {
  id: number;
  supportAgentId: number;
  supportAgentName?: string;
  viewedCompanyId: number;
  viewedCompanyName?: string;
  switchedInTime: string;
  switchedOutTime?: string;
  purpose?: string;
  ipAddress?: string;
  userAgent?: string;
  stillActive: boolean;
}

export interface SupportContextSwitchRequest {
  viewedCompanyId: number;
  purpose?: string;
}

export interface SupportMessage {
  id: number;
  ticketId: number;
  sentById: number;
  sentByName?: string;
  message: string;
  messageType: string;
  isInternal: boolean;
  attachmentUrl?: string;
  attachmentFileName?: string;
  attachmentSize?: number;
  isResolution: boolean;
  createdAt: string;
}

export interface SupportMessageRequest {
  ticketId: number;
  message: string;
  sentByUserId?: number;
  messageType?: string;
  isInternal?: boolean;
  attachmentUrl?: string;
  attachmentFileName?: string;
  isResolution?: boolean;
}

export interface SupportCategory {
  id: number;
  categoryName: string;
  description?: string;
  active: boolean;
  icon?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface SupportCategoryRequest {
  categoryName: string;
  description?: string;
  active?: boolean;
  icon?: string;
}
