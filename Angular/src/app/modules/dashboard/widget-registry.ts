import { Type } from '@angular/core';
import { PermissionCode } from '../../core/services/permission.service';
import { TotalLeadsWidget } from './widgets/total-leads-widget';
import { AccountsWidget } from './widgets/accounts-widget';
import { OpenOpportunitiesWidget } from './widgets/open-opportunities-widget';
import { WeightedForecastWidget } from './widgets/weighted-forecast-widget';
import { PendingRequestsWidget } from './widgets/pending-requests-widget';
import { InProgressRequestsWidget } from './widgets/in-progress-requests-widget';
import { SlaBreachedWidget } from './widgets/sla-breached-widget';
import { SupportTicketsWidget } from './widgets/support-tickets-widget';
import { OutstandingInvoicesWidget } from './widgets/outstanding-invoices-widget';
import { WalletBalanceWidget } from './widgets/wallet-balance-widget';
import { WalletCreditsWidget } from './widgets/wallet-credits-widget';
import { TotalEmployeesWidget } from './widgets/total-employees-widget';
import { PendingLeaveApprovalsWidget } from './widgets/pending-leave-approvals-widget';
import { PayrollStatusWidget } from './widgets/payroll-status-widget';

export type DashboardSection = 'CRM' | 'Servicedesk & Support' | 'Finance' | 'HRM';

export interface DashboardWidgetDef {
  id: string;
  section: DashboardSection;
  component: Type<unknown>;
  requiredPermission: PermissionCode;
}

/**
 * Every dashboard widget the Company Owner's view can show, each gated by a single
 * permission code. Adding a brand-new custom role/permission combination needs zero
 * new frontend code as long as it maps to an existing widget - only a genuinely new
 * module needs a new widget component + a registry entry here.
 */
export const WIDGET_REGISTRY: DashboardWidgetDef[] = [
  { id: 'total-leads', section: 'CRM', component: TotalLeadsWidget, requiredPermission: 'LEAD_VIEW' },
  { id: 'accounts', section: 'CRM', component: AccountsWidget, requiredPermission: 'CLIENT_VIEW' },
  { id: 'open-opportunities', section: 'CRM', component: OpenOpportunitiesWidget, requiredPermission: 'OPPORTUNITY_VIEW' },
  { id: 'weighted-forecast', section: 'CRM', component: WeightedForecastWidget, requiredPermission: 'OPPORTUNITY_VIEW' },

  { id: 'pending-requests', section: 'Servicedesk & Support', component: PendingRequestsWidget, requiredPermission: 'SERVICE_REQUEST_VIEW' },
  { id: 'in-progress-requests', section: 'Servicedesk & Support', component: InProgressRequestsWidget, requiredPermission: 'SERVICE_REQUEST_VIEW' },
  { id: 'sla-breached', section: 'Servicedesk & Support', component: SlaBreachedWidget, requiredPermission: 'SERVICE_REQUEST_VIEW' },
  { id: 'support-tickets', section: 'Servicedesk & Support', component: SupportTicketsWidget, requiredPermission: 'TICKET_VIEW' },

  { id: 'outstanding-invoices', section: 'Finance', component: OutstandingInvoicesWidget, requiredPermission: 'INVOICE_VIEW' },
  { id: 'wallet-balance', section: 'Finance', component: WalletBalanceWidget, requiredPermission: 'WALLET_VIEW' },
  { id: 'wallet-credits', section: 'Finance', component: WalletCreditsWidget, requiredPermission: 'WALLET_VIEW' },

  { id: 'total-employees', section: 'HRM', component: TotalEmployeesWidget, requiredPermission: 'EMPLOYEE_VIEW' },
  { id: 'pending-leave-approvals', section: 'HRM', component: PendingLeaveApprovalsWidget, requiredPermission: 'LEAVE_VIEW' },
  { id: 'payroll-status', section: 'HRM', component: PayrollStatusWidget, requiredPermission: 'PAYROLL_VIEW' },
];

export const DASHBOARD_SECTIONS: DashboardSection[] = ['CRM', 'Servicedesk & Support', 'Finance', 'HRM'];
