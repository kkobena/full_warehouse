import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { Delivery, IDelivery } from '../../../../shared/model/delevery.model';
import { ICommande } from '../../../../shared/model/commande.model';
import { DeliveryService } from '../delivery.service';
import moment from 'moment';
import { DATE_FORMAT } from '../../../../shared/constants/input.constants';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { CardModule } from 'primeng/card';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { DatePicker } from 'primeng/datepicker';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SpinerService } from '../../../../shared/spiner.service';
import { ToastAlertComponent } from '../../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../../shared/error.service';

@Component({
  selector: 'jhi-form-delivery',
  templateUrl: './delivery-modal.component.html',
  styleUrls: ['../../../common-modal.component.scss'],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RouterModule,
    TableModule,
    TooltipModule,
    FormsModule,
    ReactiveFormsModule,
    CardModule,
    KeyFilterModule,
    InputTextModule,
    DatePicker,
    ToastAlertComponent
  ]
})
export class DeliveryModalComponent implements OnInit {
  header = '';
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
      nonNullable: true
    }),
    receiptDate: new FormControl<Date | null>(null, {
      validators: [Validators.required]
    }),
    receiptAmount: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true
    }),
    taxAmount: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true
    })
  });
  private readonly entityService = inject(DeliveryService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly spinner = inject(SpinerService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    this.maxDate = new Date();
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
      receiptDate: entity.receiptDate ? new Date(moment(entity.receiptDate).format(DATE_FORMAT)) : null
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
    this.activeModal.dismiss();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDelivery>>): void {
    result
      .pipe(
        finalize(() => {
          this.spinner.hide();
          this.isSaving = false;
        })
      )
      .subscribe({
        next: (res: HttpResponse<IDelivery>) => this.onSaveSuccess(res.body),
        error: (err) => this.onSaveError(err)
      });
  }

  private onSaveSuccess(response: IDelivery | null): void {
    this.activeModal.close(response);
  }

  private onSaveError(err: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(err));
  }

  private createFrom(): IDelivery {
    return {
      ...new Delivery(),
      id: this.commande.id,
      receiptReference: this.editForm.get(['receiptReference']).value,
      receiptDate: this.editForm.get('receiptDate').value ? moment(this.editForm.get('receiptDate').value).format(DATE_FORMAT) : null,
      receiptAmount: this.editForm.get(['receiptAmount']).value,
      taxAmount: this.editForm.get(['taxAmount']).value,
      orderReference: this.commande.orderReference
    };
  }
}
