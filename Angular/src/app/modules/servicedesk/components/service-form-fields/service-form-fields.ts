import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  CompanyService,
  FORM_FIELD_TYPES,
  FormFieldType,
  ServiceFormField,
  ServiceFormFieldRequest,
} from '../../models/servicedesk.model';
import { ServiceFormFieldService } from '../../services/service-form-field.service';
import { CompanyServiceService } from '../../services/company-service.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-service-form-fields',
  imports: [CommonModule, FormsModule, RouterLink, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './service-form-fields.html',
})
export class ServiceFormFields implements OnInit {
  // VARIABLES
  serviceId!: number;
  service: CompanyService | null = null;
  fields: ServiceFormField[] = [];
  loading = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: ServiceFormFieldRequest = { label: '', fieldType: 'TEXT', required: false, sortOrder: 1 };

  deleteTarget: ServiceFormField | null = null;

  fieldTypes: FormFieldType[] = FORM_FIELD_TYPES;

  constructor(
    private route: ActivatedRoute,
    private fieldService: ServiceFormFieldService,
    private serviceService: CompanyServiceService,
    private cdr: ChangeDetectorRef,
  ) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void {
    this.serviceId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadService();
    this.loadFields();
  }

  // LOAD PARENT SERVICE (FOR HEADER CONTEXT)
  loadService(): void {
    this.serviceService.getById(this.serviceId).subscribe({
      next: (res) => { this.service = res; this.cdr.markForCheck(); },
      error: () => { this.service = null; this.cdr.markForCheck(); }
    });
  }

  // LOAD FORM FIELDS
  loadFields(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.fieldService.list(this.serviceId).subscribe({
      next: (res) => { this.fields = res || []; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load form fields'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // OPEN CREATE / EDIT
  openCreate(): void {
    this.editingId = null;
    this.form = { label: '', fieldType: 'TEXT', required: false, sortOrder: this.fields.length + 1 };
    this.showForm = true;
  }

  openEdit(f: ServiceFormField): void {
    this.editingId = f.id;
    this.form = { label: f.label, fieldType: f.fieldType, required: f.required, validationRules: f.validationRules, sortOrder: f.sortOrder };
    this.showForm = true;
  }

  // SAVE FIELD
  save(): void {
    const op = this.editingId
      ? this.fieldService.update(this.serviceId, this.editingId, this.form)
      : this.fieldService.create(this.serviceId, this.form);
    op.subscribe({
      next: () => {
        this.success = this.editingId ? 'Field updated' : 'Field added';
        this.showForm = false; this.editingId = null;
        this.cdr.markForCheck();
        this.loadFields();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save field'; this.cdr.markForCheck(); }
    });
  }

  // DELETE FIELD
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.fieldService.delete(this.serviceId, this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Field removed'; this.cdr.markForCheck(); this.loadFields(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete field'; this.cdr.markForCheck(); }
    });
  }
}
