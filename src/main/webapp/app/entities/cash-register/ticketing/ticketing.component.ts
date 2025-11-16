import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { CashRegister } from '../model/cash-register.model';
import { Ticketing } from '../model/ticketing.model';
import { CardModule } from 'primeng/card';
import { ActivatedRoute } from '@angular/router';
import { BadgeModule } from 'primeng/badge';
import { KeyFilterModule } from 'primeng/keyfilter';
import { formatNumber } from '../../../shared/util/warehouse-util';
import { CashRegisterService } from '../cash-register.service';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { InputNumber, InputNumberModule } from 'primeng/inputnumber';
import { TagModule } from 'primeng/tag';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';

@Component({
  selector: 'jhi-ticketing',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    TableModule,
    ToolbarModule,
    ReactiveFormsModule,
    CardModule,
    BadgeModule,
    KeyFilterModule,
    ConfirmDialogComponent,
    ToastAlertComponent,
    InputNumberModule,
    TagModule,
    InputGroupModule,
    InputGroupAddonModule,
  ],

  templateUrl: './ticketing-improved.html',
  styleUrls: ['./ticketing-improved.scss'],
})
export class TicketingComponent implements OnInit {
  readonly numberOf10ThousandInput = viewChild<InputNumber>('numberOf10Thousand');
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
    otherAmount: new FormControl<number | null>(null, {}),
  });
  protected selectedCashRegister: CashRegister | null = null;

  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ cashRegister }) => (this.selectedCashRegister = cashRegister));
  }

  onInputChange() {
    this.computeTotalAmount();
  }

  previousState(): void {
    window.history.back();
  }

  showError() {
    this.alert().showError("L'opération n'a pas abouti");
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
      cashRegisterId: this.selectedCashRegister.id,
      id: this.editForm.get(['id']).value,
      numberOf10Thousand: this.editForm.get(['numberOf10Thousand']).value ? this.editForm.get(['numberOf10Thousand']).value : 0,
      numberOf5Thousand: this.editForm.get(['numberOf5Thousand']).value ? this.editForm.get(['numberOf5Thousand']).value : 0,
      numberOf2Thousand: this.editForm.get(['numberOf2Thousand']).value ? this.editForm.get(['numberOf2Thousand']).value : 0,
      numberOf1Thousand: this.editForm.get(['numberOf1Thousand']).value ? this.editForm.get(['numberOf1Thousand']).value : 0,
      numberOf500Hundred: this.editForm.get(['numberOf500Hundred']).value ? this.editForm.get(['numberOf500Hundred']).value : 0,
      otherAmount: this.editForm.get(['otherAmount']).value ? this.editForm.get(['otherAmount']).value : 0,
    };
  }

  protected save(): void {
    const message =
      this.totalAmount === 0
        ? 'Le montant total doit être supérieur à <b>0</b>. Etes-vous sûr de vouloir continuer ?'
        : `le montant total est de <span class="fs-4 badge rounded-pill bg-secondary"><b> ${formatNumber(
            this.totalAmount,
          )}  </b></span> . Etes-vous sûr de vouloir continuer ?`;

    this.confirmTicketing(message);
  }

  private confirmTicketing(message: string): void {
    this.confimDialog().onConfirm(
      () => this.doTicketing(),
      'BILLETAGE',
      message,
      'pi pi-exclamation-triangle',
      () => this.numberOf10ThousandInput().el.nativeElement.focus(),
    );
  }

  private doTicketing(): void {
    this.isSaving = true;
    const ticketing = this.createFromForm();
    this.entityService.doTicketing(ticketing).subscribe({
      next: () => {
        this.isSaving = false;

        this.alert().showInfo('Billetage effectué avec succès');
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
      parseInt(this.editForm.get(['numberOf10Thousand']).value ? this.editForm.get(['numberOf10Thousand']).value : 0) * 10000 +
      parseInt(this.editForm.get(['numberOf5Thousand']).value ? this.editForm.get(['numberOf5Thousand']).value : 0) * 5000 +
      parseInt(this.editForm.get(['numberOf2Thousand']).value ? this.editForm.get(['numberOf2Thousand']).value : 0) * 2000 +
      parseInt(this.editForm.get(['numberOf1Thousand']).value ? this.editForm.get(['numberOf1Thousand']).value : 0) * 1000 +
      parseInt(this.editForm.get(['numberOf500Hundred']).value ? this.editForm.get(['numberOf500Hundred']).value : 0) * 500 +
      parseInt(this.editForm.get(['otherAmount']).value ? this.editForm.get(['otherAmount']).value : 0);
  }
}
