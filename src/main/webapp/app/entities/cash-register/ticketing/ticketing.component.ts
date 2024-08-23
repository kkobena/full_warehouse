import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { CashRegister } from '../model/cash-register.model';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Ticketing } from '../model/ticketing.model';
import { CardModule } from 'primeng/card';
import { ActivatedRoute } from '@angular/router';
import { BadgeModule } from 'primeng/badge';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { formatNumber } from '../../../shared/util/warehouse-util';
import { CashRegisterService } from '../cash-register.service';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'jhi-ticketing',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    TableModule,
    ToolbarModule,
    ReactiveFormsModule,
    CardModule,
    BadgeModule,
    KeyFilterModule,
    ConfirmDialogModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './ticketing.component.html',
})
export class TicketingComponent implements OnInit, AfterViewInit {
  @ViewChild('numberOf10Thousand') numberOf10ThousandInput: ElementRef;
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
    otherAmount: new FormControl<number | null>(null, {}),
  });
  protected selectedCashRegister: CashRegister | null = null;

  constructor(
    protected activatedRoute: ActivatedRoute,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    protected entityService: CashRegisterService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ cashRegister }) => (this.selectedCashRegister = cashRegister));
  }

  onInputChange() {
    this.computeTotalAmount();
  }

  previousState(): void {
    window.history.back();
  }

  ngAfterViewInit(): void {
    this.numberOf10ThousandInput.nativeElement.focus();
  }

  showError() {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: "L'opération n'a pas abouti",
      life: 3000,
    });
  }

  protected updateForm(ticketing: Ticketing): void {
    this.editForm.patchValue({
      id: ticketing.id,
      numberOf10Thousand: ticketing.numberOf10Thousand,
      numberOf5Thousand: ticketing.numberOf5Thousand,
      numberOf2Thousand: ticketing.numberOf2Thousand,
      numberOf1Thousand: ticketing.numberOf1Thousand,
      numberOf500Hundred: ticketing.numberOf500Hundred,
      otherAmount: ticketing.otherAmount,
    });
  }

  protected createFromForm(): Ticketing {
    return {
      ...new Ticketing(),
      cashRegisterId: this.selectedCashRegister!.id,
      id: this.editForm.get(['id'])!.value,
      numberOf10Thousand: this.editForm.get(['numberOf10Thousand'])!.value ? this.editForm.get(['numberOf10Thousand'])!.value : 0,
      numberOf5Thousand: this.editForm.get(['numberOf5Thousand'])!.value ? this.editForm.get(['numberOf5Thousand'])!.value : 0,
      numberOf2Thousand: this.editForm.get(['numberOf2Thousand'])!.value ? this.editForm.get(['numberOf2Thousand'])!.value : 0,
      numberOf1Thousand: this.editForm.get(['numberOf1Thousand'])!.value ? this.editForm.get(['numberOf1Thousand'])!.value : 0,
      numberOf500Hundred: this.editForm.get(['numberOf500Hundred'])!.value ? this.editForm.get(['numberOf500Hundred'])!.value : 0,
      otherAmount: this.editForm.get(['otherAmount'])!.value ? this.editForm.get(['otherAmount'])!.value : 0,
    };
  }

  protected save(): void {
    if (this.totalAmount === 0) {
      this.confirmationService.confirm({
        message: 'Le montant total doit être supérieur à <b>0</b>. Etes-vous sûr de vouloir continuer ?',
        header: 'Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-text ',
        accept: () => {
          this.doTicketing();
        },
        reject: () => {
          this.numberOf10ThousandInput.nativeElement.focus();
        },
      });
    } else {
      this.confirmationService.confirm({
        message: `le montant total est de <span class="font-size-lg badge rounded-pill bg-secondary"><b> ${formatNumber(
          this.totalAmount,
        )}  </b></span> . Etes-vous sûr de vouloir continuer ?`,
        header: 'Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-text ',
        rejectLabel: 'Non',
        acceptLabel: 'Oui',
        accept: () => {
          this.doTicketing();
        },
        reject: () => {
          this.numberOf10ThousandInput.nativeElement.focus();
        },
      });
    }
  }

  private doTicketing(): void {
    this.isSaving = true;
    const ticketing = this.createFromForm();
    this.entityService.doTicketing(ticketing).subscribe({
      next: () => {
        this.isSaving = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: "L'opération a été effectuée avec succès",
          life: 2000,
        });
        setTimeout(() => {
          this.previousState();
        }, 2000);
      },
      error: () => {
        this.isSaving = false;
        this.showError();
      },
    });
  }

  private computeTotalAmount(): void {
    this.totalAmount =
      parseInt(this.editForm.get(['numberOf10Thousand'])!.value ? this.editForm.get(['numberOf10Thousand'])!.value : 0) * 10000 +
      parseInt(this.editForm.get(['numberOf5Thousand'])!.value ? this.editForm.get(['numberOf5Thousand'])!.value : 0) * 5000 +
      parseInt(this.editForm.get(['numberOf2Thousand'])!.value ? this.editForm.get(['numberOf2Thousand'])!.value : 0) * 2000 +
      parseInt(this.editForm.get(['numberOf1Thousand'])!.value ? this.editForm.get(['numberOf1Thousand'])!.value : 0) * 1000 +
      parseInt(this.editForm.get(['numberOf500Hundred'])!.value ? this.editForm.get(['numberOf500Hundred'])!.value : 0) * 500 +
      parseInt(this.editForm.get(['otherAmount'])!.value ? this.editForm.get(['otherAmount'])!.value : 0);
  }
}
