import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PLATFORM_ROLES, PlatformRole, PlatformUser, PlatformUserRequest } from '../../models/platform-admin.model';
import { PlatformUserService } from '../../services/platform-user.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-platform-users',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './platform-users.html',
})
export class PlatformUsers implements OnInit {
  users: PlatformUser[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: PlatformUserRequest = this.emptyForm();
  deactivateTarget: PlatformUser | null = null;

  roles: PlatformRole[] = PLATFORM_ROLES;

  constructor(private userService: PlatformUserService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  emptyForm(): PlatformUserRequest {
    return { firstName: '', lastName: '', email: '', password: '', role: 'SYSTEM_ADMIN', phone: '' };
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.userService.list(this.page).subscribe({
      next: (res) => {
        this.users = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load platform users';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.showForm = true;
  }

  openEdit(u: PlatformUser): void {
    this.editingId = u.id;
    // Password left blank on edit - only sent to the backend (and only changed) if filled in
    this.form = { firstName: u.firstName, lastName: u.lastName, email: u.email, password: '', role: u.role, phone: u.phone };
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();
    const op = this.editingId
      ? this.userService.update(this.editingId, this.form)
      : this.userService.create(this.form);
    op.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.editingId ? 'Platform user updated' : `Platform user ${this.form.email} created`;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save platform user';
        this.cdr.markForCheck();
      },
    });
  }

  doDeactivate(): void {
    if (!this.deactivateTarget) return;
    this.userService.deactivate(this.deactivateTarget.id).subscribe({
      next: () => {
        this.deactivateTarget = null;
        this.success = 'Platform user deactivated';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.deactivateTarget = null;
        this.error = err?.error?.message || 'Failed to deactivate';
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  roleLabel(role: string): string {
    return role.replace(/_/g, ' ').toLowerCase();
  }

  roleBadgeStyle(role: string): { [key: string]: string } {
    const styles: Record<string, { bg: string; color: string; border: string }> = {
      SUPER_ADMIN: { bg: 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)', color: '#ffffff', border: '#5b21b6' },
      SYSTEM_ADMIN: { bg: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)', color: '#ffffff', border: '#1e40af' },
      SUPPORT_AGENT: { bg: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)', color: '#ffffff', border: '#075985' },
      SUPPORT_MANAGER: { bg: 'linear-gradient(135deg, #059669 0%, #047857 100%)', color: '#ffffff', border: '#065f46' },
      MARKETING_MANAGER: { bg: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)', color: '#ffffff', border: '#92400e' },
      PLATFORM_ACCOUNTANT: { bg: 'linear-gradient(135deg, #475569 0%, #334155 100%)', color: '#ffffff', border: '#1e293b' },
      SALES_MANAGER: { bg: 'linear-gradient(135deg, #e11d48 0%, #be123c 100%)', color: '#ffffff', border: '#9f1239' },
    };
    const s = styles[role] || { bg: 'linear-gradient(135deg, #64748b 0%, #475569 100%)', color: '#ffffff', border: '#334155' };
    return {
      'background': s.bg,
      'color': s.color,
      'border': `1px solid ${s.border}`,
      'font-weight': '600',
      'font-size': '0.75rem',
      'padding': '0.35em 0.75em',
      'border-radius': '6px',
      'box-shadow': '0 2px 4px rgba(0,0,0,0.1)',
      'display': 'inline-block'
    };
  }
}
