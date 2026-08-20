import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  CompanyService,
  RequiredDocument,
  RequiredDocumentRequest,
} from '../../models/servicedesk.model';
import { RequiredDocumentService } from '../../services/required-document.service';
import { CompanyServiceService } from '../../services/company-service.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-service-required-documents',
  imports: [CommonModule, FormsModule, RouterLink, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './service-required-documents.html',
})
export class ServiceRequiredDocuments implements OnInit {
  serviceId!: number;
  service: CompanyService | null = null;
  documents: RequiredDocument[] = [];
  loading = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: RequiredDocumentRequest = { docName: '', mandatory: true, sortOrder: 1 };

  deleteTarget: RequiredDocument | null = null;

  constructor(
    private route: ActivatedRoute,
    private docService: RequiredDocumentService,
    private serviceService: CompanyServiceService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.serviceId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadService();
    this.loadDocuments();
  }

  loadService(): void {
    this.serviceService.getById(this.serviceId).subscribe({
      next: (res) => { this.service = res; this.cdr.markForCheck(); },
      error: () => { this.service = null; this.cdr.markForCheck(); }
    });
  }

  loadDocuments(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.docService.list(this.serviceId).subscribe({
      next: (res) => { this.documents = res || []; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load required documents'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = { docName: '', mandatory: true, sortOrder: this.documents.length + 1 };
    this.showForm = true;
  }

  openEdit(d: RequiredDocument): void {
    this.editingId = d.id;
    this.form = {
      docName: d.docName,
      description: d.description,
      mandatory: d.mandatory,
      maxAgeDays: d.maxAgeDays,
      allowedFormats: d.allowedFormats,
      sortOrder: d.sortOrder,
    };
    this.showForm = true;
  }

  save(): void {
    const op = this.editingId
      ? this.docService.update(this.serviceId, this.editingId, this.form)
      : this.docService.create(this.serviceId, this.form);
    op.subscribe({
      next: () => {
        this.success = this.editingId ? 'Document updated' : 'Document added';
        this.showForm = false; this.editingId = null;
        this.cdr.markForCheck();
        this.loadDocuments();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save document'; this.cdr.markForCheck(); }
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.docService.delete(this.serviceId, this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Document removed'; this.cdr.markForCheck(); this.loadDocuments(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete document'; this.cdr.markForCheck(); }
    });
  }
}
