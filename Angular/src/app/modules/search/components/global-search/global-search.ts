import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  SearchService,
  SearchResultItem,
  AskResponse,
} from '../../../../core/services/search.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-global-search',
  imports: [CommonModule, FormsModule, RouterLink, Loader, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './global-search.html',
})
export class GlobalSearch implements OnInit {
  query = '';
  results: SearchResultItem[] = [];
  askResult?: AskResponse;
  loading = false;
  asking = false;
  error = '';
  searched = false;

  typeIcons: Record<string, string> = {
    LEAD: 'bi-person-plus',
    CLIENT: 'bi-building',
    OPPORTUNITY: 'bi-graph-up-arrow',
    SERVICE_REQUEST: 'bi-ticket',
    TICKET: 'bi-life-preserver',
    INVOICE: 'bi-receipt',
    REFUND: 'bi-arrow-counterclockwise',
  };

  constructor(
    private route: ActivatedRoute,
    private searchService: SearchService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const q = params.get('q');
      const ai = params.get('ai');
      if (q) {
        this.query = q;
        if (ai === 'true') {
          this.askAi();
        } else {
          this.doSearch();
        }
      }
    });
  }

  doSearch(): void {
    if (this.query.trim().length < 2) return;
    this.loading = true;
    this.error = '';
    this.askResult = undefined;
    this.cdr.markForCheck();
    this.searchService.search(this.query.trim()).subscribe({
      next: (res) => {
        this.results = res.results;
        this.searched = true;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Search failed';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  askAi(): void {
    if (this.query.trim().length < 2) return;
    this.asking = true;
    this.error = '';
    this.cdr.markForCheck();
    this.searchService.ask(this.query.trim()).subscribe({
      next: (res) => {
        this.askResult = res;
        this.asking = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'AI answer failed — check your AI provider config';
        this.asking = false;
        this.cdr.markForCheck();
      },
    });
  }

  groupTypes(): string[] {
    return [...new Set(this.results.map((r) => r.type))];
  }

  byType(type: string): SearchResultItem[] {
    return this.results.filter((r) => r.type === type);
  }

  highlight(text: string): string {
    if (!this.query.trim() || !text) return text;
    const escaped = this.query.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(`(${escaped})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }
}
