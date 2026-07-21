import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { ErrorService } from '../../../shared/error.service';
import { MvtCaisseServiceService } from '../mvt-caisse-service.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { FinancialTransaction, TypeFinancialTransaction } from '../../cash-register/model/cash-register.model';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../mode-payments/mode-payment.service';
import { getTypeName } from '../mvt-caisse-util';
import { NgbActiveModal, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { NotificationService } from '../../../shared/services/notification.service';
import { PaymentId } from '../../reglement/model/reglement.model';
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from '../../../shared/util/warehouse-util';
import { ButtonComponent, CardComponent, InputNumberComponent, SelectComponent } from '../../../shared/ui';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'jhi-form-transaction',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    InputNumberComponent,
    SelectComponent,
    PharmaDatePickerComponent,
  ],
  templateUrl: './form-transaction.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['../../common-modal.component.scss'],
})
export class FormTransactionComponent implements OnInit {
  isSaving = false;
  isValid = true;
  appendTo = 'body';
  maxDate = TODAY_NGB_DATE();
  header: string | null = null;
  protected errorService = inject(ErrorService);
  protected types: TypeFinancialTransaction[] = [
    TypeFinancialTransaction.ENTREE_CAISSE,
    TypeFinancialTransaction.SORTIE_CAISSE,
    TypeFinancialTransaction.REGLEMENT_DIFFERE,
    TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT,
    TypeFinancialTransaction.REGLMENT_FOURNISSEUR,
  ];
  protected paymentModes: IPaymentMode[] = [];
  private fb = inject(FormBuilder);
  editForm = this.fb.group({
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    paymentMode: new FormControl<IPaymentMode | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    typeFinancialTransaction: new FormControl<TypeFinancialTransaction | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    transactionDate: new FormControl<NgbDateStruct>(TODAY_NGB_DATE()),
    commentaire: new FormControl<string | null>(null, {}),
  });
  private readonly activeModal = inject(NgbActiveModal);
  private mvtCaisseService = inject(MvtCaisseServiceService);
  private modeService = inject(ModePaymentService);
  private readonly notificationService = inject(NotificationService);
  ngOnInit(): void {
    this.modeService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => {
      if (res.body) {
        this.paymentModes =
          res.body.map((paymentMode: IPaymentMode) => {
            return {
              libelle: paymentMode.libelle,
              code: paymentMode.code,
            };
          }) || [];
      }
    });
    this.editForm.get(['paymentMode']).setValue({ code: 'CASH', libelle: 'ESPECE' });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    this.subscribeToSaveResponse(this.mvtCaisseService.create(entity));
  }

  protected createFromForm(): FinancialTransaction {
    return {
      ...new FinancialTransaction(),
      amount: this.editForm.get(['amount']).value,
      paymentMode: this.editForm.get(['paymentMode']).value,
      typeTransaction: getTypeName(this.editForm.get(['typeFinancialTransaction']).value),
      transactionDate: NGB_DATE_TO_ISO(this.editForm.get(['transactionDate']).value),
      commentaire: this.editForm.get(['commentaire']).value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<PaymentId>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(paymentId: PaymentId | null): void {
    this.isSaving = false;
    this.activeModal.close(paymentId);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
