import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IPaymentMode } from 'app/shared/model/payment-mode.model';

@Component({
  selector: 'jhi-payment-mode-detail',
  templateUrl: './payment-mode-detail.component.html',
})
export class PaymentModeDetailComponent implements OnInit {
  paymentMode: IPaymentMode | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ paymentMode }) => (this.paymentMode = paymentMode));
  }

  previousState(): void {
    window.history.back();
  }
}
