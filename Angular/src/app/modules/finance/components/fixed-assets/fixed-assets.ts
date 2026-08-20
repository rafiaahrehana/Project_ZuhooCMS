import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FixedAsset, FixedAssetRequest, FixedAssetStatus, DepreciationRun } from '../../models/finance.model';
import { FixedAssetService } from '../../services/budget.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-fixed-assets',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './fixed-assets.html',
})
export class FixedAssets implements OnInit {
  assets: FixedAsset[] = [];
  runs: DepreciationRun[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  showForm = false;
  saving = false;
  form: FixedAssetRequest = this.emptyForm();
  disposeTarget: FixedAsset | null = null;

  showRunModal = false;
  running = false;
  runYear = new Date().getFullYear();
  runMonth = 1;
  months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

  constructor(private assetService: FixedAssetService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
    this.loadRuns();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.assetService.list(this.page).subscribe({
      next: (res) => {
        this.assets = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load fixed assets';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadRuns(): void {
    this.assetService.listRuns().subscribe({
      next: (res) => { this.runs = res; this.cdr.markForCheck(); },
      error: () => { this.runs = []; this.cdr.markForCheck(); },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  private emptyForm(): FixedAssetRequest {
    return {
      name: '',
      assetTag: '',
      category: '',
      cost: 0,
      salvageValue: 0,
      usefulLifeMonths: 36,
      acquisitionDate: new Date().toISOString().slice(0, 10),
      notes: '',
      postPurchaseToLedger: true,
    };
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.showForm = true;
    this.error = '';
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    this.assetService.create(this.form).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = 'Asset registered';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to register asset';
        this.cdr.markForCheck();
      },
    });
  }

  canDispose(asset: FixedAsset): boolean {
    return asset.status === 'ACTIVE' || asset.status === 'FULLY_DEPRECIATED';
  }

  confirmDispose(): void {
    if (!this.disposeTarget) return;
    this.assetService.dispose(this.disposeTarget.id).subscribe({
      next: () => {
        this.disposeTarget = null;
        this.success = 'Asset disposed - remaining book value written off';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to dispose asset';
        this.disposeTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  openRunModal(): void {
    // Default to last month - the most recently completed period.
    const now = new Date();
    if (now.getMonth() === 0) {
      this.runYear = now.getFullYear() - 1;
      this.runMonth = 12;
    } else {
      this.runYear = now.getFullYear();
      this.runMonth = now.getMonth();
    }
    this.showRunModal = true;
    this.error = '';
  }

  confirmRun(): void {
    if (this.running) return;
    this.running = true;
    this.error = '';
    this.cdr.markForCheck();
    this.assetService.runDepreciation(this.runYear, this.runMonth).subscribe({
      next: (run) => {
        this.running = false;
        this.showRunModal = false;
        this.success = `Depreciation run for ${run.year}-${String(run.month).padStart(2, '0')}: ${run.assetsDepreciated} asset(s) depreciated`;
        this.cdr.markForCheck();
        this.load();
        this.loadRuns();
      },
      error: (err) => {
        this.running = false;
        this.error = err?.error?.message || 'Failed to run depreciation';
        this.cdr.markForCheck();
      },
    });
  }

  statusClass(status: FixedAssetStatus): string {
    return (
      {
        ACTIVE: 'text-bg-success',
        FULLY_DEPRECIATED: 'text-bg-info',
        DISPOSED: 'text-bg-secondary',
      }[status] || 'text-bg-secondary'
    );
  }

  monthLabel(run: DepreciationRun): string {
    return `${run.year}-${String(run.month).padStart(2, '0')}`;
  }
}
