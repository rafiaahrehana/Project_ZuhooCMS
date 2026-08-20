import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssetImportResult } from '../../models/itam.model';
import { AssetImportService } from '../../services/asset-import.service';

@Component({
  selector: 'app-asset-import',
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './asset-import.html',
})
export class AssetImport {
  uploading = false;
  error = '';
  result: AssetImportResult | null = null;

  constructor(private importService: AssetImportService, private cdr: ChangeDetectorRef) {}

  downloadTemplate(): void {
    this.importService.getTemplateCsv().subscribe({
      next: (csv) => {
        const blob = new Blob([csv], { type: 'text/csv' });
        this.saveBlob(blob, 'asset-import-template.csv');
      },
      error: () => { this.error = 'Failed to download template'; this.cdr.markForCheck(); },
    });
  }

  downloadTemplateXlsx(): void {
    this.importService.getTemplateXlsx().subscribe({
      next: (blob) => this.saveBlob(blob, 'asset-import-template.xlsx'),
      error: () => { this.error = 'Failed to download template'; this.cdr.markForCheck(); },
    });
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploading = true;
    this.error = '';
    this.result = null;
    this.cdr.markForCheck();
    this.importService.importCsv(file).subscribe({
      next: (res) => {
        this.result = res;
        this.uploading = false;
        input.value = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Import failed';
        this.uploading = false;
        input.value = '';
        this.cdr.markForCheck();
      },
    });
  }
}
