import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  FORM_FIELD_TYPES,
  FormFieldType,
  ServiceCategory,
  ServiceTemplate,
  ServiceTemplateRequest,
  TemplateFormField,
  TemplateRequiredDocument,
  TemplateWorkflowStage,
} from '../../models/servicedesk.model';
import { ServiceTemplateService } from '../../services/service-template.service';
import { ServiceCategoryService } from '../../services/service-category.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
type EditorTab = 'basics' | 'fields' | 'documents' | 'stages';

@Component({
  selector: 'app-service-templates',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './templates.html',
})
export class Templates implements OnInit {
  // VARIABLES
  templates: ServiceTemplate[] = [];
  categories: ServiceCategory[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  // EDITOR MODAL STATE
  editorOpen = false;
  editorTab: EditorTab = 'basics';
  editingId: number | null = null;
  form: ServiceTemplateRequest = { name: '' };

  deleteTarget: ServiceTemplate | null = null;

  fieldTypes: FormFieldType[] = FORM_FIELD_TYPES;

  constructor(
    private templateService: ServiceTemplateService,
    private categoryService: ServiceCategoryService,
    private cdr: ChangeDetectorRef,
  ) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void { this.load(); }

  // LOAD TEMPLATES
  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.templateService.list(this.page).subscribe({
      next: (res) => { this.templates = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load templates'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // LOAD CATEGORY LOOKUP
  loadCategories(): void {
    if (this.categories.length) return;
    this.categoryService.lookup().subscribe({ next: (res) => { this.categories = res; this.cdr.markForCheck(); }, error: () => { this.categories = []; this.cdr.markForCheck(); } });
  }

  // OPEN CREATE
  openCreate(): void {
    this.editingId = null;
    this.form = { name: '', active: true, formFields: [], requiredDocuments: [], workflowStages: [] };
    this.editorTab = 'basics';
    this.editorOpen = true;
    this.loadCategories();
  }

  // OPEN EDIT (FETCH FRESH FOR NESTED DATA)
  openEdit(t: ServiceTemplate): void {
    this.editingId = t.id;
    this.templateService.getById(t.id).subscribe({
      next: (res) => {
        this.form = {
          name: res.name,
          description: res.description,
          categoryId: res.categoryId,
          defaultPrice: res.defaultPrice,
          estimatedDays: res.estimatedDays,
          iconUrl: res.iconUrl,
          active: res.active,
          formFields: res.formFields || [],
          requiredDocuments: res.requiredDocuments || [],
          workflowStages: res.workflowStages || [],
        };
        this.editorTab = 'basics';
        this.editorOpen = true;
        this.cdr.markForCheck();
        this.loadCategories();
      },
      error: () => { this.error = 'Failed to load template'; this.cdr.markForCheck(); }
    });
  }

  // CLOSE MODAL
  closeEditor(): void {
    this.editorOpen = false;
    this.editingId = null;
  }

  // SAVE TEMPLATE
  save(): void {
    const op = this.editingId
      ? this.templateService.update(this.editingId, this.form)
      : this.templateService.create(this.form);
    op.subscribe({
      next: () => {
        this.success = this.editingId ? 'Template updated' : 'Template created';
        this.closeEditor();
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save template'; this.cdr.markForCheck(); }
    });
  }

  // NESTED: FORM FIELDS
  addField(): void {
    const list = this.form.formFields || [];
    list.push({ label: '', fieldType: 'TEXT', required: false, sortOrder: list.length + 1 });
    this.form.formFields = list;
  }

  removeField(i: number): void {
    (this.form.formFields || []).splice(i, 1);
  }

  // NESTED: REQUIRED DOCUMENTS
  addDocument(): void {
    const list = this.form.requiredDocuments || [];
    list.push({ docName: '', mandatory: false, sortOrder: list.length + 1 });
    this.form.requiredDocuments = list;
  }

  removeDocument(i: number): void {
    (this.form.requiredDocuments || []).splice(i, 1);
  }

  // NESTED: WORKFLOW STAGES
  addStage(): void {
    const list = this.form.workflowStages || [];
    list.push({ stageName: '', stageOrder: list.length + 1, requiresClientAction: false, requiresPayment: false, isFinalStage: false });
    this.form.workflowStages = list;
  }

  removeStage(i: number): void {
    (this.form.workflowStages || []).splice(i, 1);
  }

  // DELETE TEMPLATE
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.templateService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Template deleted'; this.cdr.markForCheck(); this.load(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete template'; this.cdr.markForCheck(); }
    });
  }

  // PAGINATION
  goToPage(p: number): void { this.page = p; this.load(); }

  // HELPERS FOR TEMPLATE
  trackField(_: number, f: TemplateFormField): number | string { return f.id ?? f.label + _; }
  trackDoc(_: number, d: TemplateRequiredDocument): number | string { return d.id ?? d.docName + _; }
  trackStage(_: number, s: TemplateWorkflowStage): number | string { return s.id ?? s.stageName + _; }
}
