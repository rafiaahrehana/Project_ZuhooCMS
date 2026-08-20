import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Announcement, AnnouncementRequest, ANNOUNCEMENT_AUDIENCES, Department } from '../../models/hrm.model';
import { AnnouncementService } from '../../services/announcement.service';
import { DepartmentService } from '../../services/department.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-announcements',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './announcements.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Announcements implements OnInit {
  announcements: Announcement[] = [];
  departments: Department[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: AnnouncementRequest = this.emptyForm();

  aiInstructions = '';
  draftingAi = false;
  aiDraftError = '';

  deleteTarget: Announcement | null = null;

  audiences = ANNOUNCEMENT_AUDIENCES;

  constructor(
    private announcementService: AnnouncementService,
    private departmentService: DepartmentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadDepartments();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.announcementService.list(this.page, 20).subscribe({
      next: (res) => {
        this.announcements = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load announcements';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadDepartments(): void {
    this.departmentService.listActive().subscribe({
      next: (d) => { this.departments = d; this.cdr.markForCheck(); },
      error: () => { this.departments = []; this.cdr.markForCheck(); }
    });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.selectedId = null;
    this.aiInstructions = '';
    this.aiDraftError = '';
    this.showForm = true;
  }

  openEdit(a: Announcement): void {
    this.form = {
      title: a.title,
      body: a.body,
      audience: a.audience,
      targetDepartmentId: a.targetDepartmentId,
      priority: a.priority,
      expiresAt: a.expiresAt,
      scheduledAt: a.scheduledAt,
      attachmentUrl: a.attachmentUrl,
      notifyAll: a.notifyAll,
    };
    this.isEdit = true;
    this.selectedId = a.id;
    this.aiInstructions = '';
    this.aiDraftError = '';
    this.showForm = true;
  }

  draftWithAi(): void {
    if (!this.aiInstructions.trim() || this.draftingAi) return;
    this.draftingAi = true;
    this.aiDraftError = '';
    this.announcementService.draftWithAi(this.aiInstructions.trim()).subscribe({
      next: (draft) => {
        this.form.title = draft.title;
        this.form.body = draft.body;
        this.draftingAi = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.aiDraftError = err?.error?.message || 'Failed to generate draft';
        this.draftingAi = false;
        this.cdr.markForCheck();
      }
    });
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload = this.cleanPayload();
    const request = this.isEdit && this.selectedId
      ? this.announcementService.update(this.selectedId, payload)
      : this.announcementService.create(payload);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.isEdit ? 'Announcement updated successfully' : 'Announcement created successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save announcement';
        this.cdr.markForCheck();
      }
    });
  }

  publish(a: Announcement): void {
    this.announcementService.publish(a.id).subscribe({
      next: () => {
        this.success = 'Announcement published successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to publish announcement';
        this.cdr.markForCheck();
      }
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.announcementService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Announcement deleted successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete announcement';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  private emptyForm(): AnnouncementRequest {
    return {
      title: '',
      body: '',
      audience: 'ALL',
      priority: 1,
      notifyAll: false,
    };
  }

  private cleanPayload(): AnnouncementRequest {
    const payload: any = { ...this.form };
    if (!payload.targetDepartmentId || payload.targetDepartmentId === 'undefined' || payload.targetDepartmentId === 'null') {
      delete payload.targetDepartmentId;
    }
    if (!payload.expiresAt) {
      delete payload.expiresAt;
    } else if (typeof payload.expiresAt === 'string' && !payload.expiresAt.includes('T')) {
      payload.expiresAt = `${payload.expiresAt}T23:59:59`;
    }
    if (!payload.scheduledAt) delete payload.scheduledAt;
    if (!payload.attachmentUrl) delete payload.attachmentUrl;
    return payload;
  }
}
