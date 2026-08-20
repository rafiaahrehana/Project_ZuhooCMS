import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { PermissionService } from '../../../core/services/permission.service';
import {
  DashboardService,
  DashboardSummary,
  PlatformSummary,
  PlatformMetricsPoint,
  ClientSummary,
  RecommendationResponse,
} from '../../../core/services/dashboard.service';
import { TicketService } from '../../support/services/ticket.service';
import { AgentService } from '../../support/services/agent.service';
import { SupportTicket, SupportAgent } from '../../support/models/support.model';
import { Loader } from '../../../shared/components/loader/loader';
import { StatCard } from '../../../shared/components/stat-card/stat-card';
import { WIDGET_REGISTRY, DASHBOARD_SECTIONS, DashboardWidgetDef, DashboardSection } from '../widget-registry';

interface QuickLink {
  label: string;
  description: string;
  icon: string;
  link: string;
}

// The dashboard is role-aware. Backend endpoints and their @PreAuthorize roles:
//   /api/dashboard/summary + /recommendations + /insights -> COMPANY_OWNER, EMPLOYEE
//   /api/dashboard/platform-summary -> all 7 platform staff roles
//   /api/dashboard/client-summary   -> CLIENT
const COMPANY_ROLES = ['COMPANY_OWNER', 'EMPLOYEE'];
// Roles allowed into /platform routes (must match RoleGuard data in app.routes.ts)
const PLATFORM_ADMIN_ROLES = ['SUPER_ADMIN', 'SYSTEM_ADMIN', 'PLATFORM_ACCOUNTANT', 'SALES_MANAGER'];

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, FormsModule, Loader, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  readonly dashboardSections = DASHBOARD_SECTIONS;
  summary?: DashboardSummary;
  platformSummary?: PlatformSummary;
  platformMetricsHistory: PlatformMetricsPoint[] = [];
  clientSummary?: ClientSummary;
  recommendations: RecommendationResponse[] = [];
  insights = '';
  insightsError = '';
  insightsLoading = false;
  loading = false;
  error = '';

  // Support Shared State
  supportTickets: SupportTicket[] = [];
  slaBreachedTickets: SupportTicket[] = [];
  criticalTickets: SupportTicket[] = [];

  // Support Agent Specific State
  isSupportAgentRole = false;
  myAgentRecord?: SupportAgent;
  myAssignedTickets: SupportTicket[] = [];
  displayAgentTickets: SupportTicket[] = [];
  unassignedTicketsQueue: SupportTicket[] = [];
  agentAssignedCount = 0;
  agentOpenCount = 0;
  agentResolvedCount = 0;
  agentSlaRiskCount = 0;
  supportTotalCount = 0;
  supportOpenCount = 0;
  supportResolvedCount = 0;
  supportSlaBreachedCount = 0;
  updatingTicketId: number | null = null;
  resolvingTicketTarget: SupportTicket | null = null;
  resolutionNotesInput = '';

  // Support Manager Specific State
  isSupportManagerRole = false;
  teamAgents: SupportAgent[] = [];
  managerAllTickets: SupportTicket[] = [];
  managerTotalCount = 0;
  managerUnassignedCount = 0;
  managerSlaBreachedCount = 0;
  managerActiveAgentsCount = 0;

  // Role flags resolved once from the logged-in user
  isCompanyRole = false;
  isPlatformRole = false;
  isClientRole = false;
  canManagePlatform = false;

  subtitle = '';
  quickLinks: QuickLink[] = [];
  selectedRange: 'day' | 'week' | 'month' | 'year' = 'month';

  constructor(
    public auth: AuthService,
    private dashboardService: DashboardService,
    public permissionService: PermissionService,
    private ticketService: TicketService,
    private agentService: AgentService,
    private cdr: ChangeDetectorRef,
  ) {}

  /** Widgets the current user's permission set allows, for the given dashboard section. */
  widgetsForSection(section: DashboardSection): DashboardWidgetDef[] {
    return WIDGET_REGISTRY.filter(
      w => w.section === section && this.permissionService.hasPermission(w.requiredPermission),
    );
  }

  /** Inputs passed to each widget via NgComponentOutlet. */
  widgetInputs(): Record<string, unknown> {
    return { summary: this.summary };
  }

  ngOnInit(): void {
    const processUser = (user: any) => {
      const roles = user?.roles ?? [];
      if (!roles.length) return;

      this.isCompanyRole = roles.some((r: string) => COMPANY_ROLES.includes(r));
      this.isClientRole = roles.includes('CLIENT');
      this.isSupportAgentRole = roles.includes('SUPPORT_AGENT') && !roles.includes('SUPPORT_MANAGER') && !roles.includes('SUPER_ADMIN');
      this.isSupportManagerRole = roles.includes('SUPPORT_MANAGER') || (roles.includes('SUPPORT_AGENT') && (roles.includes('SUPER_ADMIN') || roles.includes('SUPPORT_MANAGER')));
      this.isPlatformRole = !this.isCompanyRole && !this.isClientRole && !this.isSupportAgentRole && !this.isSupportManagerRole && roles.length > 0;
      this.canManagePlatform = roles.some((r: string) => PLATFORM_ADMIN_ROLES.includes(r));

      if (this.isSupportAgentRole) {
        this.subtitle = 'Personal Support Workstation & Assigned Queue';
        this.quickLinks = this.buildSupportAgentQuickLinks();
        this.loadSupportAgentDashboard();
      } else if (this.isSupportManagerRole) {
        this.subtitle = 'Support Team Operations, SLA Compliance & Team Management';
        this.quickLinks = this.buildSupportManagerQuickLinks();
        this.loadSupportManagerDashboard();
      } else if (this.isCompanyRole) {
        this.subtitle = 'Live overview of your company';
        this.quickLinks = this.buildCompanyQuickLinks(roles);
        this.loadCompanyDashboard();
      } else if (this.isPlatformRole) {
        this.subtitle = "Here's what's happening with your platform today.";
        this.quickLinks = this.buildPlatformQuickLinks(roles);
        this.loadPlatformDashboard();
      } else if (this.isClientRole) {
        this.subtitle = 'Welcome back';
        this.quickLinks = this.buildClientQuickLinks();
        this.loadClientDashboard();
      }
      this.cdr.markForCheck();
    };

    // Immediate check if user exists in storage/subject
    const current = this.auth.getCurrentUser();
    if (current) processUser(current);

    // Reactive subscription for state changes
    this.auth.currentUser$.subscribe(user => {
      if (user) processUser(user);
    });
  }

  // ---------- COMPANY_OWNER / EMPLOYEE ----------

  private loadCompanyDashboard(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    const range = this.getDateRange();
    this.dashboardService.getSummary(range?.from, range?.to).subscribe({
      next: (s) => {
        this.summary = s;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load dashboard summary';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
    this.dashboardService.getRecommendations().subscribe({
      next: (recs) => {
        this.recommendations = recs;
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  generateInsights(): void {
    this.insightsLoading = true;
    this.insightsError = '';
    this.cdr.markForCheck();
    this.dashboardService.getInsights().subscribe({
      next: (res) => {
        this.insights = res.insights;
        this.insightsLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.insightsError =
          err?.error?.message || 'Insights failed - check your AI provider config';
        this.insightsLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ---------- PLATFORM STAFF ----------

  private loadPlatformDashboard(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.dashboardService.getPlatformSummary().subscribe({
      next: (s) => {
        this.platformSummary = s;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load platform summary';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
    this.dashboardService.getPlatformMetricsHistory(this.platformHistoryDays()).subscribe({
      next: (h) => { this.platformMetricsHistory = h; this.cdr.markForCheck(); },
      error: () => { this.platformMetricsHistory = []; this.cdr.markForCheck(); },
    });
  }

  // ---------- SUPPORT AGENT ----------

  private loadSupportAgentDashboard(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    // 1. Fetch all platform company tickets for general workstation list
    this.ticketService.list(0, 50).subscribe({
      next: (res) => {
        const all = res.content || [];
        this.supportTickets = all;
        this.unassignedTicketsQueue = all.filter(t => !t.assignedToAgentId && t.status !== 'RESOLVED' && t.status !== 'CLOSED');

        this.supportTotalCount = res.totalElements || all.length;
        this.supportOpenCount = all.filter(t => t.status === 'NEW' || t.status === 'OPEN' || t.status === 'IN_PROGRESS').length;
        this.supportResolvedCount = all.filter(t => t.status === 'RESOLVED' || t.status === 'CLOSED').length;
        this.supportSlaBreachedCount = all.filter(t => t.slaBreached).length;

        // Display all tickets if myAssignedTickets has not populated or is empty
        if (this.myAssignedTickets.length === 0) {
          this.displayAgentTickets = all;
        }

        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load tickets';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });

    // 2. Fetch agent record and tickets explicitly assigned to logged-in agent
    const userId = this.auth.getCurrentUser()?.id;
    if (userId) {
      this.agentService.getByUserId(userId).subscribe({
        next: (agent) => {
          this.myAgentRecord = agent;
          if (agent?.id) {
            this.ticketService.assignedToMe(agent.id, 0, 50).subscribe({
              next: (res) => {
                this.myAssignedTickets = res.content || [];
                this.agentAssignedCount = res.totalElements || this.myAssignedTickets.length;
                this.agentOpenCount = this.myAssignedTickets.filter(t => t.status === 'NEW' || t.status === 'OPEN' || t.status === 'IN_PROGRESS').length;
                this.agentResolvedCount = this.myAssignedTickets.filter(t => t.status === 'RESOLVED' || t.status === 'CLOSED').length;
                this.agentSlaRiskCount = this.myAssignedTickets.filter(t => t.slaBreached).length;

                this.displayAgentTickets = this.myAssignedTickets.length > 0 ? this.myAssignedTickets : this.supportTickets;
                this.cdr.markForCheck();
              },
              error: () => {}
            });
          }
        },
        error: () => {}
      });
    }
  }

  claimTicket(ticket: SupportTicket): void {
    if (!this.myAgentRecord?.id) {
      const userId = this.auth.getCurrentUser()?.id;
      if (userId) {
        this.agentService.getByUserId(userId).subscribe({
          next: (agent) => {
            this.myAgentRecord = agent;
            if (agent?.id) this.doAssignTicket(ticket.id, agent.id);
          },
          error: () => {}
        });
      }
      return;
    }
    this.doAssignTicket(ticket.id, this.myAgentRecord.id);
  }

  private doAssignTicket(ticketId: number, agentId: number): void {
    this.updatingTicketId = ticketId;
    this.cdr.markForCheck();
    this.ticketService.assign(ticketId, agentId).subscribe({
      next: () => {
        this.updatingTicketId = null;
        if (this.isSupportAgentRole) this.loadSupportAgentDashboard();
        if (this.isSupportManagerRole) this.loadSupportManagerDashboard();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to assign ticket';
        this.updatingTicketId = null;
        this.cdr.markForCheck();
      }
    });
  }

  updateTicketStatus(ticket: SupportTicket, newStatus: string): void {
    if (newStatus === 'RESOLVED') {
      this.openResolveModal(ticket);
      return;
    }
    this.updatingTicketId = ticket.id;
    this.cdr.markForCheck();
    this.ticketService.update(ticket.id, { status: newStatus }).subscribe({
      next: () => {
        this.updatingTicketId = null;
        if (this.isSupportAgentRole) this.loadSupportAgentDashboard();
        if (this.isSupportManagerRole) this.loadSupportManagerDashboard();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update ticket status';
        this.updatingTicketId = null;
        this.cdr.markForCheck();
      }
    });
  }

  openResolveModal(ticket: SupportTicket): void {
    this.resolvingTicketTarget = ticket;
    this.resolutionNotesInput = '';
    this.cdr.markForCheck();
  }

  submitTicketResolution(): void {
    if (!this.resolvingTicketTarget || !this.resolutionNotesInput.trim()) return;
    const ticketId = this.resolvingTicketTarget.id;
    this.updatingTicketId = ticketId;
    this.cdr.markForCheck();
    this.ticketService.resolve(ticketId, this.resolutionNotesInput.trim()).subscribe({
      next: () => {
        this.resolvingTicketTarget = null;
        this.resolutionNotesInput = '';
        this.updatingTicketId = null;
        if (this.isSupportAgentRole) this.loadSupportAgentDashboard();
        if (this.isSupportManagerRole) this.loadSupportManagerDashboard();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to resolve ticket';
        this.updatingTicketId = null;
        this.cdr.markForCheck();
      }
    });
  }

  private buildSupportAgentQuickLinks(): QuickLink[] {
    return [
      { label: 'My Tickets', description: 'Assigned ticket queue', icon: 'bi-ticket-perforated', link: '/support/tickets' },
      { label: 'Direct Messages', description: 'Chat with client users', icon: 'bi-envelope', link: '/support/messages' },
      { label: 'SLA Policies', description: 'Response time SLAs', icon: 'bi-stopwatch', link: '/support/sla-policies' },
      { label: 'Notifications', description: 'System alerts', icon: 'bi-bell', link: '/notifications' },
      { label: 'My Profile', description: 'Agent profile & settings', icon: 'bi-person', link: '/profile' },
    ];
  }

  // ---------- SUPPORT MANAGER ----------

  private loadSupportManagerDashboard(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    this.ticketService.list(0, 50).subscribe({
      next: (res) => {
        this.managerAllTickets = res.content || [];
        this.managerTotalCount = res.totalElements || this.managerAllTickets.length;
        this.managerUnassignedCount = this.managerAllTickets.filter(t => !t.assignedToAgentId && t.status !== 'RESOLVED' && t.status !== 'CLOSED').length;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load manager dashboard';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });

    this.ticketService.slaBreached().subscribe({
      next: (res) => {
        this.slaBreachedTickets = res || [];
        this.managerSlaBreachedCount = this.slaBreachedTickets.length;
        this.cdr.markForCheck();
      },
      error: () => {}
    });

    this.ticketService.criticalOpen().subscribe({
      next: (res) => {
        this.criticalTickets = res || [];
        this.cdr.markForCheck();
      },
      error: () => {}
    });

    this.agentService.available().subscribe({
      next: (agents) => {
        this.teamAgents = agents || [];
        this.managerActiveAgentsCount = this.teamAgents.filter(a => a.status === 'AVAILABLE' || a.acceptingTickets).length;
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  private buildSupportManagerQuickLinks(): QuickLink[] {
    return [
      { label: 'All Tickets', description: 'Manage platform tickets', icon: 'bi-ticket-perforated', link: '/support/tickets' },
      { label: 'Support Team', description: 'Agent roster & workload', icon: 'bi-headset', link: '/support/agents' },
      { label: 'Direct Messages', description: 'Client messaging', icon: 'bi-envelope', link: '/support/messages' },
      { label: 'SLA Policies', description: 'Manage SLA rules', icon: 'bi-stopwatch', link: '/support/sla-policies' },
      { label: 'Audit Logs', description: 'System activity logs', icon: 'bi-journal-text', link: '/support/audit-logs' },
    ];
  }

  private platformHistoryDays(): number {
    switch (this.selectedRange) {
      case 'day': return 2;
      case 'week': return 7;
      case 'month': return 30;
      case 'year': return 365;
    }
  }

  private buildPlatformQuickLinks(roles: string[]): QuickLink[] {
    const links: QuickLink[] = [];
    if (this.canManagePlatform) {
      links.push(
        { label: 'Companies', description: 'Manage tenant companies', icon: 'bi-buildings', link: '/platform/companies' },
        { label: 'Subscriptions', description: 'Plan catalog & pricing', icon: 'bi-credit-card', link: '/platform/subscription-management' },
        { label: 'Platform Users', description: 'SaaS staff accounts', icon: 'bi-person-badge', link: '/platform/platform-users' },
        { label: 'Feature Flags', description: 'Toggle platform features', icon: 'bi-toggles', link: '/platform/feature-flags' },
        { label: 'AI Settings', description: 'AI models & credentials', icon: 'bi-robot', link: '/ai/settings' },
      );
    }
    if (this.canManagePlatform || roles.includes('SUPPORT_AGENT') || roles.includes('SUPPORT_MANAGER')) {
      links.push(
        { label: 'Support Agents', description: 'Platform support team', icon: 'bi-headset', link: '/support/agents' },
        { label: 'Support Tickets', description: 'Resolve tenant tickets', icon: 'bi-ticket-perforated', link: '/support/tickets' },
        { label: 'SLA Policies', description: 'Response time targets', icon: 'bi-stopwatch', link: '/support/sla-policies' },
      );
    }
    links.push(
      { label: 'Notifications', description: 'Your latest updates', icon: 'bi-bell', link: '/notifications' },
      { label: 'Settings', description: 'Profile & preferences', icon: 'bi-gear', link: '/profile' },
    );
    return links;
  }

  private buildCompanyQuickLinks(roles: string[]): QuickLink[] {
    const links: QuickLink[] = [];
    if (this.permissionService.hasPermission('EMPLOYEE_VIEW')) {
      links.push({ label: 'Employees', description: 'Manage company staff', icon: 'bi-people', link: '/hrm/employees' });
    }
    if (this.permissionService.hasPermission('ATTENDANCE_VIEW')) {
      links.push({ label: 'Attendance', description: 'Timesheets & records', icon: 'bi-clock-history', link: '/attendance/records' });
    }
    if (this.permissionService.hasPermission('INVOICE_VIEW')) {
      links.push({ label: 'Invoices', description: 'Billing & revenue', icon: 'bi-file-earmark-text', link: '/finance/invoices' });
    }
    if (this.permissionService.hasPermission('SERVICE_REQUEST_VIEW')) {
      links.push({ label: 'Company Services', description: 'Client requests & tickets', icon: 'bi-headset', link: '/servicedesk/requests' });
    }
    if (this.permissionService.hasPermission('CLIENT_VIEW') || this.permissionService.hasPermission('LEAD_VIEW')) {
      links.push({ label: 'CRM & Clients', description: 'Leads & accounts', icon: 'bi-person-lines-fill', link: '/crm/leads' });
    }
    if (this.permissionService.hasPermission('HARDWARE_VIEW')) {
      links.push({ label: 'IT Assets', description: 'Hardware & software', icon: 'bi-laptop', link: '/itam/hardware' });
    }
    if (roles.includes('COMPANY_OWNER')) {
      links.push({ label: 'Roles & Permissions', description: 'Access controls', icon: 'bi-shield-lock', link: '/roles-permissions' });
    }
    links.push(
      { label: 'Notifications', description: 'Your latest updates', icon: 'bi-bell', link: '/notifications' },
      { label: 'Settings', description: 'Profile & preferences', icon: 'bi-gear', link: '/profile' }
    );
    return links;
  }

  // ---------- CLIENT ----------

  private loadClientDashboard(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.dashboardService.getClientSummary().subscribe({
      next: (s) => {
        this.clientSummary = s;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load client summary';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private buildClientQuickLinks(): QuickLink[] {
    return [
      { label: 'My Service Requests', description: 'Track your requests', icon: 'bi-clipboard-check', link: '/client/requests' },
      { label: 'Browse Services', description: 'Explore available services', icon: 'bi-grid', link: '/client/categories' },
      { label: 'Packages', description: 'Service plans & packages', icon: 'bi-box-seam', link: '/client/packages' },
      { label: 'Payment History', description: 'Invoices & receipts', icon: 'bi-credit-card', link: '/client/payments' },
      { label: 'Notifications', description: 'Your latest updates', icon: 'bi-bell', link: '/notifications' },
      { label: 'Settings', description: 'Profile & preferences', icon: 'bi-gear', link: '/client/profile' },
    ];
  }

  // ---------- shared ----------

  fmt(n: number | undefined): string {
    if (n == null) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'BDT',
      currencyDisplay: 'code',
      maximumFractionDigits: 0,
    }).format(n);
  }

  setRange(range: 'day' | 'week' | 'month' | 'year'): void {
    this.selectedRange = range;
    if (this.isCompanyRole) {
      this.loadCompanyDashboard();
    } else if (this.isPlatformRole) {
      this.loadPlatformDashboard();
    }
  }

  private getDateRange(): { from: string; to: string } | undefined {
    const now = new Date();
    const to = now.toISOString().split('T')[0];
    let from: string;
    switch (this.selectedRange) {
      case 'day':
        from = to;
        break;
      case 'week': {
        const d = new Date(now);
        d.setDate(d.getDate() - 7);
        from = d.toISOString().split('T')[0];
        break;
      }
      case 'month': {
        const d = new Date(now);
        d.setMonth(d.getMonth() - 1);
        from = d.toISOString().split('T')[0];
        break;
      }
      case 'year': {
        const d = new Date(now);
        d.setFullYear(d.getFullYear() - 1);
        from = d.toISOString().split('T')[0];
        break;
      }
    }
    return { from: from!, to };
  }

  get timeGreeting(): string {
    const hr = new Date().getHours();
    if (hr < 5) return 'Good Night';
    if (hr < 12) return 'Good Morning';
    if (hr < 17) return 'Good Afternoon';
    if (hr < 21) return 'Good Evening';
    return 'Good Night';
  }

  get userFirstName(): string {
    const user = this.auth.getCurrentUser();
    if (!user) return 'User';
    if (user.fullName) {
      return user.fullName.split(' ')[0];
    }
    return 'User';
  }

  get currentRangeLabel(): string {
    const range = this.getDateRange();
    if (!range) return '';
    const fromDate = new Date(range.from);
    const toDate = new Date(range.to);
    const format = (d: Date) => d.toLocaleString('en-US', { month: 'short', day: 'numeric' });
    const formatYear = (d: Date) => d.toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    return `${format(fromDate)} - ${formatYear(toDate)}`;
  }

  get rangeLabel(): string {
    switch (this.selectedRange) {
      case 'day': return 'Today';
      case 'week': return 'This Week';
      case 'month': return 'This Month';
      case 'year': return 'This Year';
    }
  }

  getYCoordinate(val: number): number {
    const maxVal = 300;
    return 180 - ((val / maxVal) * 150);
  }

  getLinePath(data: number[] | undefined): string {
    if (!data || data.length === 0) return '';
    return data.map((val, idx) => {
      const x = idx * 75 + 25;
      const y = this.getYCoordinate(val);
      return `${idx === 0 ? 'M' : 'L'} ${x} ${y}`;
    }).join(' ');
  }

  getAreaPath(data: number[] | undefined): string {
    if (!data || data.length === 0) return '';
    const linePath = this.getLinePath(data);
    const startX = 25;
    const endX = (data.length - 1) * 75 + 25;
    return `${linePath} L ${endX} 180 L ${startX} 180 Z`;
  }

  // ---------- PLATFORM: KPI sparklines (auto-scaled to each series' own min/max,
  // unlike getLinePath/getAreaPath above which assume a fixed 0-300 range sized
  // for the company Sales Overview chart) ----------

  private sparklineCoords(data: number[], width: number, height: number, padding: number): { x: number; y: number }[] {
    if (!data || data.length === 0) return [];
    const max = Math.max(...data, 1);
    const min = Math.min(...data, 0);
    const range = max - min || 1;
    const stepX = data.length > 1 ? (width - padding * 2) / (data.length - 1) : 0;
    return data.map((v, i) => ({
      x: padding + i * stepX,
      y: height - padding - ((v - min) / range) * (height - padding * 2),
    }));
  }

  sparklinePath(data: number[], width = 100, height = 32, padding = 3): string {
    const pts = this.sparklineCoords(data, width, height, padding);
    if (!pts.length) return '';
    return pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
  }

  sparklineArea(data: number[], width = 100, height = 32, padding = 3): string {
    const line = this.sparklinePath(data, width, height, padding);
    if (!line) return '';
    return `${line} L ${(width - padding).toFixed(1)} ${(height - padding).toFixed(1)} L ${padding} ${(height - padding).toFixed(1)} Z`;
  }

  companyCountSeries(field: 'totalCompanies' | 'activeCompanies' | 'trialCompanies' | 'suspendedCompanies'): number[] {
    return this.platformMetricsHistory.map(p => p[field]);
  }

  // ---------- PLATFORM: revenue trend chart (single series, auto-scaled) ----------

  get revenueSeries(): number[] {
    return this.platformMetricsHistory.map(p => Number(p.revenue) || 0);
  }

  get revenueChartMax(): number {
    const max = Math.max(...this.revenueSeries, 0);
    return max > 0 ? Math.ceil(max * 1.2) : 100;
  }

  revenueYLabels(): number[] {
    const max = this.revenueChartMax;
    return [max, max * 0.75, max * 0.5, max * 0.25, 0];
  }

  revenueLinePath(): string {
    return this.chartPath(this.revenueSeries, this.revenueChartMax, 520, 160, 25);
  }

  revenueAreaPath(): string {
    const line = this.revenueLinePath();
    if (!line) return '';
    const width = 520, padding = 25, height = 160;
    return `${line} L ${width - padding} ${height} L ${padding} ${height} Z`;
  }

  private chartPath(data: number[], max: number, width: number, height: number, padding: number): string {
    if (!data || data.length === 0) return '';
    const range = max || 1;
    const stepX = data.length > 1 ? (width - padding * 2) / (data.length - 1) : 0;
    return data.map((v, i) => {
      const x = padding + i * stepX;
      const y = height - (v / range) * height;
      return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
    }).join(' ');
  }

  /** A handful of evenly-spaced date labels along the x-axis (not one per day - a
   * 30-90 point series would collide badly), formatted like "Jun 18". */
  revenueXLabels(): { x: number; label: string }[] {
    const points = this.platformMetricsHistory;
    if (points.length === 0) return [];
    const width = 520, padding = 25;
    const stepX = points.length > 1 ? (width - padding * 2) / (points.length - 1) : 0;
    const maxLabels = 7;
    const labelEvery = Math.max(1, Math.ceil(points.length / maxLabels));
    const labels: { x: number; label: string }[] = [];
    for (let i = 0; i < points.length; i += labelEvery) {
      const d = new Date(points[i].date);
      labels.push({ x: padding + i * stepX, label: d.toLocaleString('en-US', { month: 'short', day: 'numeric' }) });
    }
    return labels;
  }

  get platformOverviewRows(): { icon: string; label: string; value: number; pct: number | null; bg: string; fg: string }[] {
    const s = this.platformSummary;
    if (!s) return [];
    const total = s.totalCompanies || 0;
    const pct = (n: number) => total > 0 ? Math.round((n / total) * 10000) / 100 : 0;
    return [
      { icon: 'bi-buildings', label: 'Total Companies', value: s.totalCompanies, pct: total > 0 ? 100 : 0, bg: '#ede9fe', fg: '#8b5cf6' },
      { icon: 'bi-check-circle', label: 'Active Companies', value: s.activeCompanies, pct: pct(s.activeCompanies), bg: '#d1fae5', fg: '#059669' },
      { icon: 'bi-hourglass-split', label: 'On Trial', value: s.trialCompanies, pct: pct(s.trialCompanies), bg: '#dbeafe', fg: '#0284c7' },
      { icon: 'bi-pause-circle', label: 'Suspended', value: s.suspendedCompanies, pct: pct(s.suspendedCompanies), bg: '#fef3c7', fg: '#b45309' },
      { icon: 'bi-clock-history', label: 'Pending Verification', value: s.pendingVerificationCompanies, pct: pct(s.pendingVerificationCompanies), bg: '#f1f5f9', fg: '#475569' },
      { icon: 'bi-people', label: 'Total Platform Users', value: s.totalPlatformUsers, pct: null, bg: '#e0e7ff', fg: '#4338ca' },
    ];
  }

  getDonutSegment(index: number): { stroke: string; dashArray: string; dashOffset: number } {
    const s = this.summary;
    if (!s) return { stroke: '#e2e8f0', dashArray: '0 377', dashOffset: 0 };
    
    const pending = s.serviceDeskPendingCount || 0;
    const inProgress = s.serviceDeskInProgressCount || 0;
    const resolved = s.serviceDeskResolvedCount || 0;
    const onHold = s.serviceDeskOnHoldCount || 0;
    
    const total = pending + inProgress + resolved + onHold;
    if (total === 0) {
      if (index === 0) return { stroke: '#e2e8f0', dashArray: '377 377', dashOffset: 0 };
      return { stroke: '#e2e8f0', dashArray: '0 377', dashOffset: 0 };
    }
    
    const values = [pending, inProgress, resolved, onHold];
    const colors = ['#f59e0b', '#0284c7', '#10b981', '#6b7280'];
    
    const c = 377;
    let accumulatedPercent = 0;
    
    for (let i = 0; i < index; i++) {
      accumulatedPercent += (values[i] / total) * 100;
    }
    
    const percent = (values[index] / total) * 100;
    const dashArray = `${(percent * c) / 100} ${c}`;
    const dashOffset = -((accumulatedPercent * c) / 100);
    
    return {
      stroke: colors[index],
      dashArray,
      dashOffset
    };
  }

  formatCurrency(val: number | undefined): string {
    if (val == null) return 'BDT 0';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'BDT',
      currencyDisplay: 'code',
      maximumFractionDigits: 0,
    }).format(val);
  }
}
