import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupportAuditLog } from '../../models/support.model';
import { AuditLogService } from '../../services/audit-log.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-audit-logs',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './audit-logs.html',
})
export class AuditLogs implements OnInit {
  logs: SupportAuditLog[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';

  actionTypeFilter = '';
  userIdFilter?: number;
  startDate = '';
  endDate = '';

  constructor(private auditService: AuditLogService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    let obs;
    if (this.startDate && this.endDate) {
      obs = this.auditService.byDateRange(this.startDate, this.endDate, this.page, 20);
    } else if (this.actionTypeFilter.trim()) {
      obs = this.auditService.byActionType(this.actionTypeFilter.trim(), this.page, 20);
    } else if (this.userIdFilter) {
      obs = this.auditService.byUser(this.userIdFilter, this.page, 20);
    } else {
      obs = this.auditService.list(this.page, 20);
    }

    obs.subscribe({
      next: (res) => {
        this.logs = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load audit logs';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.actionTypeFilter = '';
    this.userIdFilter = undefined;
    this.startDate = '';
    this.endDate = '';
    this.page = 0;
    this.load();
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
