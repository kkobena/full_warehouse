import { AfterViewInit, Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { Divider } from 'primeng/divider';
import { InputNumber } from 'primeng/inputnumber';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';

import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../../../entities/mode-payments/mode-payment.service';
import { DATE_FORMAT_ISO_DATE } from '../../../../shared/util/warehouse-util';

import { IDiffere, INewReglementDiffere } from '../../data-access/models';

@Component({
  selector: 'app-reglement-differe-form',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    Divider,
    InputNumber,
    InputGroup,
    InputGroupAddon,
    Select,
    InputText,
    Button,
    DatePicker,
  ],
  templateUrl: './reglement-differe-form.component.html',
  styleUrl: './reglement-differe-form.component.scss',
})
export class ReglementDiffereFormComponent implements AfterViewInit {
  readonly CASH = 'CASH';
  readonly CH = 'CH';
  readonly VIR = 'VIREMENT';

  readonly isSaving = input(false);
  readonly differe = input<IDiffere | null>(null);

  readonly reglementParams = output<INewReglementDiffere>();
  readonly rendu = output<number>();

  readonly maxDate = new Date();
  protected paymentModes: IPaymentMode[] = [];

  protected montantSaisi = signal(0);
  protected validMontantSaisi = computed(() => this.montantSaisi() > 0);

  private readonly fb = inject(FormBuilder);
  private readonly modeService = inject(ModePaymentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly reglementForm = this.fb.group({
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(5), Validators.max(10_000_000)],
      nonNullable: true,
    }),
    modePaimentCode: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
    paymentDate: new FormControl<Date | null>(null),
    banqueInfo: this.fb.group({
      nom: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
      code: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
      beneficiaire: new FormControl<string | null>(null),
    }),
  });

  get banqueInfo(): FormGroup {
    return this.reglementForm.get('banqueInfo') as FormGroup;
  }

  get cashInput(): AbstractControl<number | null> {
    return this.reglementForm.get('amount');
  }

  get valid(): boolean {
    return this.reglementForm.valid;
  }

  get isCash(): boolean {
    return this.reglementForm.get('modePaimentCode').value === this.CASH;
  }

  get showBanqueInfo(): boolean {
    const code = this.reglementForm.get('modePaimentCode').value;
    return code === this.CH || code === this.VIR;
  }

  private get initTotalAmount(): number {
    return this.differe()?.rest ?? 0;
  }

  ngAfterViewInit(): void {
    this.modeService
      .query()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<IPaymentMode[]>) => {
        if (res.body) {
          this.paymentModes = res.body;
          this.setDefaultMode();
          // Initialise le montant dès que les modes sont chargés (remplace setTimeout)
          this.reglementForm.get('amount').setValue(this.initTotalAmount);
        }
      });

    this.reglementForm.get('paymentDate').setValue(this.maxDate);

    this.reglementForm
      .get('amount')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.montantSaisi.set(value ?? 0);
        const m = (value ?? 0) - this.initTotalAmount;
        this.rendu.emit(m > 0 ? m : 0);
      });

    this.reglementForm
      .get('modePaimentCode')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.reglementForm.get('amount').setValue(this.initTotalAmount);
        if (this.showBanqueInfo) {
          this.banqueInfo.get('nom').setValidators([Validators.required]);
          this.banqueInfo.get('nom').updateValueAndValidity();
          if (value === this.CH) {
            this.banqueInfo.get('code').setValidators([Validators.required]);
          } else {
            this.banqueInfo.get('code').clearValidators();
          }
          this.banqueInfo.get('code').updateValueAndValidity();
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
    this.reglementForm.get('paymentDate').setValue(this.maxDate);
    this.setDefaultMode();
  }

  protected save(): void {
    this.reglementParams.emit(this.buildParams());
  }

  private setDefaultMode(): void {
    const cash = this.paymentModes.find(m => m.code === this.CASH);
    if (cash) {
      this.reglementForm.get('modePaimentCode').setValue(cash.code);
    }
  }

  private buildParams(): INewReglementDiffere {
    const d = this.differe();
    return {
      amount: this.reglementForm.get('amount').value,
      paimentMode: this.reglementForm.get('modePaimentCode').value,
      banqueInfo: this.showBanqueInfo ? this.banqueInfo.getRawValue() : null,
      paymentDate: DATE_FORMAT_ISO_DATE(this.reglementForm.get('paymentDate').value),
      expectedAmount: this.initTotalAmount,
      customerId: d?.customerId,
      saleIds: d?.differeItems?.map(e => e.saleId) ?? [],
    };
  }
}
