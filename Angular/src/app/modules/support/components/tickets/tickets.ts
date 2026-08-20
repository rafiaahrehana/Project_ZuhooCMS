import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SupportTicket, SupportAgent } from '../../models/support.model';
import { TicketService } from '../../services/ticket.service';
import { AgentService } from '../../services/agent.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { AuthService } from '../../../../core/services/auth.service';
import { ApiService } from '../../../../core/services/api.service';

@Component({
  selector: 'app-tickets',
  imports: [CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tickets.html',
})
export class Tickets implements OnInit {
  tickets: SupportTicket[] = [];
  agents: SupportAgent[] = [];
  categories: any[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  showSlaOnly = false;
  showCriticalOnly = false;

  // Backend role split: list/critical/escalate/reassign are SUPPORT_MANAGER,
  // /my-tickets is EMPLOYEE (creator view + satisfaction), /assigned-to-me is
  // the agent's queue (resolved via their SupportAgent record).
  isEmployee = false;
  isManager = false;
  isAgent = false;
  // Platform staff (Super/System Admin, Support Agent/Manager) triage tickets raised
  // by companies - they can never create one themselves (backend's create() endpoint
  // is COMPANY_OWNER/EMPLOYEE only), so "New Ticket" must not show for them.
  isPlatformStaff = false;
  // Platform admins should have manager-equivalent authority for escalation/reassignment
  // (the backend's /escalate and /reassign already allow them) - kept distinct from
  // isManager so plain SUPPORT_AGENT stays excluded from those actions, unchanged.
  isPlatformAdmin = false;
  myAgentId: number | null = null;

  // Escalate / reassign / satisfaction state
  escalateReason = '';
  reassignAgentId?: number;
  reassignReason = '';
  satisfactionRating = 5;
  satisfactionFeedback = '';

  selected?: SupportTicket;
  assignAgentId?: number;
  resolutionNotes = '';
  reopenReason = '';
  showCreate = false;
  form: any = { priority: 'MEDIUM' };

  statuses = ['NEW', 'OPEN', 'IN_PROGRESS', 'WAITING', 'ON_HOLD', 'RESOLVED', 'CLOSED'];

  constructor(
    private ticketService: TicketService,
    private agentService: AgentService,
    private auth: AuthService,
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const roles = this.auth.getCurrentUser()?.roles ?? [];
    this.isManager = roles.includes('SUPPORT_MANAGER');
    this.isAgent = roles.includes('SUPPORT_AGENT');
    this.isEmployee = roles.includes('EMPLOYEE') && !this.isManager && !this.isAgent;
    this.isPlatformAdmin = roles.includes('SUPER_ADMIN') || roles.includes('SYSTEM_ADMIN');
    this.isPlatformStaff = this.isManager || this.isAgent || this.isPlatformAdmin;
    if (this.isAgent) {
      // Resolve the agent record for /assigned-to-me
      const userId = this.auth.getCurrentUser()?.id;
      if (userId) {
        this.agentService.getByUserId(userId).subscribe({
          next: (a) => { this.myAgentId = a.id; this.load(); },
          error: () => this.load(),
        });
      }
    } else {
      this.load();
    }
    if (this.isPlatformStaff) {
      this.agentService.available().subscribe({ next: (r) => { this.agents = r; this.cdr.markForCheck(); } });
    }
    this.api.get<any>('/support/categories').subscribe({
      next: (res) => {
        this.categories = res.content || res || [];
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    if (this.showCriticalOnly && this.isManager) {
      // Bare list, not a page
      this.ticketService.criticalOpen().subscribe({
        next: (res) => { this.tickets = res; this.totalPages = 1; this.loading = false; this.cdr.markForCheck(); },
        error: (err) => this.onLoadError(err),
      });
      return;
    }
    if (this.isEmployee) {
      this.ticketService.myTickets(undefined, this.page).subscribe({
        next: (res) => { this.tickets = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
        error: (err) => this.onLoadError(err),
      });
      return;
    }
    if (this.isAgent && !this.isManager) {
      if (this.myAgentId != null) {
        this.ticketService.assignedToMe(this.myAgentId, this.page).subscribe({
          next: (res) => { this.tickets = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
          error: (err) => this.onLoadError(err),
        });
        return;
      }
    }
    if (this.showSlaOnly) {
      // Backend returns a plain list for SLA-breached tickets, not a page
      this.ticketService.slaBreached().subscribe({
        next: (res) => {
          this.tickets = res;
          this.totalPages = 1;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err) => this.onLoadError(err),
      });
      return;
    }
    const obs = this.statusFilter
      ? this.ticketService.listByStatus(this.statusFilter, this.page)
      : this.ticketService.list(this.page);
    obs.subscribe({
      next: (res) => {
        this.tickets = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => this.onLoadError(err),
    });
  }

  private onLoadError(err: HttpErrorResponse): void {
    this.tickets = [];
    this.totalPages = 0;
    this.loading = false;
    this.error =
      err.status === 403
        ? "You don't have permission to view these tickets."
        : err.status === 0
          ? 'Could not reach the server. Check your connection and try again.'
          : err.error?.message || 'Failed to load tickets.';
    this.cdr.markForCheck();
  }

  view(t: SupportTicket): void {
    this.ticketService.getById(t.id).subscribe({
      next: (ticket) => {
        this.selected = ticket;
        this.resolutionNotes = '';
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.showCreate = !this.showCreate;
    this.form = { priority: 'MEDIUM', title: '', description: '', categoryId: undefined };
    this.cdr.markForCheck();
  }

  create(): void {
    if (!this.form.title?.trim() || !this.form.description?.trim()) {
      this.error = 'Title and Description are required';
      this.cdr.markForCheck();
      return;
    }

    const payload: any = {
      title: this.form.title.trim(),
      description: this.form.description.trim(),
      priority: this.form.priority || 'MEDIUM',
    };

    if (this.form.categoryId) {
      payload.categoryId = Number(this.form.categoryId);
    }

    this.error = '';
    this.ticketService.create(payload).subscribe({
      next: () => {
        this.showCreate = false;
        this.form = { priority: 'MEDIUM' };
        this.success = 'Ticket created successfully';
        this.error = '';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || (Array.isArray(err?.error?.errors) ? err.error.errors.join(', ') : 'Failed to create ticket');
        this.cdr.markForCheck();
      },
    });
  }

  // The backend endpoints for assign/resolve/close/reopen return no body (ResponseEntity<Void>),
  // so we refresh the selected ticket from the server after each action.
  private refreshSelected(): void {
    if (!this.selected) return;
    this.ticketService.getById(this.selected.id).subscribe({ next: (t) => { this.selected = t; this.cdr.markForCheck(); } });
  }

  assign(): void {
    if (!this.selected || !this.assignAgentId) return;
    this.ticketService.assign(this.selected.id, this.assignAgentId).subscribe({
      next: () => {
        this.success = 'Assigned';
        this.cdr.markForCheck();
        this.refreshSelected();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  resolve(): void {
    if (!this.selected) return;
    this.ticketService.resolve(this.selected.id, this.resolutionNotes).subscribe({
      next: () => {
        this.success = 'Resolved';
        this.cdr.markForCheck();
        this.refreshSelected();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  close(): void {
    if (!this.selected) return;
    this.ticketService.close(this.selected.id).subscribe({
      next: () => {
        this.success = 'Closed';
        this.cdr.markForCheck();
        this.refreshSelected();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  reopen(): void {
    if (!this.selected || !this.reopenReason.trim()) return;
    this.ticketService.reopen(this.selected.id, this.reopenReason.trim()).subscribe({
      next: () => {
        this.success = 'Reopened';
        this.reopenReason = '';
        this.cdr.markForCheck();
        this.refreshSelected();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  escalate(): void {
    if (!this.selected || !this.escalateReason.trim()) return;
    this.ticketService.escalate(this.selected.id, this.escalateReason.trim()).subscribe({
      next: () => { this.success = 'Escalated'; this.escalateReason = ''; this.cdr.markForCheck(); this.refreshSelected(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  reassign(): void {
    if (!this.selected || !this.reassignAgentId || !this.reassignReason.trim()) return;
    this.ticketService.reassign(this.selected.id, this.reassignAgentId, this.reassignReason.trim()).subscribe({
      next: () => { this.success = 'Reassigned'; this.reassignReason = ''; this.cdr.markForCheck(); this.refreshSelected(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  submitSatisfaction(): void {
    if (!this.selected || !this.satisfactionFeedback.trim()) return;
    this.ticketService.recordSatisfaction(this.selected.id, this.satisfactionRating, this.satisfactionFeedback.trim()).subscribe({
      next: () => { this.success = 'Thanks for your feedback'; this.satisfactionFeedback = ''; this.cdr.markForCheck(); this.refreshSelected(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  statusClass(s: string): string {
    return (
      {
        NEW: 'text-bg-secondary',
        OPEN: 'text-bg-primary',
        IN_PROGRESS: 'text-bg-info',
        RESOLVED: 'text-bg-success',
        CLOSED: 'text-bg-dark',
        WAITING: 'text-bg-warning',
      }[s] || 'text-bg-light'
    );
  }

  priorityClass(p: string): string {
    return (
      {
        CRITICAL: 'text-bg-danger',
        HIGH: 'text-bg-warning',
        MEDIUM: 'text-bg-info',
        LOW: 'text-bg-light',
      }[p] || 'text-bg-secondary'
    );
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
