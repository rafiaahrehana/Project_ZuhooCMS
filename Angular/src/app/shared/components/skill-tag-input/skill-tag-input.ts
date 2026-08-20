import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { RecruitmentSkillService } from '../../../modules/hrm/services/recruitment-skill.service';

const DEBOUNCE_MS = 250;

/**
 * Chip list + text box for a comma-separated skill field (JobPosting.requiredSkills/
 * preferredSkills). Value is the plain CSV string the backend expects - no
 * separate array model to keep in sync. Suggestions come from whatever this
 * company has already typed elsewhere (see RecruitmentSkillService); typing a
 * skill that isn't suggested still works - Enter/comma commits it as free text.
 */
@Component({
  selector: 'app-skill-tag-input',
  imports: [CommonModule, FormsModule],
  templateUrl: './skill-tag-input.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkillTagInput {
  @Input() label = 'Skills';
  @Input() placeholder = 'Type a skill and press Enter';
  @Input() value = '';
  @Output() valueChange = new EventEmitter<string>();

  draft = '';
  suggestions: string[] = [];
  showSuggestions = false;

  private query$ = new Subject<string>();

  constructor(private skillService: RecruitmentSkillService, private cdr: ChangeDetectorRef) {
    this.query$
      .pipe(
        debounceTime(DEBOUNCE_MS),
        distinctUntilChanged(),
        switchMap((q) => this.skillService.suggest(q)),
      )
      .subscribe((results) => {
        this.suggestions = results.filter((s) => !this.tags.includes(s));
        this.cdr.markForCheck();
      });
  }

  get tags(): string[] {
    return this.value ? this.value.split(',').map((s) => s.trim()).filter((s) => s.length > 0) : [];
  }

  onInput(): void {
    this.showSuggestions = true;
    this.query$.next(this.draft.trim());
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.commit(this.draft);
    } else if (event.key === 'Backspace' && !this.draft && this.tags.length) {
      this.removeTag(this.tags[this.tags.length - 1]);
    }
  }

  selectSuggestion(skill: string): void {
    this.commit(skill);
  }

  commit(raw: string): void {
    const skill = raw.trim().replace(/,+$/, '');
    if (!skill) return;
    if (!this.tags.some((t) => t.toLowerCase() === skill.toLowerCase())) {
      this.emit([...this.tags, skill]);
    }
    this.draft = '';
    this.suggestions = [];
    this.showSuggestions = false;
  }

  removeTag(skill: string): void {
    this.emit(this.tags.filter((t) => t !== skill));
  }

  onBlur(): void {
    // Small delay so a suggestion click registers before the list hides.
    setTimeout(() => {
      this.showSuggestions = false;
      this.cdr.markForCheck();
    }, 150);
  }

  private emit(tags: string[]): void {
    this.value = tags.join(', ');
    this.valueChange.emit(this.value);
  }
}
