import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { CompanyService, PackageSubscription, ServiceFormField, ServiceRequest, SERVICE_REQUEST_PRIORITIES } from '../../models/servicedesk.model';
import { CreateServiceRequestRequest, ServiceRequestService } from '../../services/service-request.service';
import { CompanyServiceService } from '../../services/company-service.service';
import { ServiceFormFieldService } from '../../services/service-form-field.service';
import { ServicePackageService } from '../../services/service-package.service';
import { AuthService } from '../../../../core/services/auth.service';
import { GatewayPaymentService } from '../../../../core/services/gateway-payment.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { FileUploadResult } from '../../../../shared/services/file-upload.service';

type Tab = 'all' | 'my' | 'assigned';

// Mirror of backend PaymentChoice / PaymentMethod enums
const PAYMENT_CHOICES = ['PAY_NOW', 'PAY_LATER'] as const;
const PAYMENT_METHODS = ['BKASH', 'NAGAD', 'ROCKET', 'SSLCOMMERZ', 'BANK_TRANSFER', 'CASH', 'WALLET', 'CHEQUE'] as const;

@Component({
  selector: 'app-requests',
  imports: [CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, FileUpload],
  templateUrl: './requests.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './requests.scss',
})
export class Requests implements OnInit {
  tab: Tab = 'all';
  requests: ServiceRequest[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  // The backend endpoints are role-split: /service-requests and /assigned-to-me are
  // staff-only (COMPANY_OWNER/EMPLOYEE), while /my resolves the caller's Client
  // record and fails for anyone without one. So clients get only the My tab, and
  // staff get All + Assigned.
  isClient = false;
  tabs: { key: Tab; label: string }[] = [];

  // This table is mounted at both /servicedesk/requests (staff) and /client/requests
  // (client portal). The staff detail route is guarded by SERVICE_REQUEST_VIEW, which
  // clients don't hold - linking there from the portal bounces them to /forbidden, so
  // the row link has to follow the role.
  get detailBasePath(): string {
    return this.isClient ? '/client/requests' : '/servicedesk/requests';
  }

  // Create request (CLIENT only - POST /service-requests is hasRole('CLIENT'))
  showForm = false;
  saving = false;
  form: any = this.emptyForm();
  services: CompanyService[] = [];
  subscriptions: PackageSubscription[] = [];
  priorities = SERVICE_REQUEST_PRIORITIES;
  paymentChoices = PAYMENT_CHOICES;
  paymentMethods = PAYMENT_METHODS;

  // Dynamic form fields defined by the admin for the selected service.
  // Answers are keyed by field id (matches backend validation/storage).
  formFields: ServiceFormField[] = [];
  fieldAnswers: Record<string, string> = {};

  constructor(
    private requestService: ServiceRequestService,
    private companyServiceService: CompanyServiceService,
    private packageService: ServicePackageService,
    private formFieldService: ServiceFormFieldService,
    private auth: AuthService,
    private gatewayPayment: GatewayPaymentService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const roles = this.auth.getCurrentUser()?.roles ?? [];
    this.isClient = roles.includes('CLIENT');
    this.tabs = this.isClient
      ? [{ key: 'my', label: 'My Requests' }]
      : [
          { key: 'all', label: 'All' },
          { key: 'assigned', label: 'Assigned to Me' },
        ];
    this.tab = this.isClient ? 'my' : 'all';
    this.load();

    this.route.queryParams.subscribe(params => {
      if (params['create'] === 'true') {
        const serviceId = params['serviceId'] ? +params['serviceId'] : null;
        this.openCreate(serviceId);
      }
    });
  }

  setTab(tab: Tab): void {
    this.tab = tab;
    this.page = 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    const source =
      this.tab === 'my'
        ? this.requestService.my(this.page)
        : this.tab === 'assigned'
          ? this.requestService.assignedToMe(this.page)
          : this.requestService.list(this.page);
    source.subscribe({
      next: (res) => {
        this.requests = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load requests';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ----- Create request (CLIENT) -----

  private emptyForm(): any {
    return {
      title: '',
      description: '',
      hubServiceId: null,
      priority: 'NORMAL',
      subscriptionId: null,
      paymentChoice: 'PAY_LATER',
      paymentMethod: '',
    };
  }

  openCreate(serviceId?: number | null): void {
    this.form = this.emptyForm();
    if (serviceId) {
      this.form.hubServiceId = serviceId;
    }
    this.formFields = [];
    this.fieldAnswers = {};
    this.showForm = true;
    this.saving = false;
    this.success = '';
    this.error = '';
    
    // We need to load services and subscriptions first so the dropdowns populate.
    // If a serviceId was provided, trigger onServiceChange() after loading services.
    this.companyServiceService.listActive().subscribe({
      next: (res) => {
        this.services = res;
        this.cdr.markForCheck();
        if (this.form.hubServiceId) {
          this.onServiceChange();
        }
      },
      error: () => { this.services = []; this.cdr.markForCheck(); },
    });
    
    if (!this.subscriptions.length) {
      // Requests raised under an ACTIVE subscription consume its quota and cost zero
      this.packageService.mySubscriptions(0, 50).subscribe({
        next: (res) => { this.subscriptions = res.content.filter((s) => s.status === 'ACTIVE'); this.cdr.markForCheck(); },
        error: () => { this.subscriptions = []; this.cdr.markForCheck(); },
      });
    }
  }

  // When the client picks a service, fetch its admin-defined form fields
  onServiceChange(): void {
    this.formFields = [];
    this.fieldAnswers = {};
    if (!this.form.hubServiceId) return;
    this.formFieldService.list(this.form.hubServiceId).subscribe({
      next: (fields) => { this.formFields = fields; this.cdr.markForCheck(); },
      error: () => { this.formFields = []; this.cdr.markForCheck(); },
    });
  }

  // DROPDOWN/RADIO options are stored as a comma-separated list in the field's
  // validationRules (the builder has no dedicated options column)
  fieldOptions(field: ServiceFormField): string[] {
    return (field.validationRules || '')
      .split(',')
      .map((o) => o.trim())
      .filter((o) => o.length > 0);
  }

  // FILE_UPLOAD fields store the uploaded file's URL as their string answer,
  // same as every other field type.
  onFieldFileUploaded(fieldId: number, result: FileUploadResult): void {
    this.fieldAnswers[String(fieldId)] = result.fileUrl;
  }

  save(): void {
    if (!this.form.title?.trim() || !this.form.hubServiceId || !this.form.paymentChoice) {
      this.error = 'Title, service and payment choice are required';
      return;
    }
    // Mirror the backend's required-field validation for immediate feedback
    for (const field of this.formFields) {
      if (field.required && !(this.fieldAnswers[String(field.id)] || '').trim()) {
        this.error = `'${field.label}' is required`;
        return;
      }
    }
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    const answered: Record<string, string> = {};
    for (const [key, value] of Object.entries(this.fieldAnswers)) {
      if (value != null && String(value).trim()) answered[key] = String(value);
    }
    const payload: CreateServiceRequestRequest = {
      title: this.form.title.trim(),
      description: this.form.description || undefined,
      hubServiceId: this.form.hubServiceId,
      priority: this.form.priority || undefined,
      subscriptionId: this.form.subscriptionId ?? undefined,
      paymentChoice: this.form.paymentChoice,
      paymentMethod: this.form.paymentMethod || undefined,
      formData: Object.keys(answered).length ? answered : undefined,
    };
    this.requestService.create(payload).subscribe({
      next: (res) => {
        // A paid, invoiced request sends the client straight to checkout instead of
        // just closing the modal - they came here specifically to pay for this.
        if (this.form.paymentChoice === 'PAY_NOW' && res.invoiceId) {
          this.gatewayPayment.redirectToGateway('INVOICE', res.invoiceId, res.agreedPrice ?? 0, (msg) => {
            this.error = msg;
            this.saving = false;
            this.cdr.markForCheck();
          });
          return;
        }
        this.success = 'Request submitted';
        this.showForm = false;
        this.saving = false;
        this.page = 0;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to submit request';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  isOverdue(request: ServiceRequest): boolean {
    return request.slaBreach;
  }

  statusClass(status: string): string {
    return (
      {
        PENDING: 'text-bg-warning',
        QUOTATION_PENDING: 'text-bg-warning',
        ASSIGNED: 'text-bg-info',
        IN_PROGRESS: 'text-bg-primary',
        WAITING_CLIENT: 'text-bg-warning',
        UNDER_REVIEW: 'text-bg-info',
        COMPLETED: 'text-bg-success',
        REJECTED: 'text-bg-danger',
        CANCELLED: 'text-bg-secondary',
        RESUBMITTED: 'text-bg-info',
      }[status] || 'text-bg-secondary'
    );
  }

  priorityClass(priority: string): string {
    return (
      {
        LOW: 'text-bg-secondary',
        NORMAL: 'text-bg-info',
        HIGH: 'text-bg-warning',
        URGENT: 'text-bg-danger',
      }[priority] || 'text-bg-secondary'
    );
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
