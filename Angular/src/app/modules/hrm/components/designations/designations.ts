import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Designation,
  DesignationRequest,
  Department,
  JOB_LEVELS,
  DESIGNATION_EMPLOYMENT_CATEGORIES,
} from '../../models/hrm.model';
import { DesignationService } from '../../services/designation.service';
import { DepartmentService } from '../../services/department.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-designations',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './designations.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './designations.scss',
})
export class Designations implements OnInit {
  designations: Designation[] = [];
  departments: Department[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  jobLevels = JOB_LEVELS;
  employmentCategories = DESIGNATION_EMPLOYMENT_CATEGORIES;

  showForm = false;
  editingId: number | null = null;
  form: DesignationRequest = this.emptyForm();
  codeManuallyEdited = false;
  codeError = '';

  // Searchable department combobox state
  departmentSearch = '';
  departmentDropdownOpen = false;

  deleteTarget: Designation | null = null;

  constructor(
    private designationService: DesignationService,
    private departmentService: DepartmentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.departmentService.listActive().subscribe({
      next: (res) => { this.departments = res; this.cdr.markForCheck(); },
      error: () => { this.departments = []; this.cdr.markForCheck(); },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.designationService.list(this.page, 20).subscribe({
      next: (res) => {
        this.designations = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load designations';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private emptyForm(): DesignationRequest {
    return { name: '', code: '', level: 1, description: '', employmentCategory: '', departmentId: undefined, active: true };
  }

  get filteredDepartments(): Department[] {
    const q = this.departmentSearch.trim().toLowerCase();
    if (!q) return this.departments;
    return this.departments.filter((d) => d.name.toLowerCase().includes(q));
  }

  get selectedDepartmentName(): string {
    const dept = this.departments.find((d) => d.id === this.form.departmentId);
    return dept ? dept.name : '';
  }

  openDepartmentDropdown(): void {
    this.departmentDropdownOpen = true;
    this.departmentSearch = '';
  }

  selectDepartment(dept: Department | null): void {
    this.form.departmentId = dept ? dept.id : undefined;
    this.departmentDropdownOpen = false;
    this.departmentSearch = '';
  }

  closeDepartmentDropdown(): void {
    // Delay so a click on an option registers before the list unmounts.
    setTimeout(() => { this.departmentDropdownOpen = false; this.cdr.markForCheck(); }, 150);
  }

  onNameInput(): void {
    if (this.codeManuallyEdited) return;
    this.form.code = this.generateCode(this.form.name);
  }

  onCodeInput(): void {
    this.codeManuallyEdited = true;
    this.codeError = '';
  }

  private generateCode(name: string): string {
    const words = name.trim().split(/\s+/).filter(Boolean);
    if (!words.length) return '';
    if (words.length === 1) return words[0].slice(0, 4).toUpperCase();
    return words.map((w) => w.charAt(0).toUpperCase()).join('').slice(0, 6);
  }

  get descriptionLength(): number {
    return (this.form.description || '').length;
  }

  get nameError(): string {
    return this.form.name.trim() ? '' : 'Designation name is required.';
  }

  get levelError(): string {
    return this.form.level ? '' : 'Please select a job level.';
  }

  get descriptionError(): string {
    return this.descriptionLength > 500 ? 'Description cannot exceed 500 characters.' : '';
  }

  get formValid(): boolean {
    return !this.nameError && !!this.form.code.trim() && !this.levelError && !this.descriptionError;
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.codeManuallyEdited = false;
    this.error = '';
    this.showForm = true;
  }

  openEdit(d: Designation): void {
    this.editingId = d.id;
    this.form = {
      name: d.name,
      code: d.code,
      level: d.level,
      description: d.description || '',
      employmentCategory: d.employmentCategory || '',
      departmentId: d.departmentId,
      active: d.active,
    };
    this.codeManuallyEdited = true;
    this.error = '';
    this.showForm = true;
  }

  save(): void {
    if (!this.formValid) return;
    this.saving = true;
    this.error = '';
    this.codeError = '';
    const req = this.editingId
      ? this.designationService.update(this.editingId, this.form)
      : this.designationService.create(this.form);
    req.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.editingId ? 'Designation updated' : 'Designation created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        const message = err?.error?.message || 'Failed to save designation';
        if (message.toLowerCase().includes('already exists')) {
          this.codeError = message;
        } else {
          this.error = message;
        }
        this.cdr.markForCheck();
      },
    });
  }

  toggle(d: Designation): void {
    this.designationService.toggle(d.id).subscribe({
      next: () => this.load(),
      error: () => { this.error = 'Failed to update designation status'; this.cdr.markForCheck(); },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.designationService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Designation deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete designation';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
