export type NotificationType =
  | 'REQUEST_SUBMITTED' | 'REQUEST_ASSIGNED' | 'REQUEST_UPDATED'
  | 'COMPLETED' | 'REJECTED' | 'CANCELLED'
  | 'PAYMENT_DUE' | 'PAYMENT_RECEIVED' | 'INVOICE_GENERATED' | 'REFUND_PROCESSED'
  | 'SLA_WARNING' | 'SLA_BREACHED'
  | 'LEAVE_APPROVED' | 'LEAVE_REJECTED' | 'PAYSLIP_READY' | 'LETTER_ISSUED'
  | 'WELCOME' | 'TEAM_INVITE' | 'ANNOUNCEMENT'
  | 'GENERAL';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  actionUrl?: string;
  read: boolean;
  readAt?: string;
  serviceRequestId?: number;
  createdAt: string;
}

export interface NotificationCount {
  unreadCount: number;
}

export interface NotificationPreference {
  id: number;
  emailOnServiceRequest: boolean;
  emailOnStatusChange: boolean;
  emailOnInvoice: boolean;
  emailOnPayment: boolean;
  emailOnTaskAssigned: boolean;
  emailOnLeaveUpdate: boolean;
  inAppOnServiceRequest: boolean;
  inAppOnStatusChange: boolean;
  emailMarketing: boolean;
  updatedAt?: string;
}

export interface UpdateNotificationPreferenceRequest {
  emailOnServiceRequest: boolean;
  emailOnStatusChange: boolean;
  emailOnInvoice: boolean;
  emailOnPayment: boolean;
  emailOnTaskAssigned: boolean;
  emailOnLeaveUpdate: boolean;
  inAppOnServiceRequest: boolean;
  inAppOnStatusChange: boolean;
  emailMarketing: boolean;
}
