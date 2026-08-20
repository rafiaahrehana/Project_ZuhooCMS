import { Injectable } from '@angular/core';
import { HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { SKIP_ERROR_TOAST } from '../interceptors/http-context-tokens';

export type GatewayPurpose = 'INVOICE' | 'WALLET_TOPUP' | 'PACKAGE_SUBSCRIPTION' | 'PLATFORM_SUBSCRIPTION';

// SSLCommerz checkout: POST /payments/sslcommerz/initiate returns the
// GatewayPageURL; the caller redirects the browser there. SSLCommerz then
// sends the payer back to /payment-result via the backend callbacks.
@Injectable({ providedIn: 'root' })
export class GatewayPaymentService {
  constructor(private api: ApiService) {}

  initiate(purpose: GatewayPurpose, targetId: number | null, amount: number): Observable<{ gatewayUrl: string }> {
    // The caller (redirectToGateway's onError) already renders this failure inline -
    // skip the global toast so a misconfigured gateway isn't reported twice.
    return this.api.post<{ gatewayUrl: string }>('/payments/sslcommerz/initiate', {
      purpose, targetId, amount,
    }, new HttpContext().set(SKIP_ERROR_TOAST, true));
  }

  redirectToGateway(purpose: GatewayPurpose, targetId: number | null, amount: number, onError: (msg: string) => void): void {
    this.initiate(purpose, targetId, amount).subscribe({
      next: (res) => (window.location.href = res.gatewayUrl),
      error: (err) => onError(err?.error?.message || 'Could not start online payment'),
    });
  }
}
