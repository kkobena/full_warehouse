import { AfterViewInit, Component, computed, DestroyRef, inject, input, output, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from "@angular/forms";
import { HttpResponse } from "@angular/common/http";
import { DividerModule } from "primeng/divider";
import { DatePicker } from "primeng/datepicker";
import { Button } from "primeng/button";
import { ToggleSwitch } from "primeng/toggleswitch";
import { InputText } from "primeng/inputtext";
import { Select } from "primeng/select";
import { KeyFilter } from "primeng/keyfilter";

import { IPaymentMode } from "../../../../shared/model/payment-mode.model";
import { ModePaymentService } from "../../../../entities/mode-payments/mode-payment.service";
import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";

import { IDossierFactureProjection, IReglementParams, ModeEditionReglement } from "../../data-access/models";

@Component({
  selector: "app-reglement-form",
  imports: [
    FormsModule,
    ReactiveFormsModule,
    DividerModule,
    DatePicker,
    Button,
    ToggleSwitch,
    InputText,
    Select,
    KeyFilter
  ],
  templateUrl: "./reglement-form.component.html",
  styleUrl: "./reglement-form.component.scss"
})
export class ReglementFormComponent implements AfterViewInit {
  readonly CASH = "CASH";
  readonly CH = "CH";
  readonly VIR = "VIREMENT";

  readonly isSaving = input(false);
  readonly facture = input<IDossierFactureProjection | null>(null);
  readonly allSelection = input(false);
  readonly dossierIds = input<number[]>([]);
  readonly montantAPayer = input<number | null>(null);
  readonly typeFacture = input<ModeEditionReglement>();
  /** Set to false to hide the "Tout régler / paiement partiel" toggle (e.g. in rapprochement context) */
  readonly showPartialToggle = input(true);

  readonly partialPayment = output<boolean>();
  readonly reglementParams = output<IReglementParams>();

  protected paymentModes: IPaymentMode[] = [];
  readonly maxDate = new Date();

  protected montantSaisi = signal(0);
  protected validMontantSaisi = computed(() => {
    if (this.allSelection()) {
      return this.montantSaisi() > 0;
    }
    return this.montantSaisi() >= (this.montantAPayer() ?? 0);
  });

  protected monnaie = computed(() => (this.montantAPayer() ?? 0) - this.montantVerse);

  private readonly fb = inject(FormBuilder);
  private readonly modeService = inject(ModePaymentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly reglementForm = this.fb.group({
    amount: new FormControl<number | null>(null, { validators: [Validators.required], nonNullable: true }),
    modePaimentCode: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
    partialPayment: new FormControl<boolean | null>(true, { validators: [Validators.required], nonNullable: true }),
    paymentDate: new FormControl<Date | null>(new Date()),
    banqueInfo: this.fb.group({
      nom: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
      code: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
      beneficiaire: new FormControl<string | null>(null)
    })
  });

  get banqueInfo(): FormGroup {
    return this.reglementForm.get("banqueInfo") as FormGroup;
  }

  get cashInput(): AbstractControl<number | null> {
    return this.reglementForm.get("amount");
  }

  get isGroup(): boolean {
    return this.typeFacture() === ModeEditionReglement.GROUP;
  }

  get isPartialPayment(): boolean {
    return !this.reglementForm.get("partialPayment").value;
  }

  get isCash(): boolean {
    return this.modePaimentCode === this.CASH;
  }

  get showBanqueInfo(): boolean {
    return this.modePaimentCode === this.CH || this.modePaimentCode === this.VIR;
  }

  get isReadOnly(): boolean {
    if (this.isGroup) {
      return !this.isPartialPayment;
    }
    return !this.isPartialPayment && this.allSelection();
  }

  get valid(): boolean {
    if (this.isPartialPayment) {
      return this.dossierIds().length > 0 && this.reglementForm.valid;
    }
    return this.reglementForm.valid;
  }

  private get modePaimentCode(): string {
    return this.reglementForm.get("modePaimentCode").value;
  }

  private get initTotalAmount(): number {
    const f = this.facture();
    return f ? f.montantTotal - f.montantDetailRegle : 0;
  }

  private get defaultInputAmountValue(): number {
    if (!this.isPartialPayment) {
      return this.initTotalAmount;
    }
    if (this.isCash && this.allSelection()) {
      return this.initTotalAmount;
    }
    return this.montantAPayer() ?? this.initTotalAmount;
  }

  private get montantVerse(): number {
    return this.isCash ? (this.reglementForm.get("amount").value ?? 0) : 0;
  }

  private get montantPayer(): number {
    if (this.isPartialPayment && this.allSelection()) {
      return this.initTotalAmount;
    }
    return this.montantAPayer() ?? this.initTotalAmount;
  }

  ngAfterViewInit(): void {
    this.modeService
      .query()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<IPaymentMode[]>) => {
        if (res.body) {
          this.paymentModes = res.body;
          this.setDefaultModeReglement();
          // Initialize amount after payment modes are loaded (replaces setTimeout)
          this.reglementForm.get("amount").setValue(this.initTotalAmount);
        }
      });

    this.reglementForm
      .get("amount")
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => this.montantSaisi.set(value ?? 0));

    this.reglementForm
      .get("partialPayment")
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => this.partialPayment.emit(!value));

    this.reglementForm
      .get("modePaimentCode")
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.reglementForm.get("amount").setValue(this.defaultInputAmountValue);
        if (this.showBanqueInfo) {
          this.banqueInfo.get("nom").setValidators([Validators.required]);
          this.banqueInfo.get("nom").updateValueAndValidity();
          if (value === this.CH) {
            this.banqueInfo.get("code").setValidators([Validators.required]);
          } else {
            this.banqueInfo.get("code").clearValidators();
          }
          this.banqueInfo.get("code").updateValueAndValidity();
        } else {
          this.banqueInfo.reset();
          this.banqueInfo.get("nom").clearValidators();
          this.banqueInfo.get("nom").updateValueAndValidity();
          this.banqueInfo.get("code").clearValidators();
          this.banqueInfo.get("code").updateValueAndValidity();
        }
      });
  }

  reset(): void {
    this.reglementForm.reset();
    this.setDefaultModeReglement();
    this.reglementForm.get("partialPayment").setValue(true);
  }

  protected save(): void {
    this.reglementParams.emit(this.buildParams());
  }

  private setDefaultModeReglement(): void {
    const defaultMode = this.paymentModes.find(m => m.code === this.CH);
    if (defaultMode) {
      this.reglementForm.get("modePaimentCode").setValue(defaultMode.code);
    }
  }

  private buildParams(): IReglementParams {
    const paymentDate = this.reglementForm.get("paymentDate").value;
    const allMode = this.reglementForm.get("partialPayment").value;
    const f = this.facture();
    return {
      amount: this.reglementForm.get("amount").value,
      modePaimentCode: this.reglementForm.get("modePaimentCode").value,
      partialPayment: !allMode,
      banqueInfo: this.showBanqueInfo ? this.banqueInfo.getRawValue() : null,
      amountToPaid: this.montantPayer,
      paymentDate: DATE_FORMAT_ISO_DATE(paymentDate),
      totalAmount: this.initTotalAmount,
      id: f?.factureItemId,
      montantFacture: f?.montantTotal
    };
  }
}
