import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Loader } from '../../../shared/components/loader/loader';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { Pagination } from '../../../shared/components/pagination/pagination';

export interface CompanyUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  image?: string;
  role: string;
  active: boolean;
  emailVerified: boolean;
  customRoleName?: string;
  /** OWNER | EMPLOYEE | CLIENT - how the user is attached to this company. */
  membership: string;
  createdAt?: string;
}

@Component({
  selector: 'app-users',
  imports: [CommonModule, FormsModule, Loader, EmptyState, Pagination],
  templateUrl: './users.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Users implements OnInit {
  users: CompanyUser[] = [];
  loading = false;
  error = '';

  keyword = '';
  membership = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;

  readonly membershipFilters = [
    { value: '', label: 'All' },
    { value: 'OWNER', label: 'Owner' },
    { value: 'EMPLOYEE', label: 'Staff' },
    { value: 'CLIENT', label: 'Clients' },
  ];

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    const params: Record<string, string> = {};
    if (this.keyword.trim()) params['keyword'] = this.keyword.trim();
    if (this.membership) params['membership'] = this.membership;

    this.api.getPaged<CompanyUser>('/users', this.page, 20, params).subscribe({
      next: (res: PagedResponse<CompanyUser>) => {
        this.users = res.content;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load users';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  filterBy(value: string): void {
    this.membership = value;
    this.page = 0;
    this.load();
  }

  goToPage(page: number): void {
    this.page = page;
    this.load();
  }

  fullName(u: CompanyUser): string {
    return [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email;
  }

  initial(u: CompanyUser): string {
    return (u.firstName || u.email || 'U').charAt(0).toUpperCase();
  }

  membershipClass(membership: string): string {
    switch (membership) {
      case 'OWNER': return 'is-open';
      case 'EMPLOYEE': return 'is-info';
      default: return 'is-muted';
    }
  }

  membershipLabel(membership: string): string {
    switch (membership) {
      case 'OWNER': return 'Owner';
      case 'EMPLOYEE': return 'Staff';
      case 'CLIENT': return 'Client';
      default: return membership;
    }
  }

  /** The custom role is the meaningful one when set; the base role is the fallback. */
  roleLabel(u: CompanyUser): string {
    if (u.customRoleName) return u.customRoleName;
    return (u.role || '')
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }
}
