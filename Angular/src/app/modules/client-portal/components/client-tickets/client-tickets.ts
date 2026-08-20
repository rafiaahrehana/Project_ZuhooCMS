import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SupportTicket } from '../../../support/models/support.model';
import { TicketService } from '../../../support/services/ticket.service';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { FileUploadResult } from '../../../../shared/services/file-upload.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

// CUSTOMER_SUPPORT tickets - a client's own "something's broken, help" channel,
// distinct from Service Requests (which are catalog-service driven and require
// picking an existing CompanyService).
@Component({
  selector: 'app-client-tickets',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, Loader, EmptyState, FileUpload],
  templateUrl: './client-tickets.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientTickets implements OnInit {
  tickets: SupportTicket[] = [];
  loading = false;
  error = '';

  showCreate = false;
  creating = false;
  form;
  // Optional screenshot/image attached to the new ticket (images only - see
  // imagesOnly on <app-file-upload>) - uploaded immediately on selection,
  // then included by url/filename when the ticket itself is submitted.
  attachment: FileUploadResult | null = null;

  constructor(
    private ticketService: TicketService,
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', Validators.required],
      priority: ['MEDIUM', Validators.required],
    });
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.ticketService.myClientTickets(0, 50).subscribe({
      next: (res) => {
        this.tickets = [...res.content].sort(
          (a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime(),
        );
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load your tickets';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.form.reset({ title: '', description: '', priority: 'MEDIUM' });
    this.attachment = null;
    this.showCreate = true;
    this.cdr.markForCheck();
  }

  closeCreate(): void {
    this.showCreate = false;
    this.cdr.markForCheck();
  }

  onAttachmentUploaded(result: FileUploadResult): void {
    this.attachment = result;
    this.cdr.markForCheck();
  }

  removeAttachment(): void {
    this.attachment = null;
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.creating = true;
    this.cdr.markForCheck();
    const payload = {
      ...this.form.getRawValue(),
      attachmentUrl: this.attachment?.fileUrl,
      attachmentFileName: this.attachment?.fileName,
    };
    this.ticketService.createForClient(payload as any).subscribe({
      next: (ticket) => {
        this.creating = false;
        this.showCreate = false;
        this.cdr.markForCheck();
        this.router.navigate(['/client/tickets', ticket.id]);
      },
      error: (err) => {
        this.creating = false;
        this.error = err?.error?.message || 'Failed to create ticket';
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
