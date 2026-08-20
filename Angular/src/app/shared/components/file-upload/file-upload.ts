import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadResult, FileUploadService } from '../../services/file-upload.service';

const IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];

@Component({
  selector: 'app-file-upload',
  imports: [CommonModule],
  templateUrl: './file-upload.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FileUpload {
  @Input() label = 'Upload File';
  @Input() accept = '';
  // 'avatar' hits the images-only /upload/avatar endpoint (server-verifies real
  // image content, 5MB cap); 'file' hits the general /upload endpoint.
  @Input() variant: 'file' | 'avatar' = 'file';
  // Client-side size check, purely for fast feedback - the server enforces its
  // own limit regardless (5MB for avatars, the global multipart cap otherwise).
  @Input() maxSizeMB = 5;
  // Applies the same image-extension/MIME check 'avatar' gets, but still posts
  // to the general /upload endpoint - for callers that want "images only" (e.g.
  // a screenshot attachment) without this actually being a profile avatar.
  @Input() imagesOnly = false;
  @Output() uploaded = new EventEmitter<FileUploadResult>();

  uploading = false;
  error = '';
  lastResult: FileUploadResult | null = null;

  constructor(private uploadService: FileUploadService, private cdr: ChangeDetectorRef) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const validationError = this.validate(file);
    if (validationError) {
      this.error = validationError;
      this.lastResult = null;
      input.value = '';
      this.cdr.markForCheck();
      return;
    }

    this.uploading = true;
    this.error = '';
    this.cdr.markForCheck();
    const upload$ = this.variant === 'avatar' ? this.uploadService.uploadAvatar(file) : this.uploadService.upload(file);
    upload$.subscribe({
      next: (result) => {
        this.uploading = false;
        this.lastResult = result;
        this.uploaded.emit(result);
        input.value = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.uploading = false;
        this.error = err?.error?.message || 'Upload failed';
        input.value = '';
        this.cdr.markForCheck();
      },
    });
  }

  // Fast client-side feedback before spending a round trip - the server is the
  // real source of truth (extension whitelist + content-type cross-check +,
  // for avatars, decoding the bytes to confirm they're a genuine image).
  private validate(file: File): string | null {
    if (this.maxSizeMB && file.size > this.maxSizeMB * 1024 * 1024) {
      return `File is too large. Maximum size is ${this.maxSizeMB}MB`;
    }
    if (this.variant === 'avatar' || this.imagesOnly) {
      const name = file.name.toLowerCase();
      const hasImageExtension = IMAGE_EXTENSIONS.some((ext) => name.endsWith(ext));
      const isImageType = !file.type || file.type.startsWith('image/');
      if (!hasImageExtension || !isImageType) {
        return 'Please choose an image file (JPG, PNG, GIF, or WEBP)';
      }
    }
    return null;
  }
}
