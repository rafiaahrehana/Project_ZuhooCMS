import { Component, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { SupportMessage, SupportTicket } from '../../models/support.model';
import { MessageService } from '../../services/message.service';
import { TicketService } from '../../services/ticket.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ChatSocketService } from '../../../../core/services/chat-socket.service';
import { ChatThread, ChatMessage } from '../../../../shared/components/chat-thread/chat-thread';
import { FileUploadService } from '../../../../shared/services/file-upload.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

// Staff-facing counterpart to Messages: this company's own CUSTOMER_SUPPORT
// tickets (their clients messaging them), not PLATFORM_SUPPORT (this company
// messaging BusinessOS) - the two used to be mixed into the same inbox before
// the backend started filtering by ticketType. Deliberately simpler than
// Messages (no agent assignment, no internal-notes toggle) since the audience
// here is just "whoever on staff picks this up," not a multi-tier support desk.
@Component({
  selector: 'app-client-chat',
  imports: [CommonModule, Loader, EmptyState, ChatThread],
  templateUrl: './client-chat.html',
  styleUrl: './client-chat.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientChat implements OnDestroy {
  ticketId?: number;
  ticket?: SupportTicket;
  messages: SupportMessage[] = [];
  loading = false;
  posting = false;
  error = '';

  conversations: SupportTicket[] = [];
  loadingConversations = false;

  pendingAttachment: { fileUrl: string; fileName: string } | null = null;
  uploadingAttachment = false;

  private chatUnsubscribe?: () => void;

  constructor(
    private messageService: MessageService,
    private ticketService: TicketService,
    private auth: AuthService,
    private chatSocket: ChatSocketService,
    private fileUploadService: FileUploadService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    const qp = this.route.snapshot.queryParamMap.get('ticketId');
    if (qp) {
      this.ticketId = Number(qp);
      this.load();
      this.connectLive();
    }
    this.loadConversations();
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

  get chatMessages(): ChatMessage[] {
    return [...this.messages]
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
      .map((m) => ({
        id: m.id,
        authorId: m.sentById,
        authorName: m.sentByName || 'Unknown',
        content: m.message,
        createdAt: m.createdAt,
        attachmentUrl: m.attachmentUrl,
        attachmentFileName: m.attachmentFileName,
      }));
  }

  loadConversations(): void {
    this.loadingConversations = true;
    this.cdr.markForCheck();
    this.ticketService.companyClientTickets(0, 30).subscribe({
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

  selectConversation(t: SupportTicket): void {
    if (this.ticketId === t.id) return;
    this.ticketId = t.id;
    this.ticket = t;
    this.messages = [];
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
        this.messages = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load messages for that ticket';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private connectLive(): void {
    this.chatUnsubscribe?.();
    if (!this.ticketId) return;
    this.chatUnsubscribe = this.chatSocket.subscribe(
      `/user/queue/support-tickets/${this.ticketId}/messages`,
      (message: SupportMessage) => {
        if (this.messages.some((m) => m.id === message.id)) return;
        this.messages = [...this.messages, message];
        this.cdr.markForCheck();
      },
    );
  }

  onAttachFile(file: File): void {
    this.uploadingAttachment = true;
    this.error = '';
    this.cdr.markForCheck();
    this.fileUploadService.upload(file).subscribe({
      next: (result) => {
        this.uploadingAttachment = false;
        this.pendingAttachment = result;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.uploadingAttachment = false;
        this.error = err?.error?.message || 'Failed to upload attachment';
        this.cdr.markForCheck();
      },
    });
  }

  removeAttachment(): void {
    this.pendingAttachment = null;
    this.cdr.markForCheck();
  }

  send(text: string): void {
    if (!this.ticketId) return;
    this.posting = true;
    this.cdr.markForCheck();
    const attachment = this.pendingAttachment;
    this.messageService
      .create({
        ticketId: this.ticketId,
        message: text,
        attachmentUrl: attachment?.fileUrl,
        attachmentFileName: attachment?.fileName,
      })
      .subscribe({
        next: () => {
          this.posting = false;
          this.pendingAttachment = null;
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
