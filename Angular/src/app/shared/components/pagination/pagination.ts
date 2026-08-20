import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-pagination',
  imports: [],
  templateUrl: './pagination.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pagination.scss',
})
export class Pagination {
  @Input() page = 0;
  @Input() totalPages = 0;
  @Output() pageChange = new EventEmitter<number>();

  go(p: number): void {
    if (p < 0 || p >= this.totalPages || p === this.page) return;
    this.pageChange.emit(p);
  }
}
