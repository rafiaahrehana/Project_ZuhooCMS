import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  imports: [],
  templateUrl: './empty-state.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './empty-state.scss',
})
export class EmptyState {
  @Input() icon = 'bi-inbox';
  @Input() message = 'Nothing here yet.';
}
