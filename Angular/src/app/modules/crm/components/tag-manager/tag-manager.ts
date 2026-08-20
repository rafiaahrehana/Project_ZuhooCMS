import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Tag } from '../../models/crm.model';
import { TagService } from '../../services/tag.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

const DEFAULT_COLOR = '#8352ED';

@Component({
  selector: 'app-tag-manager',
  imports: [CommonModule, FormsModule, Loader, ConfirmDialog],
  templateUrl: './tag-manager.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagManager implements OnInit {
  tags: Tag[] = [];
  loading = false;
  error = '';
  success = '';

  newTag: Partial<Tag> = { name: '', color: DEFAULT_COLOR };
  saving = false;

  editingId: number | null = null;
  editForm: Partial<Tag> = {};

  deleteTarget: Tag | null = null;

  constructor(private tagService: TagService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.tagService.list().subscribe({
      next: (tags) => {
        this.tags = tags;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load tags';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  create(): void {
    if (!this.newTag.name?.trim()) return;
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    this.tagService.create({ name: this.newTag.name.trim(), color: this.newTag.color || DEFAULT_COLOR }).subscribe({
      next: () => {
        this.success = 'Tag created';
        this.newTag = { name: '', color: DEFAULT_COLOR };
        this.saving = false;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create tag';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  startEdit(tag: Tag): void {
    this.editingId = tag.id;
    this.editForm = { name: tag.name, color: tag.color };
    this.cdr.markForCheck();
  }

  cancelEdit(): void {
    this.editingId = null;
    this.cdr.markForCheck();
  }

  saveEdit(tag: Tag): void {
    if (!this.editForm.name?.trim()) return;
    this.tagService.update(tag.id, this.editForm).subscribe({
      next: () => {
        this.success = 'Tag updated';
        this.editingId = null;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update tag';
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.tagService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.success = 'Tag deleted';
        this.deleteTarget = null;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete tag';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }
}
