import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

/**
 * Company payroll policy. The percentages are what a salary structure is
 * normally built from - house rent at 40% of basic and so on - and the
 * per-day/overtime settings are what payroll and the salary sheet price
 * absence and extra hours with.
 */
export interface PayrollSettings {
  perDayBasis: 'CALENDAR_DAYS' | 'FIXED_30' | 'FIXED_26' | 'ACTUAL_WORKING_DAYS';
  absenceDeductionBase: 'BASIC' | 'GROSS';
  overtimeEnabled: boolean;
  overtimeMultiplier: number;
  overtimeBase: 'BASIC' | 'GROSS';
  standardHoursPerDay: number;

  houseRentPercent: number;
  medicalPercent: number;
  transportPercent: number;
  foodPercent: number;
  providentFundPercent: number;
  taxPercent: number;
}

@Injectable({ providedIn: 'root' })
export class PayrollSettingsService {
  private readonly endpoint = '/hr/payroll-settings';

  constructor(private api: ApiService) {}

  get(): Observable<PayrollSettings> {
    return this.api.get<PayrollSettings>(this.endpoint);
  }

  update(payload: PayrollSettings): Observable<PayrollSettings> {
    return this.api.put<PayrollSettings>(this.endpoint, payload);
  }
}
