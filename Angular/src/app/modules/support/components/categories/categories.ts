import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PagedResponse } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-categories',
  imports: [CommonModule, FormsModule, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './categories.html',
})
export class Categories implements OnInit {
  categories: any[] = [];
  loading = false;
  error = '';
  success = '';
  showForm = false;
  form: any = {};
  deleteTarget: any = null;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  // NOTE: ApiService already prepends environment.apiUrl, which itself already contains "/api".
  // The backend controller is mapped at "/api/support/categories" (no "/v1"), so the path below
  // must NOT repeat "/api" or add "/v1" - the previous version double-prefixed and 404'd every call.
  private readonly endpoint = '/support/categories';

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    // Backend returns Page<SupportCategoryResponse>, not a plain array
    this.api.getPaged<any>(this.endpoint, 0, 200).subscribe({
      next: (res) => {
        this.categories = res.content;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load categories';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    const op = this.form.id
      ? this.api.patch<any>(`${this.endpoint}/${this.form.id}`, this.form)
      : this.api.post<any>(this.endpoint, this.form);
    op.subscribe({
      next: () => {
        this.showForm = false;
        this.form = {};
        this.success = 'Saved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.api.delete<void>(`${this.endpoint}/${this.deleteTarget.id}`).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Cannot delete';
        this.cdr.markForCheck();
      },
    });
  }
}
