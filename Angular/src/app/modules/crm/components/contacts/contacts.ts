import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClientContact } from '../../models/crm.model';
import { ContactService } from '../../services/contact.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

// Global, cross-client Contacts directory - a Contact is always created from
// within a Client's own page (no standalone create here), this is purely a
// searchable "who works where" list across every client.
@Component({
  selector: 'app-contacts',
  imports: [CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState],
  templateUrl: './contacts.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Contacts implements OnInit {
  contacts: ClientContact[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  keyword = '';

  constructor(private contactService: ContactService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.contactService.listAll(this.page, 20, this.keyword.trim() || undefined).subscribe({
      next: (res) => {
        this.contacts = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load contacts';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
