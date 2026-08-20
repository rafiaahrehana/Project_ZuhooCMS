import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupportAgent } from '../../models/support.model';
import { AgentService } from '../../services/agent.service';
import { PlatformUserService } from '../../../platform-admin/services/platform-user.service';
import { PlatformUser } from '../../../platform-admin/models/platform-admin.model';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-agents',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './agents.html',
})
export class Agents implements OnInit {
  agents: SupportAgent[] = [];
  platformUsers: PlatformUser[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  showForm = false;
  editingId: number | null = null;
  form: any = {};

  constructor(
    private agentService: AgentService,
    private userService: PlatformUserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadUsers();
  }

  editingAgentName = '';

  openCreate(): void {
    this.editingId = null;
    this.editingAgentName = '';
    this.form = { maxConcurrentTickets: 10, status: 'ACTIVE' };
    this.showForm = true;
    this.error = '';
    this.cdr.markForCheck();
  }

  openEdit(a: SupportAgent): void {
    this.editingId = a.id;
    this.editingAgentName = a.fullName || a.email || 'Agent #' + a.id;
    this.form = {
      userId: a.userId,
      department: a.department || '',
      specialization: a.specialization || '',
      maxConcurrentTickets: a.maxConcurrentTickets || 10,
      status: a.status || 'ACTIVE'
    };
    this.showForm = true;
    this.error = '';
    this.cdr.markForCheck();
  }

  loadUsers(): void {
    this.userService.list(0, 100).subscribe({
      next: (res) => {
        this.platformUsers = res.content || [];
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.agentService.list(this.page).subscribe({
      next: (res) => {
        this.agents = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load agents';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    this.form.status = this.form.status || 'ACTIVE';
    this.error = '';

    if (this.editingId) {
      const payload = {
        department: this.form.department || '',
        specialization: this.form.specialization || '',
        maxConcurrentTickets: Number(this.form.maxConcurrentTickets) || 10,
        status: this.form.status
      };
      this.agentService.update(this.editingId, payload).subscribe({
        next: () => {
          this.showForm = false;
          this.editingId = null;
          this.editingAgentName = '';
          this.form = {};
          this.success = 'Agent updated successfully';
          this.cdr.markForCheck();
          this.load();
        },
        error: (err) => {
          this.error = err?.error?.message || 'Failed to update support agent';
          this.cdr.markForCheck();
        },
      });
    } else {
      if (!this.form.userId) {
        this.error = 'Please select a Platform User';
        this.cdr.markForCheck();
        return;
      }
      const payload = {
        userId: Number(this.form.userId),
        department: this.form.department || '',
        specialization: this.form.specialization || '',
        maxConcurrentTickets: Number(this.form.maxConcurrentTickets) || 10,
        status: this.form.status
      };
      this.agentService.create(payload).subscribe({
        next: () => {
          this.showForm = false;
          this.form = {};
          this.success = 'Agent created successfully';
          this.cdr.markForCheck();
          this.load();
        },
        error: (err) => {
          this.error = err?.error?.message || 'Failed to create support agent';
          this.cdr.markForCheck();
        },
      });
    }
  }

  toggleAccepting(a: SupportAgent): void {
    this.agentService.setAccepting(a.id, !a.acceptingTickets).subscribe({
      next: () => this.load(),
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  statusClass(s: string): string {
    return (
      {
        ACTIVE: 'text-bg-success',
        INACTIVE: 'text-bg-secondary',
        ON_BREAK: 'text-bg-warning',
        OFFLINE: 'text-bg-dark',
      }[s] || 'text-bg-secondary'
    );
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
