import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SupportMessage, SupportTicket } from '../../../support/models/support.model';
import { MessageService } from '../../../support/services/message.service';
import { TicketService } from '../../../support/services/ticket.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ChatSocketService } from '../../../../core/services/chat-socket.service';
import { ChatThread, ChatMessage } from '../../../../shared/components/chat-thread/chat-thread';
import { FileUploadResult, FileUploadService } from '../../../../shared/services/file-upload.service';
import { Loader } from '../../../../shared/components/loader/loader';

@Component({
  selector: 'app-client-ticket-detail',
  imports: [CommonModule, RouterLink, Loader, ChatThread],
  templateUrl: './client-ticket-detail.html',
  styleUrl: './client-ticket-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientTicketDetail implements OnInit, OnDestroy {
  ticketId!: number;
  ticket?: SupportTicket;
  messages: SupportMessage[] = [];
  loading = false;
  posting = false;
  error = '';
  // Uploaded (via ChatThread's paperclip button -> attachFile output) ahead of
  // the message it'll ride along with - ChatThread's (send) only emits text,
  // so this is attached in send() below rather than passed through the thread
  // component itself.
  pendingAttachment: FileUploadResult | null = null;
  uploadingAttachment = false;

  private chatUnsubscribe?: () => void;

  constructor(
    private route: ActivatedRoute,
    private ticketService: TicketService,
    private messageService: MessageService,
    private auth: AuthService,
    private chatSocket: ChatSocketService,
    private fileUploadService: FileUploadService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.ticketId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
    this.connectLive();
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

  // A client only ever sees external messages - the backend already excludes
  // internal notes from getClientMessages(), this just sorts oldest-first.
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

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    this.ticketService.getClientTicketById(this.ticketId).subscribe({
      next: (t) => { this.ticket = t; this.cdr.markForCheck(); },
      error: () => {
        this.error = 'Ticket not found';
        this.cdr.markForCheck();
      },
    });

    this.messageService.getClientMessages(this.ticketId).subscribe({
      next: (res) => {
        this.messages = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load messages';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private connectLive(): void {
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
    this.posting = true;
    this.cdr.markForCheck();
    const attachment = this.pendingAttachment;
    // The live push only reaches the *other* party, never the sender - refetch
    // so this tab also sees the message it just sent.
    this.messageService
      .createForClient({
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
        WAITING: 'text-bg-warning',
        ON_HOLD: 'text-bg-warning',
        RESOLVED: 'text-bg-success',
        CLOSED: 'text-bg-dark',
        REOPENED: 'text-bg-danger',
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
