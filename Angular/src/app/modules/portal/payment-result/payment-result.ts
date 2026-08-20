import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-payment-result',
  imports: [CommonModule, RouterLink],
  templateUrl: './payment-result.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './payment-result.scss',
})
export class PaymentResult implements OnInit {
  status = '';
  tranId = '';
  get ok(): boolean { return this.status === 'SUCCESS'; }
  get cancelled(): boolean { return this.status === 'CANCELLED'; }

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.status = this.route.snapshot.queryParamMap.get('status') || 'FAILED';
    this.tranId = this.route.snapshot.queryParamMap.get('tranId') || '';
  }
}
