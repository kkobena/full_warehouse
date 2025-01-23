import { Component, OnInit } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { NgIf } from '@angular/common';
import { ToastModule } from 'primeng/toast';
import { ErrorService } from '../../../shared/error.service';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { MvtCaisseServiceService } from '../mvt-caisse-service.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { FinancialTransaction, TypeFinancialTransaction } from '../../cash-register/model/cash-register.model';
import { DropdownModule } from 'primeng/dropdown';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../mode-payments/mode-payment.service';
import { CalendarModule } from 'primeng/calendar';
import { getTypeName } from '../mvt-caisse-util';
import { TranslateDirective } from '../../../shared/language';
import { InputNumberModule } from 'primeng/inputnumber';

@Component({
    selector: 'jhi-form-transaction',
    imports: [
        FaIconComponent,
        FormsModule,
        InputTextModule,
        KeyFilterModule,
        NgIf,
        ReactiveFormsModule,
        ToastModule,
        TranslateDirective,
        DropdownModule,
        CalendarModule,
        InputNumberModule,
    ],
    templateUrl: './form-transaction.component.html',
    styleUrl: './form-transaction.component.scss'
})
export class FormTransactionComponent implements OnInit {
  isSaving = false;
  isValid = true;
  appendTo = 'body';
  maxDate = new Date();
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
    transactionDate: new FormControl<Date | null>(null, {}),
    commentaire: new FormControl<string | null>(null, {}),
  });

  protected types: TypeFinancialTransaction[] = [
    TypeFinancialTransaction.ENTREE_CAISSE,
    TypeFinancialTransaction.SORTIE_CAISSE,
    TypeFinancialTransaction.REGLEMENT_DIFFERE,
    TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT,
    TypeFinancialTransaction.REGLMENT_FOURNISSEUR,
  ];
  protected paymentModes: IPaymentMode[] = [];

  constructor(
    protected errorService: ErrorService,
    private fb: FormBuilder,
    private ref: DynamicDialogRef,
    private config: DynamicDialogConfig,
    private mvtCaisseService: MvtCaisseServiceService,
    private messageService: MessageService,
    private modeService: ModePaymentService,
  ) {}

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

  protected createFromForm(): FinancialTransaction {
    return {
      ...new FinancialTransaction(),
      amount: this.editForm.get(['amount'])!.value,
      paymentMode: this.editForm.get(['paymentMode'])!.value,
      typeTransaction: getTypeName(this.editForm.get(['typeFinancialTransaction'])!.value),
      transactionDate: this.editForm.get(['transactionDate'])!.value,
      commentaire: this.editForm.get(['commentaire'])!.value,
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
