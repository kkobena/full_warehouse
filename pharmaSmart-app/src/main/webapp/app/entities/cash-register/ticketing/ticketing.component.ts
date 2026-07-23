import { Component, ElementRef, inject, OnInit, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { CashRegister } from "../model/cash-register.model";
import { Ticketing } from "../model/ticketing.model";
import { ActivatedRoute } from "@angular/router";
import { formatNumber } from "../../../shared/util/warehouse-util";
import { CashRegisterService } from "../cash-register.service";
import { CommonModule } from "@angular/common";
import { NgbConfirmDialogService } from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../shared/services/notification.service";
import { ButtonComponent, InputNumberComponent } from "../../../shared/ui";

@Component({
  selector: "app-ticketing",
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    InputNumberComponent
  ],

  templateUrl: "./ticketing-improved.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["./ticketing-improved.scss"]
})
export class TicketingComponent implements OnInit {
  readonly numberOf10ThousandInput = viewChild("numberOf10Thousand", { read: ElementRef<HTMLElement> });
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly entityService = inject(CashRegisterService);
  protected fb = inject(FormBuilder);
  protected isSaving = false;
  protected display = false;
  protected totalAmount = 0;
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    numberOf10Thousand: new FormControl<number | null>(null, {}),
    numberOf5Thousand: new FormControl<number | null>(null, {}),
    numberOf2Thousand: new FormControl<number | null>(null, {}),
    numberOf1Thousand: new FormControl<number | null>(null, {}),
    numberOf500Hundred: new FormControl<number | null>(null, {}),
    otherAmount: new FormControl<number | null>(null, {})
  });
  protected selectedCashRegister: CashRegister | null = null;
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ cashRegister }) => (this.selectedCashRegister = cashRegister));
    this.editForm.valueChanges.subscribe(() => this.computeTotalAmount());
  }

  previousState(): void {
    window.history.back();
  }

  showError() {
    this.notificationService.error("L'opération n'a pas abouti");
  }

  protected updateForm(ticketing: Ticketing): void {
    this.editForm.patchValue({
      id: ticketing.id,
      numberOf10Thousand: ticketing.numberOf10Thousand,
      numberOf5Thousand: ticketing.numberOf5Thousand,
      numberOf2Thousand: ticketing.numberOf2Thousand,
      numberOf1Thousand: ticketing.numberOf1Thousand,
      numberOf500Hundred: ticketing.numberOf500Hundred,
      otherAmount: ticketing.otherAmount
    });
  }

  protected createFromForm(): Ticketing {
    return {
      ...new Ticketing(),
      cashRegisterId: this.selectedCashRegister.id,
      id: this.editForm.get(["id"]).value,
      numberOf10Thousand: this.editForm.get(["numberOf10Thousand"]).value ? this.editForm.get(["numberOf10Thousand"]).value : 0,
      numberOf5Thousand: this.editForm.get(["numberOf5Thousand"]).value ? this.editForm.get(["numberOf5Thousand"]).value : 0,
      numberOf2Thousand: this.editForm.get(["numberOf2Thousand"]).value ? this.editForm.get(["numberOf2Thousand"]).value : 0,
      numberOf1Thousand: this.editForm.get(["numberOf1Thousand"]).value ? this.editForm.get(["numberOf1Thousand"]).value : 0,
      numberOf500Hundred: this.editForm.get(["numberOf500Hundred"]).value ? this.editForm.get(["numberOf500Hundred"]).value : 0,
      otherAmount: this.editForm.get(["otherAmount"]).value ? this.editForm.get(["otherAmount"]).value : 0
    };
  }

  protected save(): void {
    const message =
      this.totalAmount === 0
        ? "Le montant total doit être supérieur à <b>0</b>. Etes-vous sûr de vouloir continuer ?"
        : `le montant total est de <span class="fs-4 badge rounded-pill bg-secondary"><b> ${formatNumber(
          this.totalAmount
        )}  </b></span> . Etes-vous sûr de vouloir continuer ?`;

    this.confirmTicketing(message);
  }

  private confirmTicketing(message: string): void {
    this.confirmDialog.onConfirm(
      () => this.doTicketing(),
      "BILLETAGE",
      message,
      "pi pi-exclamation-triangle",
      () => this.numberOf10ThousandInput()?.nativeElement.querySelector('input')?.focus()
    );
  }

  private doTicketing(): void {
    this.isSaving = true;
    const ticketing = this.createFromForm();
    this.entityService.doTicketing(ticketing).subscribe({
      next: () => {
        this.isSaving = false;

        this.notificationService.success("Billetage effectué avec succès");
        setTimeout(() => {
          this.previousState();
        }, 2000);
      },
      error: () => {
        this.isSaving = false;
        this.showError();
      }
    });
  }

  private computeTotalAmount(): void {
    this.totalAmount =
      parseInt(this.editForm.get(["numberOf10Thousand"]).value ? this.editForm.get(["numberOf10Thousand"]).value : 0) * 10000 +
      parseInt(this.editForm.get(["numberOf5Thousand"]).value ? this.editForm.get(["numberOf5Thousand"]).value : 0) * 5000 +
      parseInt(this.editForm.get(["numberOf2Thousand"]).value ? this.editForm.get(["numberOf2Thousand"]).value : 0) * 2000 +
      parseInt(this.editForm.get(["numberOf1Thousand"]).value ? this.editForm.get(["numberOf1Thousand"]).value : 0) * 1000 +
      parseInt(this.editForm.get(["numberOf500Hundred"]).value ? this.editForm.get(["numberOf500Hundred"]).value : 0) * 500 +
      parseInt(this.editForm.get(["otherAmount"]).value ? this.editForm.get(["otherAmount"]).value : 0);
  }
}
