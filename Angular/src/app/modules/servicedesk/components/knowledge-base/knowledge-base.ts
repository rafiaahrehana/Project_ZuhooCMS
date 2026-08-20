import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { KbArticle } from '../../models/servicedesk.model';
import { KbService } from '../../services/kb.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-knowledge-base',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, HasPermissionDirective],
  templateUrl: './knowledge-base.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './knowledge-base.scss',
})
export class KnowledgeBase implements OnInit {
  articles: KbArticle[] = [];
  selected?: KbArticle;
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  keyword = '';
  statusFilter = '';

  showEditor = false;
  editingId: number | null = null;
  draft: Partial<KbArticle> = {};

  constructor(private kbService: KbService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    const params: any = {};
    if (this.keyword.trim()) params.keyword = this.keyword.trim();
    if (this.statusFilter) params.status = this.statusFilter;
    this.kbService.list(this.page, 20, params).subscribe({
      next: (res) => {
        this.articles = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load articles';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  open(article: KbArticle): void {
    this.kbService.getById(article.id).subscribe({
      next: (a) => { this.selected = a; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load article'; this.cdr.markForCheck(); },
    });
  }

  markHelpful(): void {
    if (!this.selected) return;
    this.kbService.markHelpful(this.selected.id).subscribe({
      next: (a) => { this.selected = a; this.cdr.markForCheck(); },
    });
  }

  publish(article: KbArticle): void {
    this.kbService.publish(article.id).subscribe({ next: () => this.load() });
  }

  archive(article: KbArticle): void {
    this.kbService.archive(article.id).subscribe({ next: () => this.load() });
  }

  startNew(): void {
    this.draft = { clientVisible: false };
    this.editingId = null;
    this.showEditor = true;
    this.selected = undefined;
  }

  startEdit(article: KbArticle): void {
    this.editingId = article.id;
    this.draft = {
      title: article.title,
      summary: article.summary,
      keywords: article.keywords,
      content: article.content,
      clientVisible: article.clientVisible,
    };
    this.showEditor = true;
    this.selected = undefined;
  }

  saveDraft(): void {
    if (!this.draft.title?.trim() || !this.draft.content?.trim()) {
      this.error = 'Title and content are required';
      return;
    }
    const op = this.editingId
      ? this.kbService.update(this.editingId, this.draft)
      : this.kbService.create(this.draft);
    op.subscribe({
      next: () => {
        this.showEditor = false;
        this.draft = {};
        this.editingId = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save article'; this.cdr.markForCheck(); },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
