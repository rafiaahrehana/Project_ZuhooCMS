import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortalService, MyCompany, UpdateMyCompanyRequest } from '../../../portal/portal.service';
import { Loader } from '../../../../shared/components/loader/loader';

@Component({
  selector: 'app-company-settings',
  imports: [CommonModule, FormsModule, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './company-settings.html',
})
export class CompanySettings implements OnInit {
  loading = false;
  saving = false;
  error = '';
  success = '';

  form: UpdateMyCompanyRequest = { locationDetail: {} };

  /** The form lives in a modal; the page itself is a read-only summary. */
  editing = false;

  readonly monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'];

  monthName(m?: number | null): string {
    return m ? this.monthNames[m - 1] : this.monthNames[0];
  }

  /** One-line address for the summary card. */
  get addressSummary(): string {
    const l = this.form.locationDetail || {};
    return [l.streetAddress, l.level3, l.level2, l.level1, l.postalCode, l.country]
      .filter(Boolean).join(', ');
  }

  openEdit(): void {
    this.success = '';
    this.error = '';
    this.editing = true;
  }

  constructor(private portalService: PortalService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.portalService.getMyCompany().subscribe({
      next: (c: MyCompany) => {
        this.form = {
          companyName: c.companyName,
          companyPhone: c.companyPhone,
          website: c.website,
          portalAbout: c.portalAbout,
          taxRegistrationNumber: c.taxRegistrationNumber,
          bankName: c.bankName,
          bankAccountName: c.bankAccountName,
          bankAccountNumber: c.bankAccountNumber,
          bankBranch: c.bankBranch,
          fiscalYearStartMonth: c.fiscalYearStartMonth ?? 1,
          baseCurrency: c.baseCurrency ?? 'BDT',
          locationDetail: {
            country: c.locationDetail?.country,
            level1: c.locationDetail?.level1,
            level2: c.locationDetail?.level2,
            level3: c.locationDetail?.level3,
            level4: c.locationDetail?.level4,
            postalCode: c.locationDetail?.postalCode,
            streetAddress: c.locationDetail?.streetAddress,
          },
        };
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load company settings';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();
    this.portalService.updateMyCompany(this.form).subscribe({
      next: () => {
        this.saving = false;
        this.editing = false;
        this.success = 'Company settings saved';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save company settings';
        this.cdr.markForCheck();
      },
    });
  }
}
