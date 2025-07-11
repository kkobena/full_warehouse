import { HttpResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { Delivery, IDelivery } from '../../../../shared/model/delevery.model';
import { ICommande } from '../../../../shared/model/commande.model';
import { DeliveryService } from '../delivery.service';
import moment, { Moment } from 'moment';
import { DATE_FORMAT } from '../../../../shared/constants/input.constants';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { DatePicker } from 'primeng/datepicker';

@Component({
  selector: 'jhi-form-delivery',
  templateUrl: './delivery-modal.component.html',
  providers: [MessageService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RouterModule,
    RippleModule,
    DynamicDialogModule,
    TableModule,
    NgxSpinnerModule,
    TooltipModule,
    FormsModule,
    ReactiveFormsModule,
    CardModule,
    ToastModule,
    KeyFilterModule,
    InputTextModule,
    DatePicker,
  ],
})
export class DeliveryModalComponent implements OnInit {
  commande: ICommande;
  isEdit: boolean = false;
  protected isSaving = false;
  protected fb = inject(FormBuilder);
  protected maxDate = new Date();
  protected minDate = new Date();
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    receiptReference: new FormControl<string | null>(null, {
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
  });
  private readonly entityService = inject(DeliveryService);
  private readonly ref = inject(DynamicDialogRef);
  private readonly config = inject(DynamicDialogConfig);
  private readonly messageService = inject(MessageService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    this.maxDate = new Date();

    this.commande = this.config.data.commande;
    this.updateForm(this.commande);

    this.minDate = new Date(moment(this.commande.createdAt).format(DATE_FORMAT));
    this.editForm
      .get('receiptAmount')
      .setValidators([Validators.min(this.commande.grossAmount), Validators.max(this.commande.grossAmount)]);
    this.editForm.get('receiptAmount').updateValueAndValidity();
  }

  updateForm(entity: ICommande): void {
    this.editForm.patchValue({
      id: entity.id,
      receiptReference: entity.receiptReference,
      receiptAmount: entity.grossAmount,
      taxAmount: entity.taxAmount,
      receiptDate: entity.receiptDate ? new Date(moment(entity.receiptDate).format(DATE_FORMAT)) : null,
    });
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFrom();
    this.spinner.show();
    if (this.isEdit) {
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
    this.spinner.hide();
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  private createFrom(): IDelivery {
    const receiptDate = this.editForm.get('receiptDate').value;
    return {
      ...new Delivery(),
      id: this.commande.id,
      receiptReference: this.editForm.get(['receiptReference']).value,
      receiptDate: receiptDate ? moment(receiptDate).format(DATE_FORMAT) : null,
      receiptAmount: this.editForm.get(['receiptAmount']).value,
      taxAmount: this.editForm.get(['taxAmount']).value,
      orderReference: this.commande.orderReference,
    };
  }

  private buildDate(param: any): Moment {
    const receiptDate = new Date(param);
    return moment(receiptDate);
  }
}
