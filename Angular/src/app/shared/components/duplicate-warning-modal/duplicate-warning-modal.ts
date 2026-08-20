import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DuplicateMatch } from '../../../modules/crm/models/crm.model';

// Informational, non-blocking: the record was already created by the time this
// shows (duplicate detection runs as a nudge, not a gate) - so this only offers
// "go look at the existing one" or "dismiss", never "undo the creation".
@Component({
  selector: 'app-duplicate-warning-modal',
  imports: [RouterLink],
  templateUrl: './duplicate-warning-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DuplicateWarningModal {
  @Input() match: DuplicateMatch | null = null;
  @Output() dismissed = new EventEmitter<void>();

  matchedOnLabel(): string {
    const on = this.match?.matchedOn || '';
    return on.charAt(0).toUpperCase() + on.slice(1);
  }
}
