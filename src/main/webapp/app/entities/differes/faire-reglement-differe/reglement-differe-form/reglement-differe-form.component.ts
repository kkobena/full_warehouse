import { AfterViewInit, Component, computed, inject, input, OnDestroy, output, signal } from '@angular/core';
import { Divider } from 'primeng/divider';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../../mode-payments/mode-payment.service';
import { HttpResponse } from '@angular/common/http';
import moment from 'moment';
import { DATE_FORMAT } from '../../../../shared/constants/input.constants';
import { Differe } from '../../model/differe.model';
import { NewReglementDiffere } from '../../model/new-reglement-differe.model';
import { InputNumber } from 'primeng/inputnumber';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';
import { FloatLabel } from 'primeng/floatlabel';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { Button } from 'primeng/button';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DatePicker } from 'primeng/datepicker';

@Component({
  selector: 'jhi-reglement-differe-form',
  imports: [
    Divider,
    FormsModule,
    ReactiveFormsModule,
    InputNumber,
    InputGroup,
    InputGroupAddon,
    FloatLabel,
    Select,
    InputText,
    Button,
    DatePicker
  ],
  templateUrl: './reglement-differe-form.component.html'
})
export class ReglementDiffereFormComponent implements AfterViewInit, OnDestroy {
  readonly CASH = 'CASH';
  readonly CH = 'CH';
  readonly VIR = 'VIREMENT';
  readonly isSaving = input(false);
  readonly differe = input<Differe>();
  readonly reglementParams = output<NewReglementDiffere>();
  readonly rendu = output<number>();
  montantSaisi = signal(0);
  validMontantSaisi = computed(() => {
    return this.montantSaisi() > 0;
  });
  protected isValid = true;
  protected appendTo = 'body';
  protected maxDate = new Date();
  protected translate = inject(TranslateService);
  protected primeNGConfig = inject(PrimeNG);
  protected btnLabel = 'Valider';
  protected fb = inject(FormBuilder);
  protected paymentModes: IPaymentMode[] = [];
  protected reglementForm = this.fb.group({
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(5), Validators.max(10000000)],
      nonNullable: true
    }),

    modePaimentCode: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true
    }),

    paymentDate: new FormControl<Date | null>(null, {}),

    banqueInfo: this.fb.group({
      nom: new FormControl<string | null>(null, {
        validators: [Validators.required],
        nonNullable: true
      }),
      code: new FormControl<string | null>(null, {
        validators: [Validators.required],
        nonNullable: true
      }),
      beneficiaire: new FormControl<string | null>(null, {})
    })
  });
  private readonly modeService = inject(ModePaymentService);
  private destroy$ = new Subject<void>();

  get banqueInfo(): FormGroup {
    return this.reglementForm.get('banqueInfo') as FormGroup;
  }

  get valid(): boolean {
    return this.reglementForm.valid;
  }

  get modePaimentCode(): string {
    return this.reglementForm.get('modePaimentCode').value;
  }

  get initTotalAmount(): number {
    return this.differe().rest;
  }

  get defaultDefautInputAmountValue(): number {
    return this.initTotalAmount;
  }

  get isCash(): boolean {
    return this.modePaimentCode === this.CASH;
  }

  get showBanqueInfo(): boolean {
    return this.modePaimentCode === this.CH || this.modePaimentCode === this.VIR;
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
    this.translate
      .stream('primeng')
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        this.primeNGConfig.setTranslation(data);
      });

    this.modeService
      .query()
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<IPaymentMode[]>) => {
        if (res.body) {
          this.paymentModes = res.body;
          this.setDefaultModeReglement();
        }
      });
    this.reglementForm
      .get('amount')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.montantSaisi.set(value);

        const m = value - this.differe().rest;
        if (m < 0) {
          this.rendu.emit(0);
        } else {
          this.rendu.emit(m);
        }
      });
    setTimeout(() => {
      this.reglementForm.get('amount')?.setValue(this.initTotalAmount);
    }, 30);

    this.reglementForm
      .get('modePaimentCode')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.reglementForm.get('amount')?.setValue(this.defaultDefautInputAmountValue);
        if (this.showBanqueInfo) {
          this.banqueInfo.get('nom')?.setValidators([Validators.required]);
          this.banqueInfo.get('nom')?.updateValueAndValidity();
          if (value === this.CH) {
            this.banqueInfo.get('code')?.setValidators([Validators.required]);
            this.banqueInfo.get('code')?.updateValueAndValidity();
          } else {
            this.banqueInfo.get('code')?.clearValidators();
            this.banqueInfo.get('code')?.updateValueAndValidity();
          }
        } else {
          this.banqueInfo.reset();
          this.banqueInfo.get('nom')?.clearValidators();
          this.banqueInfo.get('nom')?.updateValueAndValidity();
          this.banqueInfo.get('code')?.clearValidators();
          this.banqueInfo.get('code')?.updateValueAndValidity();
        }
      });
    this.reglementForm.get('paymentDate')?.setValue(this.maxDate);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reset(): void {
    this.reglementForm.reset();
    this.setDefaultModeReglement();
  }

  protected save(): void {
    this.reglementParams.emit(this.createFromForm());
  }

  protected previousState(): void {
    window.history.back();
  }

  private setDefaultModeReglement(): void {
    this.reglementForm.get('modePaimentCode')?.setValue(this.paymentModes.find(mode => mode.code === this.CASH)?.code);
  }

  private createFromForm(): NewReglementDiffere {
    const formValue = this.reglementForm.value;
    return {
      amount: formValue.amount,
      paimentMode: formValue.modePaimentCode,
      banqueInfo: this.showBanqueInfo ? formValue.banqueInfo : null,
      paymentDate: formValue.paymentDate ? moment(formValue.paymentDate).format(DATE_FORMAT) : null,
      expectedAmount: this.initTotalAmount,
      customerId: this.differe()?.customerId,
      saleIds: this.differe()?.differeItems.map(e => e.saleId)
    };
  }
}
