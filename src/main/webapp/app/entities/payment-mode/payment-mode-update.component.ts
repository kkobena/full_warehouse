import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';

import { IPaymentMode, PaymentMode } from 'app/shared/model/payment-mode.model';
import { PaymentModeService } from './payment-mode.service';

@Component({
  selector: 'jhi-payment-mode-update',
  templateUrl: './payment-mode-update.component.html',
})
export class PaymentModeUpdateComponent implements OnInit {
  isSaving = false;

  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    code: [null, [Validators.required]],
    group: [null, [Validators.required]],
  });

  constructor(protected paymentModeService: PaymentModeService, protected activatedRoute: ActivatedRoute, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ paymentMode }) => {
      this.updateForm(paymentMode);
    });
  }

  updateForm(paymentMode: IPaymentMode): void {
    this.editForm.patchValue({
      id: paymentMode.id,
      libelle: paymentMode.libelle,
      code: paymentMode.code,
      group: paymentMode.group,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const paymentMode = this.createFromForm();
    if (paymentMode.id !== undefined) {
      this.subscribeToSaveResponse(this.paymentModeService.update(paymentMode));
    } else {
      this.subscribeToSaveResponse(this.paymentModeService.create(paymentMode));
    }
  }

  private createFromForm(): IPaymentMode {
    return {
      ...new PaymentMode(),
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
      code: this.editForm.get(['code'])!.value,
      group: this.editForm.get(['group'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IPaymentMode>>): void {
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
}
