import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { Delivery, IDelivery } from 'app/shared/model/delevery.model';
import { ICommande } from 'app/shared/model/commande.model';
import { DeliveryService } from '../../../../../entities/commande/delevery/delivery.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import type { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ButtonComponent, CardComponent, InputNumberComponent, KeyFilterDirective } from 'app/shared/ui';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';

@Component({
  selector: 'app-form-delivery',
  templateUrl: './delivery-modal.component.html',
  styleUrls: ['./form-delevery.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    ButtonComponent,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    CardComponent,
    KeyFilterDirective,
    PharmaDatePickerComponent,
    NgxSpinnerModule,
    InputNumberComponent,
  ]
})
export class DeliveryModalComponent implements OnInit {
  header = '';
  commande: ICommande;
  isEdit = false;
  protected isSaving = false;
  protected blockSpace: RegExp = /^[a-zA-Z0-9_\-]+$/;
  protected fb = inject(FormBuilder);
  protected maxDate: NgbDateStruct | null = null;
  protected minDate: NgbDateStruct | null = null;
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    receiptReference: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    receiptDate: new FormControl<NgbDateStruct | null>(null, {
      validators: [Validators.required],
    }),
    receiptAmount: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true,
    }),
    taxAmount: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true,
    }),
  });
  private readonly entityService = inject(DeliveryService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.maxDate = DeliveryModalComponent.toNgbDate(new Date());
    this.updateForm(this.commande);
    this.minDate = DeliveryModalComponent.toNgbDate(new Date(this.commande.createdAt));
    this.editForm
      .get('receiptAmount')
      .setValidators([Validators.min(this.commande.grossAmount), Validators.max(this.commande.grossAmount)]);
    this.editForm.get('receiptAmount').updateValueAndValidity();
  }

  private static toNgbDate(date: Date): NgbDateStruct {
    return { year: date.getFullYear(), month: date.getMonth() + 1, day: date.getDate() };
  }

  updateForm(entity: ICommande): void {
    this.editForm.patchValue({
      id: entity.id,
      receiptReference: entity.receiptReference,
      receiptAmount: entity.grossAmount,
      taxAmount: entity.taxAmount,
      receiptDate: entity.receiptDate ? DeliveryModalComponent.toNgbDate(new Date(entity.receiptDate)) : null,
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
        }),
      )
      .subscribe({
        next: (res: HttpResponse<IDelivery>) => this.onSaveSuccess(res.body),
        error: err => this.onSaveError(err),
      });
  }

  private onSaveSuccess(response: IDelivery | null): void {
    this.activeModal.close(response);
  }

  private onSaveError(err: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
  }

  private createFrom(): IDelivery {
    return {
      ...new Delivery(),
      id: this.commande.id,
      commandeId: this.commande.commandeId,
      receiptReference: this.editForm.get(['receiptReference']).value,
      receiptDate: NGB_DATE_TO_ISO(this.editForm.get('receiptDate').value),
      receiptAmount: this.editForm.get(['receiptAmount']).value,
      taxAmount: this.editForm.get(['taxAmount']).value,
      orderReference: this.commande.orderReference,
    };
  }
}
