import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';
import { ClientHomeRedirectGuard } from './core/guards/client-home-redirect.guard';
import { DashboardAccessGuard } from './core/guards/dashboard-access.guard';

// Portal (public landing + per-company public portal + owner settings)

// Finance

// Support

// ITAM

// Attendance

export const routes: Routes = [
  { path: '', loadComponent: () => import('./modules/dashboard/components/dashboard.component').then(m => m.DashboardComponent), canActivate: [AuthGuard, ClientHomeRedirectGuard, DashboardAccessGuard] },
  { path: 'dashboard', redirectTo: '', pathMatch: 'full' },
  { path: 'my-profile', loadComponent: () => import('./shared/components/welcome/welcome').then(m => m.Welcome), canActivate: [AuthGuard] },
  { path: 'employee-dashboard', loadComponent: () => import('./modules/dashboard/components/employee-dashboard/employee-dashboard').then(m => m.EmployeeDashboard), canActivate: [AuthGuard] },
  { path: 'hrm/my-payslips', loadComponent: () => import('./modules/hrm/components/my-payslips/my-payslips').then(m => m.MyPayslips), canActivate: [AuthGuard] },
  // Public pages - no auth
  { path: 'home', loadComponent: () => import('./modules/landing/landing').then(m => m.Landing) },
  // Public careers page - candidates browsing a company's open positions
  { path: 'careers/:slug', loadComponent: () => import('./modules/careers/careers-page').then(m => m.CareersPage) },
  { path: 'contact', loadComponent: () => import('./modules/landing/components/contact-sales/contact-sales').then(m => m.ContactSales) },
  { path: 'portal/:subdomain', loadChildren: () => import('./modules/site/site.routes').then(m => m.SITE_ROUTES) },
  { path: 'payment-result', loadComponent: () => import('./modules/portal/payment-result/payment-result').then(m => m.PaymentResult) },

  {
    path: 'roles-permissions',
    loadComponent: () => import('./modules/roles-permissions/components/roles-permissions/roles-permissions').then(m => m.RolesPermissions),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['COMPANY_OWNER'] },
  },
  { path: 'auth/login', loadComponent: () => import('./modules/auth/login/login').then(m => m.Login) },
  { path: 'auth/register', loadComponent: () => import('./modules/auth/register/register').then(m => m.Register) },
  { path: 'auth/register-client', loadComponent: () => import('./modules/auth/client-register/client-register').then(m => m.ClientRegister) },
  { path: 'auth/forgot-password', loadComponent: () => import('./modules/auth/forgot-password/forgot-password').then(m => m.ForgotPassword) },
  { path: 'auth/reset-password', loadComponent: () => import('./modules/auth/reset-password/reset-password').then(m => m.ResetPassword) },
  { path: 'auth/verify-email', loadComponent: () => import('./modules/auth/verify-email/verify-email').then(m => m.VerifyEmail) },
  { path: 'auth', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'search', loadComponent: () => import('./modules/search/components/global-search/global-search').then(m => m.GlobalSearch), canActivate: [AuthGuard] },
  { path: 'ai', loadComponent: () => import('./modules/ai/components/ai-assistant/ai-assistant').then(m => m.AiAssistant), canActivate: [AuthGuard] },
  { path: 'ai/settings', loadComponent: () => import('./modules/ai/components/ai-settings/ai-settings').then(m => m.AiSettings), canActivate: [AuthGuard] },
  { path: 'notifications', loadComponent: () => import('./modules/notifications/notifications').then(m => m.Notifications), canActivate: [AuthGuard] },
  { path: 'profile', loadComponent: () => import('./modules/preferences/preferences').then(m => m.Preferences), canActivate: [AuthGuard] },
  { path: 'notifications/preferences', redirectTo: 'profile', pathMatch: 'full' },
  { path: 'settings/billing', redirectTo: 'finance/wallet', pathMatch: 'full' },

  // Account pages behind the Authentication nav group. No permission gate: these
  // act on the signed-in user's own credentials, so holding an account is the
  // only entitlement needed.
  {
    path: 'account/password',
    loadComponent: () => import('./modules/account/change-password/change-password').then(m => m.ChangePassword),
    canActivate: [AuthGuard],
  },
  {
    path: 'account/email',
    loadComponent: () => import('./modules/account/change-email/change-email').then(m => m.ChangeEmail),
    canActivate: [AuthGuard],
  },
  {
    path: 'users',
    loadComponent: () => import('./modules/account/users/users').then(m => m.Users),
    canActivate: [AuthGuard, RoleGuard],
    data: { requiredPermission: 'USER_VIEW' },
  },
  {
    path: 'crm',
    canActivate: [AuthGuard, RoleGuard],
    // canActivateChild (not just canActivate) so RoleGuard re-checks each individual
    // child's own requiredPermission - without it, moving between two children of an
    // already-active parent (e.g. /crm/leads -> /crm/pipeline) skips guard evaluation
    // entirely, since the parent node itself doesn't get reactivated on lateral moves.
    canActivateChild: [AuthGuard, RoleGuard],
    loadChildren: () => import('./modules/crm/crm.routes').then(m => m.CRM_ROUTES)
  },
  {
    path: 'client',
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['CLIENT'] },
    loadChildren: () => import('./modules/client-portal/client-portal.routes').then(m => m.CLIENT_PORTAL_ROUTES)
  },
  {
    path: 'servicedesk',
    canActivate: [AuthGuard, RoleGuard],
    canActivateChild: [AuthGuard, RoleGuard],
    loadChildren: () => import('./modules/servicedesk/servicedesk.routes').then(m => m.SERVICEDESK_ROUTES)
  },
  {
    path: 'hrm',
    canActivate: [AuthGuard, RoleGuard],
    canActivateChild: [AuthGuard, RoleGuard],
    loadChildren: () => import('./modules/hrm/hrm.routes').then(m => m.HRM_ROUTES)
  },
  {
    path: 'platform',
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['SUPER_ADMIN', 'SYSTEM_ADMIN', 'PLATFORM_ACCOUNTANT', 'SALES_MANAGER', 'SUPPORT_AGENT', 'SUPPORT_MANAGER'] },
    loadChildren: () => import('./modules/platform-admin/platform-admin.routes').then(m => m.PLATFORM_ADMIN_ROUTES)
  },
  {
    path: 'subscriptionPlan',
    canActivate: [AuthGuard],
    loadComponent: () => import('./modules/subscription/components/subscription-plan/subscription-plan').then(m => m.SubscriptionPlan)
  },
  {
    path: 'finance',
    canActivate: [AuthGuard, RoleGuard],
    canActivateChild: [AuthGuard, RoleGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./modules/finance/components/finance-dashboard/finance-dashboard').then(m => m.FinanceDashboard), data: { requiredPermission: 'FINANCIAL_REPORT_VIEW' } },
      { path: 'coa', loadComponent: () => import('./modules/finance/components/chart-of-accounts/chart-of-accounts').then(m => m.ChartOfAccounts), data: { requiredPermission: 'CHART_OF_ACCOUNT_VIEW' } },
      { path: 'expenses', loadComponent: () => import('./modules/finance/components/expenses/expenses').then(m => m.Expenses), data: { requiredPermission: 'EXPENSE_VIEW' } },
      { path: 'invoices', loadComponent: () => import('./modules/finance/components/invoices/invoices').then(m => m.Invoices), data: { requiredPermission: 'INVOICE_VIEW' } },
      { path: 'refunds', loadComponent: () => import('./modules/finance/components/refunds/refunds').then(m => m.Refunds), data: { requiredPermission: 'INVOICE_VIEW' } },
      { path: 'journal-entries', loadComponent: () => import('./modules/finance/components/journal-entries/journal-entries').then(m => m.JournalEntries), data: { requiredPermission: 'JOURNAL_ENTRY_VIEW' } },
      { path: 'reports', loadComponent: () => import('./modules/finance/components/reports/reports').then(m => m.Reports), data: { requiredPermission: 'FINANCIAL_REPORT_VIEW' } },
      { path: 'wallet', loadComponent: () => import('./modules/finance/components/wallet/wallet').then(m => m.WalletPage), data: { requiredPermission: 'WALLET_VIEW' } },
      { path: 'payment-receipts', loadComponent: () => import('./modules/finance/components/payment-receipts/payment-receipts').then(m => m.PaymentReceipts), data: { requiredPermission: 'PAYMENT_RECEIPT_VIEW' } },
      { path: 'general-ledger', loadComponent: () => import('./modules/finance/components/general-ledger/general-ledger').then(m => m.GeneralLedger), data: { requiredPermission: 'GENERAL_LEDGER_VIEW' } },
      { path: 'bank-reconciliation', loadComponent: () => import('./modules/finance/components/bank-reconciliation/bank-reconciliation').then(m => m.BankReconciliationPage), data: { requiredPermission: 'BANK_RECONCILIATION_VIEW' } },
      { path: 'company-settings', loadComponent: () => import('./modules/finance/components/company-settings/company-settings').then(m => m.CompanySettings), data: { requiredPermission: 'COMPANY_SETTINGS' } },
      { path: 'accounting-periods', loadComponent: () => import('./modules/finance/components/accounting-periods/accounting-periods').then(m => m.AccountingPeriods), data: { requiredPermission: 'ACCOUNTING_PERIOD_VIEW' } },
      { path: 'fiscal-years', loadComponent: () => import('./modules/finance/components/fiscal-years/fiscal-years').then(m => m.FiscalYears), data: { requiredPermission: 'ACCOUNTING_PERIOD_VIEW' } },
      { path: 'vendors', loadComponent: () => import('./modules/finance/components/vendors/vendors').then(m => m.Vendors), data: { requiredPermission: 'VENDOR_VIEW' } },
      { path: 'vendor-bills', loadComponent: () => import('./modules/finance/components/vendor-bills/vendor-bills').then(m => m.VendorBills), data: { requiredPermission: 'VENDOR_BILL_VIEW' } },
      { path: 'budgets', loadComponent: () => import('./modules/finance/components/budgets/budgets').then(m => m.Budgets), data: { requiredPermission: 'BUDGET_VIEW' } },
      { path: 'fixed-assets', loadComponent: () => import('./modules/finance/components/fixed-assets/fixed-assets').then(m => m.FixedAssets), data: { requiredPermission: 'FIXED_ASSET_VIEW' } },
      { path: '', redirectTo: 'invoices', pathMatch: 'full' }
    ]
  },
  {
    path: 'support',
    canActivate: [AuthGuard, RoleGuard],
    canActivateChild: [AuthGuard, RoleGuard],
    children: [
      { path: 'tickets', loadComponent: () => import('./modules/support/components/tickets/tickets').then(m => m.Tickets), data: { requiredPermission: 'TICKET_VIEW' } },
      { path: 'categories', loadComponent: () => import('./modules/support/components/categories/categories').then(m => m.Categories), data: { requiredPermission: 'SUPPORT_CATEGORY_VIEW' } },
      { path: 'messages', loadComponent: () => import('./modules/support/components/messages/messages').then(m => m.Messages), data: { requiredPermission: 'SUPPORT_MESSAGE_VIEW' } },
      { path: 'client-chat', loadComponent: () => import('./modules/support/components/client-chat/client-chat').then(m => m.ClientChat), data: { requiredPermission: 'SUPPORT_MESSAGE_VIEW' } },
      { path: 'sla-policies', loadComponent: () => import('./modules/support/components/sla-policies/sla-policies').then(m => m.SlaPolicies), data: { requiredPermission: 'SLA_POLICY_VIEW' } },
      { path: 'audit-logs', loadComponent: () => import('./modules/support/components/audit-logs/audit-logs').then(m => m.AuditLogs), data: { requiredPermission: 'AUDIT_LOG_VIEW' } },
      { path: 'agents', loadComponent: () => import('./modules/support/components/agents/agents').then(m => m.Agents), data: { roles: ['SUPER_ADMIN', 'SUPPORT_MANAGER'] } },
      { path: 'context-switches', loadComponent: () => import('./modules/support/components/context-switches/context-switches').then(m => m.ContextSwitches) },
      { path: '', redirectTo: 'tickets', pathMatch: 'full' }
    ]
  },
  {
    path: 'itam',
    canActivate: [AuthGuard, RoleGuard],
    canActivateChild: [AuthGuard, RoleGuard],
    children: [
      { path: 'hardware', loadComponent: () => import('./modules/itam/components/hardware/hardware').then(m => m.Hardware), data: { requiredPermission: 'HARDWARE_VIEW' } },
      { path: 'software', loadComponent: () => import('./modules/itam/components/software/software').then(m => m.Software), data: { requiredPermission: 'SOFTWARE_LICENSE_VIEW' } },
      { path: 'assignments', loadComponent: () => import('./modules/itam/components/assignments/assignments').then(m => m.Assignments), data: { requiredPermission: 'ASSET_ASSIGNMENT_VIEW' } },
      { path: 'offboarding', loadComponent: () => import('./modules/itam/components/offboarding/offboarding').then(m => m.Offboarding), data: { requiredPermission: 'OFFBOARDING_VIEW' } },
      { path: 'import', loadComponent: () => import('./modules/itam/components/asset-import/asset-import').then(m => m.AssetImport), data: { requiredPermission: 'ASSET_IMPORT_VIEW' } },
      { path: '', redirectTo: 'hardware', pathMatch: 'full' }
    ]
  },
  {
    path: 'attendance',
    canActivate: [AuthGuard, RoleGuard],
    canActivateChild: [AuthGuard, RoleGuard],
    children: [
      { path: 'check-in', loadComponent: () => import('./modules/attendance/components/check-in-out/check-in-out').then(m => m.CheckInOut), data: { requiredPermission: 'ATTENDANCE_MARK' } },
      { path: 'records', loadComponent: () => import('./modules/attendance/components/attendance-list/attendance-list').then(m => m.AttendanceList), data: { requiredPermission: 'ATTENDANCE_VIEW' } },
      { path: 'timesheets', loadComponent: () => import('./modules/attendance/components/timesheets/timesheets').then(m => m.Timesheets), data: { requiredPermission: 'TIMESHEET_VIEW' } },
      { path: 'shift-assignments', loadComponent: () => import('./modules/attendance/components/shift-assignments/shift-assignments').then(m => m.ShiftAssignments), data: { requiredPermission: 'SHIFT_ASSIGNMENT_VIEW' } },
      { path: 'biometric-data', loadComponent: () => import('./modules/attendance/components/biometric-data/biometric-data').then(m => m.BiometricDataPage), data: { requiredPermission: 'BIOMETRIC_VIEW' } },
      { path: 'reports', loadComponent: () => import('./modules/attendance/components/reports/reports').then(m => m.Reports), data: { requiredPermission: 'ATTENDANCE_VIEW' } },
      { path: '', redirectTo: 'check-in', pathMatch: 'full' }
    ]
  },
  { path: 'forbidden', loadComponent: () => import('./shared/components/forbidden/forbidden').then(m => m.Forbidden) },
  { path: 'server-error', loadComponent: () => import('./shared/components/server-error/server-error').then(m => m.ServerError) },
  { path: '**', loadComponent: () => import('./shared/components/not-found/not-found').then(m => m.NotFound) }
];