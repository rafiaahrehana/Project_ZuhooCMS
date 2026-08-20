import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  CompanyService,
  ServicePrerequisite,
  ServicePrerequisiteRequest,
} from '../../models/servicedesk.model';
import { ServicePrerequisiteService } from '../../services/service-prerequisite.service';
import { CompanyServiceService } from '../../services/company-service.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-service-prerequisites',
  imports: [CommonModule, FormsModule, RouterLink, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './service-prerequisites.html',
})
export class ServicePrerequisites implements OnInit {
  serviceId!: number;
  service: CompanyService | null = null;
  prerequisites: ServicePrerequisite[] = [];
  // Every other active service in the company - the candidate pool for "requires X first".
  otherServices: CompanyService[] = [];
  loading = false;
  error = '';
  success = '';

  showForm = false;
  form: ServicePrerequisiteRequest = { prerequisiteServiceId: 0, mandatory: true };

  deleteTarget: ServicePrerequisite | null = null;

  constructor(
    private route: ActivatedRoute,
    private prerequisiteService: ServicePrerequisiteService,
    private serviceService: CompanyServiceService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.serviceId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadService();
    this.loadPrerequisites();
    this.serviceService.listActive().subscribe({
      next: (res) => {
        this.otherServices = (res || []).filter((s) => s.id !== this.serviceId);
        this.cdr.markForCheck();
      },
    });
  }

  loadService(): void {
    this.serviceService.getById(this.serviceId).subscribe({
      next: (res) => { this.service = res; this.cdr.markForCheck(); },
      error: () => { this.service = null; this.cdr.markForCheck(); }
    });
  }

  loadPrerequisites(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.prerequisiteService.list(this.serviceId).subscribe({
      next: (res) => { this.prerequisites = res || []; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load prerequisites'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  openCreate(): void {
    this.form = { prerequisiteServiceId: 0, mandatory: true };
    this.showForm = true;
  }

  save(): void {
    if (!this.form.prerequisiteServiceId) return;
    this.prerequisiteService.create(this.serviceId, this.form).subscribe({
      next: () => {
        this.success = 'Prerequisite added';
        this.showForm = false;
        this.cdr.markForCheck();
        this.loadPrerequisites();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to add prerequisite'; this.cdr.markForCheck(); }
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.prerequisiteService.delete(this.serviceId, this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Prerequisite removed'; this.cdr.markForCheck(); this.loadPrerequisites(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot remove prerequisite'; this.cdr.markForCheck(); }
    });
  }
}
