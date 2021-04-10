import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';

import { IPayment, Payment } from 'app/shared/model/payment.model';
import { PaymentService } from './payment.service';
import { IPaymentMode } from 'app/shared/model/payment-mode.model';
import { PaymentModeService } from 'app/entities/payment-mode/payment-mode.service';
type SelectableEntity = IPaymentMode;

@Component({
  selector: 'jhi-payment-update',
  templateUrl: './payment-update.component.html',
})
export class PaymentUpdateComponent implements OnInit {
  isSaving = false;
  paymentmodes: IPaymentMode[] = [];
  editForm = this.fb.group({
    id: [],
    netAmount: [null, [Validators.required]],
    paidAmount: [null, [Validators.required]],
    restToPay: [null, [Validators.required]],
    createdAt: [null, [Validators.required]],
    updatedAt: [null, [Validators.required]],
    paymentMode: [],
  });

  constructor(
    protected paymentService: PaymentService,
    protected paymentModeService: PaymentModeService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ payment }) => {
      if (!payment.id) {
        const today = moment().startOf('day');
        payment.createdAt = today;
        payment.updatedAt = today;
      }

      this.updateForm(payment);
      this.paymentModeService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => (this.paymentmodes = res.body || []));
    });
  }

  updateForm(payment: IPayment): void {
    this.editForm.patchValue({
      id: payment.id,
      netAmount: payment.netAmount,
      paidAmount: payment.paidAmount,
      restToPay: payment.restToPay,
      createdAt: payment.createdAt ? payment.createdAt.format(DATE_TIME_FORMAT) : null,
      updatedAt: payment.updatedAt ? payment.updatedAt.format(DATE_TIME_FORMAT) : null,
      paymentMode: payment.paymentMode,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const payment = this.createFromForm();
    if (payment.id !== undefined) {
      this.subscribeToSaveResponse(this.paymentService.update(payment));
    } else {
      this.subscribeToSaveResponse(this.paymentService.create(payment));
    }
  }

  private createFromForm(): IPayment {
    return {
      ...new Payment(),
      id: this.editForm.get(['id'])!.value,
      netAmount: this.editForm.get(['netAmount'])!.value,
      paidAmount: this.editForm.get(['paidAmount'])!.value,
      restToPay: this.editForm.get(['restToPay'])!.value,
      createdAt: this.editForm.get(['createdAt'])!.value ? moment(this.editForm.get(['createdAt'])!.value, DATE_TIME_FORMAT) : undefined,
      updatedAt: this.editForm.get(['updatedAt'])!.value ? moment(this.editForm.get(['updatedAt'])!.value, DATE_TIME_FORMAT) : undefined,
      paymentMode: this.editForm.get(['paymentMode'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IPayment>>): void {
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
