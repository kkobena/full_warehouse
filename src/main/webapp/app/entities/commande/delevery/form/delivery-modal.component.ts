import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { Delivery, IDelivery } from '../../../../shared/model/delevery.model';
import { ICommande } from '../../../../shared/model/commande.model';
import { DeliveryService } from '../delivery.service';
import moment, { Moment } from 'moment';
import { DATE_FORMAT } from '../../../../shared/constants/input.constants';
import { NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-form-delivery',
  templateUrl: './delivery-modal.component.html',
  providers: [MessageService],
})
export class DeliveryModalComponent implements OnInit {
  isSaving = false;
  entity?: IDelivery;

  commande: ICommande;
  maxDate = new Date();
  minDate = new Date();
  editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    receiptRefernce: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    receiptDate: new FormControl<Date | null>(null, {
      validators: [Validators.required],
    }),
    receiptAmount: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true,
    }),
    taxAmount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0)],
      nonNullable: true,
    }),
    sequenceBon: new FormControl<string | null>(null, {}),
  });

  constructor(
    protected entityService: DeliveryService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private fb: FormBuilder,
    private messageService: MessageService,
    private spinner: NgxSpinnerService
  ) {}

  ngOnInit(): void {
    this.maxDate = new Date();
    this.entity = this.config.data.entity;

    this.commande = this.config.data.commande;

    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.minDate = new Date(moment(this.commande?.createdAt).format(DATE_FORMAT));
    this.editForm
      .get('receiptAmount')!
      .setValidators([Validators.min(this.commande?.grossAmount), Validators.max(this.commande?.grossAmount)]);
    this.editForm.get('receiptAmount')!.updateValueAndValidity();
  }

  updateForm(entity: IDelivery): void {
    this.editForm.patchValue({
      id: entity.id,
      receiptRefernce: entity.receiptRefernce,
      sequenceBon: entity.sequenceBon,
      receiptAmount: entity.receiptAmount,
      taxAmount: entity.taxAmount,
      receiptDate: entity.receiptDate ? new Date(moment(entity.receiptDate).format(DATE_FORMAT)) : null,
    });
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFrom();
    this.spinner.show();
    if (entity.id) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  cancel(): void {
    this.ref.destroy();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDelivery>>): void {
    result.subscribe({
      next: (res: HttpResponse<IDelivery>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
      complete: () => this.spinner.hide(),
    });
  }

  protected onSaveSuccess(response: IDelivery | null): void {
    this.ref.close(response);
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  private createFrom(): IDelivery {
    return {
      ...new Delivery(),
      id: this.editForm.get(['id'])!.value,
      receiptRefernce: this.editForm.get(['receiptRefernce'])!.value,
      receiptFullDate: this.buildDate(this.editForm.get(['receiptDate'])!.value),
      sequenceBon: this.editForm.get(['sequenceBon'])!.value,
      receiptAmount: this.editForm.get(['receiptAmount'])!.value,
      taxAmount: this.editForm.get(['taxAmount'])!.value,
      orderReference: this.commande?.orderRefernce,
    };
  }

  private buildDate(param: any): Moment {
    const receiptDate = new Date(param);
    return moment(receiptDate);
  }
}