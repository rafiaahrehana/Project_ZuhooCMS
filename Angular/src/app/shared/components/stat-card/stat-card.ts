import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

export type StatCardVariant = 'primary' | 'purple' | 'success' | 'info' | 'dark' | 'danger';

@Component({
  selector: 'app-stat-card',
  imports: [RouterLink],
  templateUrl: './stat-card.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './stat-card.scss',
})
export class StatCard {
  @Input() label = '';
  @Input() value: string | number | null = 0;
  @Input() icon = 'bi-graph-up';
  @Input() variant: StatCardVariant = 'primary';
  @Input() sub = '';
  @Input() link = '';
  @Input() queryParams: Record<string, string> | null = null;
}
