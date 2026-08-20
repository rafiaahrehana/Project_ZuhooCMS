import { Component, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SupportMessage, SupportTicket } from '../../models/support.model';
import { MessageService } from '../../services/message.service';
import { TicketService } from '../../services/ticket.service';
import { AgentService } from '../../services/agent.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ChatSocketService } from '../../../../core/services/chat-socket.service';
import { ChatThread, ChatMessage } from '../../../../shared/components/chat-thread/chat-thread';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-messages',
  imports: [CommonModule, FormsModule, Loader, EmptyState, ChatThread],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './messages.html',
  styleUrl: './messages.scss',
})
export class Messages implements OnDestroy {
  ticketId?: number;
  ticket?: SupportTicket;
  external: SupportMessage[] = [];
  internal: SupportMessage[] = [];
  showInternal = false;
  sendAsInternal = false;
  loading = false;
  posting = false;
  error = '';

  // Inbox (left pane) - the list this page shows when landed on directly, so it's
  // never just a dead-end empty state; also lets you switch conversations without
  // going back to Support Tickets Desk.
  conversations: SupportTicket[] = [];
  loadingConversations = false;

  isPlatformStaff = false;
  // Mirrors the role split in tickets.ts - which ticket-list endpoint we're allowed
  // to call determines what shows up in the inbox.
  isEmployee = false;
  isManager = false;
  isAgent = false;
  private myAgentId: number | null = null;

  private chatUnsubscribe?: () => void;

  constructor(
    private messageService: MessageService,
    private ticketService: TicketService,
    private agentService: AgentService,
    private auth: AuthService,
    private chatSocket: ChatSocketService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    const roles = this.auth.getCurrentUser()?.roles ?? [];
    this.isManager = roles.includes('SUPPORT_MANAGER');
    this.isAgent = roles.includes('SUPPORT_AGENT');
    this.isEmployee = roles.includes('EMPLOYEE') && !this.isManager && !this.isAgent;
    this.isPlatformStaff =
      this.isManager || this.isAgent || roles.includes('SUPER_ADMIN') || roles.includes('SYSTEM_ADMIN');

    const qp = this.route.snapshot.queryParamMap.get('ticketId');
    if (qp) {
      this.ticketId = Number(qp);
      this.load();
      this.connectLive();
    }

    if (this.isAgent && !this.isManager) {
      const userId = this.auth.getCurrentUser()?.id;
      if (userId) {
        this.agentService.getByUserId(userId).subscribe({
          next: (a) => { this.myAgentId = a.id; this.loadConversations(); },
          error: () => this.loadConversations(),
        });
      } else {
        this.loadConversations();
      }
    } else {
      this.loadConversations();
    }
  }

  ngOnDestroy(): void {
    this.chatUnsubscribe?.();
  }

  get currentUserId(): number | null {
    return this.auth.getCurrentUser()?.id ?? null;
  }

  get chatConnected(): boolean {
    return this.chatSocket.connected;
  }

  // Oldest-first for ChatThread; merge in internal notes (staff only) so the
  // "Internal note" pill on each bubble is the only thing distinguishing them
  // from the client-facing thread, rather than a separate list.
  get chatMessages(): ChatMessage[] {
    const combined = this.isPlatformStaff && this.showInternal
      ? [...this.external, ...this.internal]
      : this.external;
    return [...combined]
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
      .map((m) => ({
        id: m.id,
        authorId: m.sentById,
        authorName: m.sentByName || 'Unknown',
        content: m.message,
        createdAt: m.createdAt,
        internal: m.isInternal,
      }));
  }

  loadConversations(): void {
    this.loadingConversations = true;
    this.cdr.markForCheck();
    const obs = this.isEmployee
      ? this.ticketService.myTickets(undefined, 0, 30)
      : this.isAgent && !this.isManager && this.myAgentId != null
        ? this.ticketService.assignedToMe(this.myAgentId, 0, 30)
        : this.ticketService.list(0, 30);
    obs.subscribe({
      next: (res) => {
        this.conversations = [...res.content].sort(
          (a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime(),
        );
        this.loadingConversations = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.conversations = [];
        this.loadingConversations = false;
        this.cdr.markForCheck();
      },
    });
  }

  // Switch conversation without a full route reload - updates the URL (so refresh/back
  // still lands on the same conversation) and re-subscribes the live channel.
  selectConversation(t: SupportTicket): void {
    if (this.ticketId === t.id) return;
    this.ticketId = t.id;
    this.ticket = t;
    this.external = [];
    this.internal = [];
    this.error = '';
    this.router.navigate([], { relativeTo: this.route, queryParams: { ticketId: t.id }, queryParamsHandling: 'merge' });
    this.load();
    this.connectLive();
  }

  load(): void {
    if (!this.ticketId) return;
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    this.ticketService.getById(this.ticketId).subscribe({
      next: (t) => { this.ticket = t; this.cdr.markForCheck(); },
      error: () => {},
    });

    this.messageService.getExternalMessages(this.ticketId).subscribe({
      next: (res) => {
        this.external = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load messages for that ticket';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
    if (this.isPlatformStaff) {
      this.messageService.getInternalNotes(this.ticketId).subscribe({
        next: (res) => { this.internal = res; this.cdr.markForCheck(); },
      });
    }
  }

  private connectLive(): void {
    this.chatUnsubscribe?.();
    if (!this.ticketId) return;
    this.chatUnsubscribe = this.chatSocket.subscribe(
      `/user/queue/support-tickets/${this.ticketId}/messages`,
      (message: SupportMessage) => {
        const bucket = message.isInternal ? this.internal : this.external;
        if (bucket.some((m) => m.id === message.id)) return;
        if (message.isInternal) {
          this.internal = [...this.internal, message];
        } else {
          this.external = [...this.external, message];
        }
        this.cdr.markForCheck();
      },
    );
  }

  send(text: string): void {
    if (!this.ticketId) return;
    this.posting = true;
    this.cdr.markForCheck();
    // The live push only reaches the *other* party, never the sender - refetch
    // so this tab also sees the message it just sent.
    this.messageService
      .create({ ticketId: this.ticketId, message: text, isInternal: this.isPlatformStaff && this.sendAsInternal })
      .subscribe({
        next: () => {
          this.posting = false;
          this.load();
        },
        error: (err) => {
          this.posting = false;
          this.error = err?.error?.message || 'Failed to send message';
          this.cdr.markForCheck();
        },
      });
  }

  statusClass(s: string | undefined): string {
    return (
      {
        NEW: 'text-bg-secondary',
        OPEN: 'text-bg-primary',
        IN_PROGRESS: 'text-bg-info',
        RESOLVED: 'text-bg-success',
        CLOSED: 'text-bg-dark',
        WAITING: 'text-bg-warning',
      }[s || ''] || 'text-bg-light'
    );
  }

  priorityClass(p: string | undefined): string {
    return (
      {
        CRITICAL: 'text-bg-danger',
        HIGH: 'text-bg-warning',
        MEDIUM: 'text-bg-info',
        LOW: 'text-bg-light',
      }[p || ''] || 'text-bg-secondary'
    );
  }
}
