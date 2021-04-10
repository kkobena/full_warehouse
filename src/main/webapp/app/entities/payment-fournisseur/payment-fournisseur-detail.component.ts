import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';

@Component({
  selector: 'jhi-payment-fournisseur-detail',
  templateUrl: './payment-fournisseur-detail.component.html',
})
export class PaymentFournisseurDetailComponent implements OnInit {
  paymentFournisseur: IPaymentFournisseur | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ paymentFournisseur }) => (this.paymentFournisseur = paymentFournisseur));
  }

  previousState(): void {
    window.history.back();
  }
}
