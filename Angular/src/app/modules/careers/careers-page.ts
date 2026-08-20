import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface JobCard {
  id: number;
  title: string;
  location?: string;
  employmentType?: string;
  remote: boolean;
  deadline?: string;
  departmentName?: string;
}

interface JobDetail extends JobCard {
  description?: string;
  requirements?: string;
  responsibilities?: string;
  salaryMin?: number;
  salaryMax?: number;
  vacancies?: number;
}

interface CareerPageView {
  companyName: string;
  headline?: string;
  about?: string;
  brandColor?: string;
  jobs: JobCard[];
}

/**
 * PUBLIC careers page - candidates browsing /careers/:slug. No auth, no app
 * shell. Uses HttpClient directly: ApiService assumes an authenticated
 * workspace session this page must not require.
 */
@Component({
  selector: 'app-careers-page',
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './careers-page.html',
  styleUrl: './careers-page.scss',
})
export class CareersPage implements OnInit {
  slug = '';
  page?: CareerPageView;
  loading = true;
  notFound = false;

  // Job detail + application
  selectedJob?: JobDetail;
  loadingJob = false;
  applying = false;
  applied = false;
  applyError = '';
  form: any = {};

  // Resume: a real upload (parseable for the ATS match score) by default,
  // with a fallback to pasting an external link (not parseable - see
  // CvScoringService on the backend, which only ever reads its own uploads).
  uploadingResume = false;
  resumeUploadError = '';
  resumeFileName = '';
  useResumeLink = false;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.slug = this.route.snapshot.paramMap.get('slug') || '';
    this.http.get<CareerPageView>(`${environment.apiUrl}/public/careers/${this.slug}`).subscribe({
      next: (page) => {
        this.page = page;
        this.loading = false;
        if (page.brandColor) {
          document.documentElement.style.setProperty('--careers-accent', page.brandColor);
        }
        this.cdr.markForCheck();
      },
      error: () => { this.notFound = true; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  openJob(job: JobCard): void {
    this.loadingJob = true;
    this.applied = false;
    this.applyError = '';
    this.form = {};
    this.selectedJob = { ...job };
    this.http.get<JobDetail>(`${environment.apiUrl}/public/careers/${this.slug}/jobs/${job.id}`).subscribe({
      next: (detail) => { this.selectedJob = detail; this.loadingJob = false; this.cdr.markForCheck(); },
      error: () => { this.loadingJob = false; this.cdr.markForCheck(); },
    });
  }

  closeJob(): void {
    this.selectedJob = undefined;
    this.applied = false;
    this.applyError = '';
    this.uploadingResume = false;
    this.resumeUploadError = '';
    this.resumeFileName = '';
    this.useResumeLink = false;
  }

  onResumeFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadingResume = true;
    this.resumeUploadError = '';
    this.cdr.markForCheck();

    const formData = new FormData();
    formData.append('file', file);
    this.http.post<{ fileUrl: string }>(
      `${environment.apiUrl}/public/careers/${this.slug}/upload-resume`, formData,
    ).subscribe({
      next: (res) => {
        this.uploadingResume = false;
        this.resumeFileName = file.name;
        this.form.resumeUrl = res.fileUrl;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.uploadingResume = false;
        this.resumeUploadError = err?.error?.message || 'Could not upload your resume - please try again';
        input.value = '';
        this.cdr.markForCheck();
      },
    });
  }

  toggleResumeLink(): void {
    this.useResumeLink = !this.useResumeLink;
    this.form.resumeUrl = '';
    this.resumeFileName = '';
    this.resumeUploadError = '';
  }

  apply(): void {
    if (!this.selectedJob || this.applying) return;
    if (!this.form.applicantName?.trim() || !this.form.applicantEmail?.trim()) {
      this.applyError = 'Name and email are required';
      return;
    }
    this.applying = true;
    this.applyError = '';
    this.cdr.markForCheck();
    this.http.post(`${environment.apiUrl}/public/careers/${this.slug}/jobs/${this.selectedJob.id}/apply`, this.form).subscribe({
      next: () => { this.applying = false; this.applied = true; this.cdr.markForCheck(); },
      error: (err) => {
        this.applying = false;
        this.applyError = err?.error?.message || 'Could not submit your application - please try again';
        this.cdr.markForCheck();
      },
    });
  }

  typeLabel(t?: string): string {
    return t ? t.replaceAll('_', '-').toLowerCase().replace(/(^|-)([a-z])/g, (m) => m.toUpperCase()).replace('-', '-') : '';
  }
}
