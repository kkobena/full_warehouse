import { AfterViewInit, Component, computed, DestroyRef, inject, Input, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from "@angular/forms";
import { HttpResponse } from "@angular/common/http";
import { NgbActiveModal, NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";

import { IPaymentMode } from "../../../../shared/model/payment-mode.model";
import { ModePaymentService } from "../../../../entities/mode-payments/mode-payment.service";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import {
  ButtonComponent,
  CardComponent,
  InputNumberComponent,
  KeyFilterDirective,
  SelectComponent
} from 'app/shared/ui';
import {
  IDossierFactureProjection,
  ILigneRapprochement,
  IReglementParams,
  ModeEditionReglement
} from "../../data-access/models";

@Component({
  selector: "app-reglement-rapprochement-modal",
  imports: [
    FormsModule,
    ReactiveFormsModule,
    PharmaDatePickerComponent,
    ButtonComponent,
    CardComponent,
    InputNumberComponent,
    KeyFilterDirective,
    SelectComponent
  ],
  templateUrl: "./reglement-rapprochement-modal.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./reglement-rapprochement-modal.component.scss"
})
export class ReglementRapprochementModalComponent implements AfterViewInit {
  @Input({ required: true }) ligne!: ILigneRapprochement;

  readonly CASH = "CASH";
  readonly CH = "CH";
  readonly VIR = "VIREMENT";

  protected readonly typeFacture = ModeEditionReglement.FACTURE_TOTAL;
  readonly activeModal = inject(NgbActiveModal);

  protected paymentModes: IPaymentMode[] = [];
  protected isSaving = false;

  protected montantSaisi = signal(0);

  protected validMontantSaisi = computed(() => this.montantSaisi() >= this.initTotalAmount);

  readonly today = new Date();
  readonly maxDateStruct: NgbDateStruct = {
    year: this.today.getFullYear(),
    month: this.today.getMonth() + 1,
    day: this.today.getDate()
  };

  private readonly fb = inject(FormBuilder);
  private readonly modeService = inject(ModePaymentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly reglementForm = this.fb.group({
    amount: new FormControl<number | null>(null, { validators: [Validators.required], nonNullable: true }),
    modePaimentCode: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
    paymentDate: new FormControl<NgbDateStruct | null>(this.maxDateStruct),
    banqueInfo: this.fb.group({
      nom: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
      code: new FormControl<string | null>(null, { validators: [Validators.required], nonNullable: true }),
      beneficiaire: new FormControl<string | null>(null)
    })
  });

  /** Adapts ILigneRapprochement → IDossierFactureProjection */
  get facture(): IDossierFactureProjection {
    return {
      numFacture: this.ligne.numFacture,
      montantTotal: this.ligne.montantFacture ?? 0,
      montantDetailRegle: this.ligne.montantRegle ?? 0,
      factureItemId: {
        id: this.ligne.factureId!,
        invoiceDate: this.ligne.invoiceDate ?? ""
      }
    };
  }

  get banqueInfo(): FormGroup {
    return this.reglementForm.get("banqueInfo") as FormGroup;
  }


  get showBanqueInfo(): boolean {
    return this.modePaimentCode === this.CH || this.modePaimentCode === this.VIR;
  }

  get valid(): boolean {
    return this.reglementForm.valid;
  }

  private get modePaimentCode(): string {
    return this.reglementForm.get("modePaimentCode").value;
  }

  get initTotalAmount(): number {
    return (this.ligne.montantFacture ?? 0) - (this.ligne.montantRegle ?? 0);
  }

  ngAfterViewInit(): void {
    this.modeService
      .query()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<IPaymentMode[]>) => {
        if (res.body) {
          this.paymentModes = res.body;
          this.setDefaultModeReglement();
          this.reglementForm.get("amount").setValue(this.initTotalAmount);
        }
      });

    this.reglementForm
      .get("amount")
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => this.montantSaisi.set(value ?? 0));

    this.reglementForm
      .get("modePaimentCode")
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        this.reglementForm.get("amount").setValue(this.initTotalAmount);
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

  protected save(): void {
    if (!this.valid || !this.validMontantSaisi()) return;
    this.activeModal.close(this.buildParams());
  }

  private setDefaultModeReglement(): void {
    const defaultMode = this.paymentModes.find(m => m.code === this.CH);
    if (defaultMode) {
      this.reglementForm.get("modePaimentCode").setValue(defaultMode.code);
    }
  }

  private ngbDateToIso(d: NgbDateStruct | null): string | null {
    if (!d) return null;
    return `${d.year}-${String(d.month).padStart(2, "0")}-${String(d.day).padStart(2, "0")}`;
  }

  private buildParams(): IReglementParams {
    const paymentDate = this.reglementForm.get("paymentDate").value as NgbDateStruct | null;
    const f = this.facture;
    return {
      amount: this.reglementForm.get("amount").value,
      modePaimentCode: this.reglementForm.get("modePaimentCode").value,
      partialPayment: false,
      banqueInfo: this.showBanqueInfo ? this.banqueInfo.getRawValue() : null,
      amountToPaid: this.initTotalAmount,
      paymentDate: this.ngbDateToIso(paymentDate),
      totalAmount: this.initTotalAmount,
      id: f.factureItemId,
      montantFacture: f.montantTotal,
      mode: ModeEditionReglement.FACTURE_TOTAL
    };
  }
}
