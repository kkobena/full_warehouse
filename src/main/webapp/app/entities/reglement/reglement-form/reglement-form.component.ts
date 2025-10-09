import { AfterViewInit, Component, computed, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DossierFactureProjection } from '../model/reglement-facture-dossier.model';
import { ModeEditionReglement, ReglementParams } from '../model/reglement.model';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { INVOICES_STATUT } from '../../../shared/constants/data-constants';
import { HttpResponse } from '@angular/common/http';
import { ModePaymentService } from '../../mode-payments/mode-payment.service';
import { TranslateService } from '@ngx-translate/core';
import { DividerModule } from 'primeng/divider';
import moment from 'moment/moment';
import { DATE_FORMAT } from '../../../shared/constants/input.constants';
import { PrimeNG } from 'primeng/config';
import { DatePicker } from 'primeng/datepicker';
import { Button } from 'primeng/button';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { KeyFilter } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-reglement-form',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    DividerModule,
    DatePicker,
    Button,
    ToggleSwitch,
    InputGroup,
    InputGroupAddon,
    InputText,
    Select,
    KeyFilter,
  ],
  templateUrl: './reglement-form.component.html',
})
export class ReglementFormComponent implements AfterViewInit {
  readonly CASH = 'CASH';
  readonly CH = 'CH';
  readonly VIR = 'VIREMENT';
  readonly isSaving = input(false);
  readonly facture = input<DossierFactureProjection>();
  readonly allSelection = input(false);
  readonly dossierIds = input<number[]>([]);
  readonly montantAPayer = input<number | null>(null);
  readonly typeFacture = input<ModeEditionReglement>();
  readonly partialPayment = output<boolean>();
  readonly reglementParams = output<ReglementParams>();
  isValid = true;
  appendTo = 'body';
  maxDate = new Date();
  translate = inject(TranslateService);
  primeNGConfig = inject(PrimeNG);

  montantSaisi = signal(0);
  validMontantSaisi = computed(() => {
    if (this.isGroup) {
      if (this.allSelection()) {
        return this.montantSaisi() > 0;
      }
      return this.montantSaisi() >= this.montantAPayer();
    } else if (this.allSelection()) {
      return this.montantSaisi() > 0;
    }

    return this.montantSaisi() >= this.montantAPayer();
  });
  monnaie = computed(() => {
    return this.montantAPayer() - this.montantVerse;
  });

  protected paymentModes: IPaymentMode[] = [];
  protected readonly statuts = INVOICES_STATUT;
  private fb = inject(FormBuilder);
  reglementForm = this.fb.group({
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),

    modePaimentCode: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    partialPayment: new FormControl<boolean | null>(true, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    paymentDate: new FormControl<Date | null>(null, {}),

    banqueInfo: this.fb.group({
      nom: new FormControl<string | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
      code: new FormControl<string | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
      beneficiaire: new FormControl<string | null>(null, {}),
    }),
  });
  private modeService = inject(ModePaymentService);

  get banqueInfo(): FormGroup {
    return this.reglementForm.get('banqueInfo') as FormGroup;
  }

  get isReadOnly(): boolean {
    if (this.isGroup) {
      return !this.isPartialPayment;
    }
    return !this.isPartialPayment && this.allSelection();
  }

  get isGroup(): boolean {
    return this.typeFacture() === ModeEditionReglement.GROUP;
  }

  get valid(): boolean {
    if (this.isPartialPayment) {
      return this.dossierIds().length > 0 && this.reglementForm.valid;
    }

    return this.reglementForm.valid;
  }

  get modePaimentCode(): string {
    return this.reglementForm.get('modePaimentCode').value;
  }

  get initTotalAmount(): number {
    const facture = this.facture();
    return facture?.montantTotal - facture?.montantDetailRegle;
  }

  get defaultDefautInputAmountValue(): number {
    if (!this.isPartialPayment) {
      return this.initTotalAmount;
    } else {
      if (this.isCash && this.allSelection()) {
        return this.initTotalAmount;
      } else {
        return this.montantAPayer() || this.initTotalAmount;
      }
    }
  }

  get isCash(): boolean {
    return this.modePaimentCode === this.CASH;
  }

  get showBanqueInfo(): boolean {
    return this.modePaimentCode === this.CH || this.modePaimentCode === this.VIR;
  }

  get isPartialPayment(): boolean {
    return !this.reglementForm.get('partialPayment').value;
  }

  get montantPayer(): number {
    if (this.isPartialPayment && this.allSelection()) {
      return this.initTotalAmount;
    } else {
      return this.montantAPayer() || this.initTotalAmount;
    }
  }

  get montantVerse(): number {
    if (this.isCash) {
      return this.reglementForm.get('amount').value;
    }
    return 0;
  }

  get cashInput(): AbstractControl<number | null> {
    return this.reglementForm.get('amount');
  }

  ngAfterViewInit() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });

    this.modeService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => {
      if (res.body) {
        this.paymentModes = res.body;
        this.setDefaultModeReglement();
      }
    });
    this.reglementForm.get('amount').valueChanges.subscribe(value => {
      this.montantSaisi.set(value);
    });
    setTimeout(() => {
      this.reglementForm.get('amount').setValue(this.initTotalAmount);
    }, 30);

    this.reglementForm.get('partialPayment').valueChanges.subscribe(value => {
      this.partialPayment.emit(!value);
      /* if (value) {
        this.reglementForm.get('amount')?.setValue(this.initTotalAmount);
      } */
    });
    this.reglementForm.get('modePaimentCode').valueChanges.subscribe(value => {
      this.reglementForm.get('amount').setValue(this.defaultDefautInputAmountValue);
      if (this.showBanqueInfo) {
        this.banqueInfo.get('nom').setValidators([Validators.required]);
        this.banqueInfo.get('nom').updateValueAndValidity();
        if (value === this.CH) {
          this.banqueInfo.get('code').setValidators([Validators.required]);
          this.banqueInfo.get('code').updateValueAndValidity();
        } else {
          this.banqueInfo.get('code').clearValidators();
          this.banqueInfo.get('code').updateValueAndValidity();
        }
      } else {
        this.banqueInfo.reset();
        this.banqueInfo.get('nom').clearValidators();
        this.banqueInfo.get('nom').updateValueAndValidity();
        this.banqueInfo.get('code').clearValidators();
        this.banqueInfo.get('code').updateValueAndValidity();
      }
    });
  }

  reset(): void {
    this.reglementForm.reset();
    this.setDefaultModeReglement();
    this.reglementForm.get('partialPayment').setValue(true);
  }

  protected save(): void {
    this.reglementParams.emit(this.createFromForm());
  }

  private setDefaultModeReglement(): void {
    this.reglementForm.get('modePaimentCode').setValue(this.paymentModes.find(mode => mode.code === this.CH).code);
  }

  private createFromForm(): ReglementParams {
    const paymentDate = this.reglementForm.get('paymentDate').value;
    const allMode = this.reglementForm.get('partialPayment').value;
    return {
      amount: this.reglementForm.get('amount').value,
      modePaimentCode: this.reglementForm.get('modePaimentCode').value,
      partialPayment: !allMode,
      banqueInfo: this.showBanqueInfo ? this.banqueInfo.getRawValue() : null,
      amountToPaid: this.montantPayer,
      paymentDate: paymentDate ? moment(paymentDate).format(DATE_FORMAT) : null,
      totalAmount: this.initTotalAmount,
      id: this.facture().factureItemId,
      montantFacture: this.facture().montantTotal,
    };
  }
}
