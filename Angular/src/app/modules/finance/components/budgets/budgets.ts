import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Budget, BudgetRequest } from '../../models/finance.model';
import { BudgetService } from '../../services/budget.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-budgets',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './budgets.html',
})
export class Budgets implements OnInit, OnDestroy {
  fiscalYear = new Date().getFullYear();
  budgets: Budget[] = [];
  loading = false;
  error = '';
  success = '';

  showForm = false;
  editing = false;
  saving = false;
  editingId: number | null = null;
  form: BudgetRequest = this.emptyForm();
  deleteTarget: Budget | null = null;

  // Cancels any in-flight save when the form is closed/reopened, so a straggler
  // response from a previous (e.g. cancelled mid-flight) save can never resolve
  // against a newly-opened form and leave its button stuck spinning/disabled.
  private cancelSave$ = new Subject<void>();

  constructor(private budgetService: BudgetService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.cancelSave$.next();
    this.cancelSave$.complete();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.budgetService.listForYear(this.fiscalYear).subscribe({
      next: (res) => {
        this.budgets = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load budgets';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  changeYear(delta: number): void {
    this.fiscalYear += delta;
    this.load();
  }

  private emptyForm(): BudgetRequest {
    return { category: '', fiscalYear: this.fiscalYear, amount: 0, notes: '' };
  }

  openCreate(): void {
    this.cancelSave$.next();
    this.form = this.emptyForm();
    this.editing = false;
    this.editingId = null;
    this.showForm = true;
    this.saving = false;
    this.error = '';
  }

  openEdit(budget: Budget): void {
    this.cancelSave$.next();
    this.form = {
      category: budget.category,
      fiscalYear: budget.fiscalYear,
      amount: budget.amount,
      notes: budget.notes ?? '',
    };
    this.editing = true;
    this.editingId = budget.id;
    this.showForm = true;
    this.saving = false;
    this.error = '';
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    const obs = this.editing && this.editingId
      ? this.budgetService.update(this.editingId, this.form)
      : this.budgetService.create(this.form);
    obs.pipe(takeUntil(this.cancelSave$)).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.editing ? 'Budget updated' : 'Budget created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save budget';
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.budgetService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Budget deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete budget';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  progressClass(budget: Budget): string {
    if (budget.usedPercent > 100) return 'bg-danger';
    if (budget.usedPercent >= 80) return 'bg-warning';
    return 'bg-success';
  }

  progressWidth(budget: Budget): number {
    return Math.min(100, Math.max(0, budget.usedPercent || 0));
  }
}
