import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CustomRoleService } from '../../services/custom-role.service';
import { CustomRole, CustomRoleRequest, Permission } from '../../models/roles-permissions.model';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Employee } from '../../../hrm/models/hrm.model';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

interface PermissionGroup {
  module: string;
  permissions: Permission[];
}

@Component({
  selector: 'app-roles-permissions',
  imports: [CommonModule, FormsModule, Loader, EmptyState, ConfirmDialog],
  templateUrl: './roles-permissions.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RolesPermissions implements OnInit {
  roles: CustomRole[] = [];
  loading = false;
  error = '';
  success = '';

  // Create / edit role
  showRoleForm = false;
  editingRole: CustomRole | null = null;
  roleForm: CustomRoleRequest = { name: '', description: '' };
  savingRole = false;
  deleteTarget: CustomRole | null = null;

  // Selected role detail (permissions + assigned employees)
  selected: CustomRole | null = null;
  permissionCatalog: Permission[] = [];
  permissionGroups: PermissionGroup[] = [];
  selectedCodes = new Set<string>();
  private originalCodes = new Set<string>();
  editingPermissions = false;

  /** Read-only permissions modal - the full checklist is too tall to live inline. */
  viewingPermissions = false;

  /** Total permissions in the catalog, for the "N of M enabled" summary. */
  get totalPermissionCount(): number {
    return this.permissionGroups.reduce((acc, g) => acc + g.permissions.length, 0);
  }

  /** Modules where this role has at least one permission. */
  get activeModuleCount(): number {
    return this.permissionGroups.filter((g) => g.permissions.some((p) => this.selectedCodes.has(p.code))).length;
  }
  savingPermissions = false;
  loadingDetail = false;

  employees: Employee[] = [];
  assignEmployeeId: number | null = null;
  assigning = false;

  constructor(
    private roleService: CustomRoleService,
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.roleService.listPermissionCatalog().subscribe({
      next: (perms) => {
        this.permissionCatalog = perms;
        this.permissionGroups = this.groupByModule(perms);
        this.cdr.markForCheck();
      },
    });
    this.employeeService.list(0, 200).subscribe({
      next: (res) => { this.employees = res.content; this.cdr.markForCheck(); },
    });
  }

  private groupByModule(perms: Permission[]): PermissionGroup[] {
    const map = new Map<string, Permission[]>();
    for (const p of perms) {
      if (!map.has(p.module)) map.set(p.module, []);
      map.get(p.module)!.push(p);
    }
    return Array.from(map.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([module, permissions]) => ({ module, permissions }));
  }

  load(): void {
    this.loading = true;
    this.roleService.list().subscribe({
      next: (roles) => { this.roles = roles; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load roles'; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  // ----- Role CRUD -----

  openCreateRole(): void {
    this.editingRole = null;
    this.roleForm = { name: '', description: '' };
    this.showRoleForm = true;
    this.cdr.markForCheck();
  }

  openEditRole(role: CustomRole): void {
    if (role.systemRole) return;
    this.editingRole = role;
    this.roleForm = { name: role.name, description: role.description };
    this.showRoleForm = true;
    this.cdr.markForCheck();
  }

  saveRole(): void {
    if (!this.roleForm.name?.trim()) {
      this.error = 'Role name is required';
      this.cdr.markForCheck();
      return;
    }
    this.savingRole = true;
    this.error = '';
    this.cdr.markForCheck();
    const obs = this.editingRole
      ? this.roleService.update(this.editingRole.id, this.roleForm)
      : this.roleService.create(this.roleForm);
    obs.subscribe({
      next: (role) => {
        this.success = this.editingRole ? 'Role updated' : 'Role created';
        this.savingRole = false;
        this.showRoleForm = false;
        this.cdr.markForCheck();
        this.load();
        if (this.editingRole) this.selectRole(role);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save role';
        this.savingRole = false;
        this.cdr.markForCheck();
      },
    });
  }

  confirmDeleteRole(): void {
    if (!this.deleteTarget) return;
    this.roleService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.success = 'Role deleted';
        if (this.selected?.id === this.deleteTarget!.id) this.selected = null;
        this.deleteTarget = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete role';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  // ----- Role detail: permissions + employees -----

  selectRole(role: CustomRole): void {
    this.selected = role;
    this.loadingDetail = true;
    this.editingPermissions = false;
    this.error = '';
    this.cdr.markForCheck();
    this.roleService.getPermissions(role.id).subscribe({
      next: (codes) => {
        this.selectedCodes = new Set(codes);
        this.originalCodes = new Set(codes);
        this.loadingDetail = false;
        this.cdr.markForCheck();
      },
      error: () => { this.loadingDetail = false; this.cdr.markForCheck(); },
    });
  }

  editPermissions(): void {
    this.editingPermissions = true;
  }

  cancelEditPermissions(): void {
    this.selectedCodes = new Set(this.originalCodes);
    this.editingPermissions = false;
  }

  togglePermission(code: string): void {
    if (this.selectedCodes.has(code)) this.selectedCodes.delete(code);
    else this.selectedCodes.add(code);
  }

  toggleModule(group: PermissionGroup): void {
    const allChecked = group.permissions.every((p) => this.selectedCodes.has(p.code));
    for (const p of group.permissions) {
      if (allChecked) this.selectedCodes.delete(p.code);
      else this.selectedCodes.add(p.code);
    }
  }

  isModuleFullyChecked(group: PermissionGroup): boolean {
    return group.permissions.every((p) => this.selectedCodes.has(p.code));
  }

  isModulePartiallyChecked(group: PermissionGroup): boolean {
    const checked = group.permissions.filter((p) => this.selectedCodes.has(p.code)).length;
    return checked > 0 && checked < group.permissions.length;
  }

  savePermissions(): void {
    if (!this.selected) return;
    this.savingPermissions = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();
    this.roleService.setPermissions(this.selected.id, Array.from(this.selectedCodes)).subscribe({
      next: (codes) => {
        this.selectedCodes = new Set(codes);
        this.originalCodes = new Set(codes);
        this.success = 'Permissions saved';
        this.savingPermissions = false;
        this.editingPermissions = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save permissions';
        this.savingPermissions = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ----- Employee assignment -----

  get assignedEmployees(): Employee[] {
    if (!this.selected) return [];
    return this.employees.filter((e) => e.customRoleId === this.selected!.id);
  }

  get unassignedEmployees(): Employee[] {
    if (!this.selected) return [];
    return this.employees.filter((e) => e.customRoleId !== this.selected!.id);
  }

  assignEmployee(): void {
    if (!this.selected || !this.assignEmployeeId) return;
    this.assigning = true;
    this.error = '';
    this.cdr.markForCheck();
    this.roleService.assignEmployee(this.selected.id, this.assignEmployeeId).subscribe({
      next: () => {
        const emp = this.employees.find((e) => e.id === this.assignEmployeeId);
        if (emp) { emp.customRoleId = this.selected!.id; emp.customRoleName = this.selected!.name; }
        this.assignEmployeeId = null;
        this.assigning = false;
        this.success = 'Employee assigned';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to assign employee';
        this.assigning = false;
        this.cdr.markForCheck();
      },
    });
  }

  unassignEmployee(emp: Employee): void {
    this.roleService.unassignEmployee(emp.id).subscribe({
      next: () => {
        emp.customRoleId = undefined;
        emp.customRoleName = undefined;
        this.success = 'Employee unassigned';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to unassign employee';
        this.cdr.markForCheck();
      },
    });
  }
}
