import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';

import { ToastModule } from 'primeng/toast';
import { ErrorService } from '../../../shared/error.service';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { MvtCaisseServiceService } from '../mvt-caisse-service.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { FinancialTransaction, TypeFinancialTransaction } from '../../cash-register/model/cash-register.model';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../mode-payments/mode-payment.service';
import { getTypeName } from '../mvt-caisse-util';
import { TranslateDirective } from '../../../shared/language';
import { InputNumberModule } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-form-transaction',
  imports: [
    FormsModule,
    InputTextModule,
    KeyFilterModule,
    ReactiveFormsModule,
    ToastModule,
    TranslateDirective,
    InputNumberModule,
    Select,
    DatePicker,
    Button,
  ],
  templateUrl: './form-transaction.component.html',
  styleUrl: './form-transaction.component.scss',
})
export class FormTransactionComponent implements OnInit, AfterViewInit {
  isSaving = false;
  isValid = true;
  appendTo = 'body';
  maxDate = new Date();
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
    transactionDate: new FormControl<Date>(new Date()),
    commentaire: new FormControl<string | null>(null, {}),
  });
  private ref = inject(DynamicDialogRef);
  private mvtCaisseService = inject(MvtCaisseServiceService);
  private messageService = inject(MessageService);
  private modeService = inject(ModePaymentService);

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
  }

  cancel(): void {
    this.ref.close();
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    this.subscribeToSaveResponse(this.mvtCaisseService.create(entity));
  }

  ngAfterViewInit(): void {
    // this.editForm.get(['transactionDate'])!.setValue(new Date());
    this.editForm.get(['paymentMode']).setValue({ code: 'CASH', libelle: 'ESPECE' });
  }

  protected createFromForm(): FinancialTransaction {
    return {
      ...new FinancialTransaction(),
      amount: this.editForm.get(['amount']).value,
      paymentMode: this.editForm.get(['paymentMode']).value,
      typeTransaction: getTypeName(this.editForm.get(['typeFinancialTransaction']).value),
      transactionDate: this.editForm.get(['transactionDate']).value,
      commentaire: this.editForm.get(['commentaire']).value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<FinancialTransaction>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(financialTransaction: FinancialTransaction | null): void {
    this.isSaving = false;
    this.ref.close(financialTransaction);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;

    if (error.error?.errorKey) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: this.errorService.getErrorMessage(error),
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Erreur interne du serveur.',
      });
    }
  }
}
