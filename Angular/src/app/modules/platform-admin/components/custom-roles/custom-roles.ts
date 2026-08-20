import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CustomRole, CustomRoleRequest } from '../../models/platform-admin.model';
import { CustomRoleService } from '../../services/custom-role.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-custom-roles',
  imports: [CommonModule, FormsModule, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './custom-roles.html',
})
export class CustomRoles implements OnInit {
  // VARIABLES
  roles: CustomRole[] = [];
  loading = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: CustomRoleRequest = { name: '' };

  deleteTarget: CustomRole | null = null;

  constructor(private roleService: CustomRoleService, private cdr: ChangeDetectorRef) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void { this.load(); }

  // LOAD ROLES
  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.roleService.list().subscribe({
      next: (res) => { this.roles = res || []; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load custom roles'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // OPEN CREATE / EDIT
  openCreate(): void { this.editingId = null; this.form = { name: '' }; this.showForm = true; }
  openEdit(r: CustomRole): void {
    if (r.systemRole) return;
    this.editingId = r.id;
    this.form = { name: r.name, description: r.description };
    this.showForm = true;
  }

  // SAVE
  save(): void {
    const op = this.editingId
      ? this.roleService.update(this.editingId, this.form)
      : this.roleService.create(this.form);
    op.subscribe({
      next: () => {
        this.success = this.editingId ? 'Role updated' : 'Role created';
        this.showForm = false; this.editingId = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save role'; this.cdr.markForCheck(); }
    });
  }

  // DELETE
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.roleService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Role deleted'; this.cdr.markForCheck(); this.load(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete role'; this.cdr.markForCheck(); }
    });
  }
}
