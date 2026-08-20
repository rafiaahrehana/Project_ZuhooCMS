import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AverageRating, ServiceReview } from '../../models/servicedesk.model';
import { ServiceReviewService } from '../../services/service-review.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-service-reviews',
  imports: [CommonModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reviews.html',
})
export class Reviews implements OnInit {
  // VARIABLES
  reviews: ServiceReview[] = [];
  average: AverageRating | null = null;
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  deleteTarget: ServiceReview | null = null;

  constructor(private reviewService: ServiceReviewService, private cdr: ChangeDetectorRef) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void {
    this.load();
    this.loadAverage();
  }

  // LOAD REVIEWS
  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.reviewService.list(this.page).subscribe({
      next: (res) => { this.reviews = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load reviews'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // LOAD OVERALL AVERAGE
  loadAverage(): void {
    this.reviewService.averageRating().subscribe({
      next: (res) => { this.average = res; this.cdr.markForCheck(); },
      error: () => { this.average = null; this.cdr.markForCheck(); }
    });
  }

  // DELETE REVIEW
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.reviewService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Review deleted'; this.cdr.markForCheck(); this.load(); this.loadAverage(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete review'; this.cdr.markForCheck(); }
    });
  }

  // PAGINATION
  goToPage(p: number): void { this.page = p; this.load(); }

  // STARS RENDERING
  stars(rating: number): number[] { return Array(Math.max(0, Math.min(5, rating))).fill(0); }
  emptyStars(rating: number): number[] { return Array(Math.max(0, 5 - Math.min(5, rating))).fill(0); }
}
