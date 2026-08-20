import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';

export interface CareerPageSettings {
  slug: string;
  headline?: string;
  about?: string;
  brandColor?: string;
  published: boolean;
}

/**
 * Configuration of the PUBLIC careers page. Read-only summary on the page,
 * edit in a modal - the public URL is the thing people come here to copy.
 */
@Component({
  selector: 'app-career-page',
  imports: [CommonModule, FormsModule, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './career-page.html',
})
export class CareerPage implements OnInit {
  settings?: CareerPageSettings;
  loading = false;
  saving = false;
  error = '';
  success = '';
  editing = false;
  form: CareerPageSettings = { slug: '', published: true };
  copied = false;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loading = true;
    this.api.get<CareerPageSettings>('/hr/career-page').subscribe({
      next: (s) => { this.settings = s; this.loading = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load career page settings';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get publicUrl(): string {
    return this.settings ? `${location.origin}/careers/${this.settings.slug}` : '';
  }

  openEdit(): void {
    if (!this.settings) return;
    this.form = { ...this.settings, brandColor: this.settings.brandColor || '#367C2B' };
    this.error = '';
    this.success = '';
    this.editing = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.api.put<CareerPageSettings>('/hr/career-page', this.form).subscribe({
      next: (s) => {
        this.settings = s;
        this.saving = false;
        this.editing = false;
        this.success = 'Career page saved';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save';
        this.cdr.markForCheck();
      },
    });
  }

  copyLink(): void {
    navigator.clipboard?.writeText(this.publicUrl).then(() => {
      this.copied = true;
      this.cdr.markForCheck();
      setTimeout(() => { this.copied = false; this.cdr.markForCheck(); }, 1500);
    });
  }
}
