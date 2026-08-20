import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService, PagedResponse } from '../../../core/services/api.service';
import { Wallet, WalletTransaction, WalletTransactionType } from '../models/finance.model';

@Injectable({ providedIn: 'root' })
export class WalletService {
  private readonly endpoint = '/wallet';
  constructor(private api: ApiService) {}

  getWallet(): Observable<Wallet> {
    return this.api.get<Wallet>(this.endpoint);
  }

  getTransactions(type?: WalletTransactionType, page = 0, size = 20): Observable<PagedResponse<WalletTransaction>> {
    return this.api.getPaged<WalletTransaction>(`${this.endpoint}/transactions`, page, size, type ? { type } : undefined);
  }
}
