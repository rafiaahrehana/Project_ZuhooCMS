import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem, CdkDrag, CdkDropList } from '@angular/cdk/drag-drop';
import { ApplicationStatus, JobApplication } from '../../models/hrm.model';
import { RecruitmentService } from '../../services/recruitment.service';
import { Loader } from '../../../../shared/components/loader/loader';

/**
 * A pipeline column groups one or more literal ApplicationStatus values.
 * `droppable: false` columns are display-only - Offer only moves through the
 * Offers screen (RecruitmentServiceImpl.updateStatus rejects setting an
 * offer sub-status directly), and Hired only through the Hire action.
 */
interface PipelineColumn {
  key: string;
  label: string;
  statuses: ApplicationStatus[];
  /** Status a card takes on when dropped into this column - undefined when not droppable. */
  dropStatus?: ApplicationStatus;
  droppable: boolean;
}

const COLUMNS: PipelineColumn[] = [
  { key: 'APPLIED', label: 'Applied', statuses: ['APPLIED'], dropStatus: 'APPLIED', droppable: true },
  { key: 'SCREENING', label: 'Screening', statuses: ['SCREENING'], dropStatus: 'SCREENING', droppable: true },
  { key: 'SHORTLISTED', label: 'Shortlisted', statuses: ['SHORTLISTED'], dropStatus: 'SHORTLISTED', droppable: true },
  { key: 'INTERVIEW_SCHEDULED', label: 'Interview Scheduled', statuses: ['INTERVIEW_SCHEDULED'], dropStatus: 'INTERVIEW_SCHEDULED', droppable: true },
  { key: 'INTERVIEWED', label: 'Interviewed', statuses: ['INTERVIEWED'], dropStatus: 'INTERVIEWED', droppable: true },
  { key: 'SELECTED', label: 'Selected', statuses: ['SELECTED'], dropStatus: 'SELECTED', droppable: true },
  { key: 'OFFER', label: 'Offer', statuses: ['OFFER_PENDING', 'OFFER_SENT', 'OFFER_ACCEPTED', 'OFFER_REJECTED'], droppable: false },
  { key: 'HIRED', label: 'Hired', statuses: ['HIRED'], droppable: false },
];

@Component({
  selector: 'app-pipeline',
  imports: [CommonModule, RouterLink, DragDropModule, Loader],
  templateUrl: './pipeline.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pipeline implements OnInit {
  readonly columns = COLUMNS;
  board: Record<string, JobApplication[]> = {};
  loading = false;
  error = '';

  constructor(private recruitmentService: RecruitmentService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = '';
    this.recruitmentService.list(0, 500).subscribe({
      next: (res) => {
        this.board = {};
        this.columns.forEach((c) => (this.board[c.key] = []));
        // REJECTED/WITHDRAWN aren't part of the active pipeline - they still
        // show on the full Applications list, just not this working board.
        res.content
          .filter((a) => a.status !== 'REJECTED' && a.status !== 'WITHDRAWN')
          .forEach((a) => {
            const col = this.columns.find((c) => c.statuses.includes(a.status));
            if (col) this.board[col.key].push(a);
          });
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.error = 'Failed to load pipeline'; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  /** Offer and Hired only ever accept cards moved by the app itself (never by drag). */
  canEnter = (drag: CdkDrag, drop: CdkDropList) => {
    const targetKey = drop.id;
    const target = this.columns.find((c) => c.key === targetKey);
    return !!target?.droppable;
  };

  onDrop(event: CdkDragDrop<JobApplication[]>, column: PipelineColumn): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }
    if (!column.droppable || !column.dropStatus) return; // enterPredicate already blocks this, belt and suspenders
    const application = event.previousContainer.data[event.previousIndex];
    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
    this.recruitmentService.updateStatus(application.id, column.dropStatus).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.error = err?.error?.message || 'Failed to move candidate';
        this.cdr.markForCheck();
        this.load(); // revert the optimistic move
      },
    });
  }
}
