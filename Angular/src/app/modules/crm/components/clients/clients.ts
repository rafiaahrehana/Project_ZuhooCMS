import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Client, DuplicateMatch, Tag } from '../../models/crm.model';
import { ClientService } from '../../services/client.service';
import { TagService } from '../../services/tag.service';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Employee } from '../../../hrm/models/hrm.model';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { DuplicateWarningModal } from '../../../../shared/components/duplicate-warning-modal/duplicate-warning-modal';

// Mirror of backend ClientStatus enum
const CLIENT_STATUSES = ['ACTIVE', 'INACTIVE', 'BLOCKED'] as const;

@Component({
  selector: 'app-clients',
  imports: [CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective, DuplicateWarningModal],
  templateUrl: './clients.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './clients.scss',
})
export class Clients implements OnInit {
  clients: Client[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';

  statuses = CLIENT_STATUSES;
  employees: Employee[] = [];
  tags: Tag[] = [];
  tagFilter: number | null = null;

  // Create modal (backend CreateClientRequest provisions a portal user: email + password)
  showCreate = false;
  saving = false;
  createForm: any = this.emptyCreateForm();

  // Edit modal (backend UpdateClientRequest - account-level fields only)
  editing: Client | null = null;
  editForm: any = {};

  deleteTarget: Client | null = null;
  duplicateMatch: DuplicateMatch | null = null;

  // Quick "add a new tag" row inside the create/edit form's tag picker
  newTagName = '';
  newTagColor = '#7d55fa';
  addingTag = false;

  constructor(
    private clientService: ClientService,
    private employeeService: EmployeeService,
    private tagService: TagService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.employeeService.list(0, 100).subscribe({ next: (res) => { this.employees = res.content; this.cdr.markForCheck(); } });
    this.tagService.list().subscribe({ next: (tags) => { this.tags = tags; this.cdr.markForCheck(); } });
  }

  private emptyCreateForm(): any {
    return {
      provisionPortalLogin: false,
      firstName: '', lastName: '', email: '', password: '', phone: '',
      clientCompanyName: '', industry: '', website: '', taxId: '', accountManagerId: null,
      billingAddress: '', shippingAddress: '', tags: '', employeeCount: null, annualRevenue: null,
      tagIds: [],
    };
  }

  tagColor(tagId: number): string {
    return this.tags.find((t) => t.id === tagId)?.color || '#6b7280';
  }

  createTag(targetForm: any): void {
    const name = this.newTagName.trim();
    if (!name || this.addingTag) return;
    this.addingTag = true;
    this.tagService.create({ name, color: this.newTagColor }).subscribe({
      next: (tag) => {
        this.tags = [...this.tags, tag];
        targetForm.tagIds = [...targetForm.tagIds, tag.id];
        this.newTagName = '';
        this.addingTag = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create tag';
        this.addingTag = false;
        this.cdr.markForCheck();
      },
    });
  }

  setTagFilter(tagId: number | null): void {
    this.tagFilter = tagId;
    this.page = 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.clientService.list(this.page, 20, this.statusFilter || undefined, this.tagFilter).subscribe({
      next: (res) => {
        this.clients = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load clients';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.createForm = this.emptyCreateForm();
    this.showCreate = true;
    this.cdr.markForCheck();
  }

  saveCreate(): void {
    const f = this.createForm;
    if (!f.clientCompanyName?.trim()) {
      this.error = 'Company name is required';
      this.cdr.markForCheck();
      return;
    }
    if (f.provisionPortalLogin && (!f.firstName?.trim() || !f.lastName?.trim() || !f.email?.trim() || !f.password)) {
      this.error = 'First name, last name, email and password are required to provision a portal login';
      this.cdr.markForCheck();
      return;
    }
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    const payload: any = {};
    Object.entries(f).forEach(([k, v]) => { if (v !== '' && v !== null) payload[k] = v; });
    this.clientService.create(payload).subscribe({
      next: (res) => {
        this.success = 'Client created';
        this.saving = false;
        this.showCreate = false;
        if (res.possibleDuplicate) {
          this.duplicateMatch = res.possibleDuplicate;
        }
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create client';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  openEdit(client: Client): void {
    this.editing = client;
    this.editForm = {
      clientCompanyName: client.clientCompanyName || '',
      industry: client.industry || '',
      website: client.website || '',
      taxId: client.taxId || '',
      status: client.status,
      accountManagerId: client.accountManagerId ?? null,
      portalAccessEnabled: client.portalAccessEnabled ?? false,
      billingAddress: client.billingAddress || '',
      shippingAddress: client.shippingAddress || '',
      tags: client.tags || '',
      employeeCount: client.employeeCount ?? null,
      annualRevenue: client.annualRevenue ?? null,
      tagIds: client.tagList?.map((t) => t.id) || [],
    };
    this.cdr.markForCheck();
  }

  saveEdit(): void {
    if (!this.editing) return;
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    this.clientService.update(this.editing.id, this.editForm).subscribe({
      next: () => {
        this.success = 'Client updated';
        this.saving = false;
        this.editing = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update client';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  /** Client awaiting invite confirmation. */
  inviteTarget: Client | null = null;
  /** Id of the client whose invite is in flight, for the row spinner. */
  invitingId: number | null = null;

  confirmInvite(): void {
    const target = this.inviteTarget;
    if (!target) return;

    // Cleared immediately so the dialog closes; the row spinner shows progress.
    this.inviteTarget = null;
    this.invitingId = target.id;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.clientService.inviteToPortal(target.id).subscribe({
      next: (updated) => {
        this.invitingId = null;

        // The login is created even when the email fails - reporting both as
        // "invite sent" would leave a client waiting for mail that never came.
        if (updated?.inviteEmailSent === false) {
          this.error = (updated.inviteEmailError || 'The invite email could not be delivered.')
            + ' The login is ready — check the mail server settings, then resend.';
        } else {
          this.success = `Portal invite sent to ${target.clientCompanyName || 'the client'}`;
        }
        this.cdr.markForCheck();
        // Reload so the portal-access column reflects the change.
        this.load();
      },
      error: (err) => {
        // The most common failure is a client with no contact email - the backend
        // says so explicitly, so surface its message rather than a generic one.
        this.error = err?.error?.message || 'Failed to send the portal invite';
        this.invitingId = null;
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.clientService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.success = 'Client deleted';
        this.deleteTarget = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete client';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  statusClass(status: string): string {
    return {
      ACTIVE: 'text-bg-success', INACTIVE: 'text-bg-secondary', BLOCKED: 'text-bg-danger',
    }[status] || 'text-bg-secondary';
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
