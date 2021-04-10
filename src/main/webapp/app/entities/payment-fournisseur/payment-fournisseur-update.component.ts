import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';

import { IPaymentFournisseur, PaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { PaymentFournisseurService } from './payment-fournisseur.service';
import { ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from 'app/entities/commande/commande.service';
import { IPaymentMode } from 'app/shared/model/payment-mode.model';
import { PaymentModeService } from 'app/entities/payment-mode/payment-mode.service';

type SelectableEntity = ICommande | IPaymentMode;

@Component({
  selector: 'jhi-payment-fournisseur-update',
  templateUrl: './payment-fournisseur-update.component.html',
})
export class PaymentFournisseurUpdateComponent implements OnInit {
  isSaving = false;
  commandes: ICommande[] = [];
  paymentmodes: IPaymentMode[] = [];
  editForm = this.fb.group({
    id: [],
    netAmount: [null, [Validators.required]],
    paidAmount: [null, [Validators.required]],
    restToPay: [null, [Validators.required]],
    createdAt: [null, [Validators.required]],
    updatedAt: [null, [Validators.required]],
    commande: [],
    paymentMode: [],
    dateDimension: [],
  });

  constructor(
    protected paymentFournisseurService: PaymentFournisseurService,
    protected commandeService: CommandeService,
    protected paymentModeService: PaymentModeService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ paymentFournisseur }) => {
      if (!paymentFournisseur.id) {
        const today = moment().startOf('day');
        paymentFournisseur.createdAt = today;
        paymentFournisseur.updatedAt = today;
      }

      this.updateForm(paymentFournisseur);

      this.commandeService.query().subscribe((res: HttpResponse<ICommande[]>) => (this.commandes = res.body || []));
      this.paymentModeService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => (this.paymentmodes = res.body || []));
    });
  }

  updateForm(paymentFournisseur: IPaymentFournisseur): void {
    this.editForm.patchValue({
      id: paymentFournisseur.id,
      netAmount: paymentFournisseur.netAmount,
      paidAmount: paymentFournisseur.paidAmount,
      restToPay: paymentFournisseur.restToPay,
      createdAt: paymentFournisseur.createdAt ? paymentFournisseur.createdAt.format(DATE_TIME_FORMAT) : null,
      updatedAt: paymentFournisseur.updatedAt ? paymentFournisseur.updatedAt.format(DATE_TIME_FORMAT) : null,
      commande: paymentFournisseur.commande,
      paymentMode: paymentFournisseur.paymentMode,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const paymentFournisseur = this.createFromForm();
    if (paymentFournisseur.id !== undefined) {
      this.subscribeToSaveResponse(this.paymentFournisseurService.update(paymentFournisseur));
    } else {
      this.subscribeToSaveResponse(this.paymentFournisseurService.create(paymentFournisseur));
    }
  }

  private createFromForm(): IPaymentFournisseur {
    return {
      ...new PaymentFournisseur(),
      id: this.editForm.get(['id'])!.value,
      netAmount: this.editForm.get(['netAmount'])!.value,
      paidAmount: this.editForm.get(['paidAmount'])!.value,
      restToPay: this.editForm.get(['restToPay'])!.value,
      createdAt: this.editForm.get(['createdAt'])!.value ? moment(this.editForm.get(['createdAt'])!.value, DATE_TIME_FORMAT) : undefined,
      updatedAt: this.editForm.get(['updatedAt'])!.value ? moment(this.editForm.get(['updatedAt'])!.value, DATE_TIME_FORMAT) : undefined,
      commande: this.editForm.get(['commande'])!.value,
      paymentMode: this.editForm.get(['paymentMode'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IPaymentFournisseur>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  trackById(index: number, item: SelectableEntity): any {
    return item.id;
  }
}
