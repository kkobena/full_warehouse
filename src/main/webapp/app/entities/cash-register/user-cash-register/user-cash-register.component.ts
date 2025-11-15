import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { CashRegisterService } from '../cash-register.service';
import { RouterModule } from '@angular/router';
import { ConfigurationService } from '../../../shared/configuration.service';
import { CashRegister, CashRegisterStatut } from '../model/cash-register.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { left } from '@popperjs/core';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';
import { Tag } from 'primeng/tag';

@Component({
  selector: 'jhi-user-cash-register',
  imports: [
    WarehouseCommonModule,
    PanelModule,
    ButtonModule,
    RouterModule,
    CardModule,
    TableModule,
    ReactiveFormsModule,
    KeyFilterModule,
    InputTextModule,
    ConfirmDialogComponent,
    ToastAlertComponent,
    Tag,
  ],
  templateUrl: './user-cash-register.html',
  styleUrls: ['./user-cash-register.scss'],
})
export class UserCashRegisterComponent implements OnInit, AfterViewInit {
  protected cashFundAmountInput = viewChild<ElementRef>('cashFundAmountInput');
  protected fb = inject(FormBuilder);
  protected overtureCaisseAuto = false;
  protected isSaving = false;
  protected openCaisse = false;
  protected cashFundAmount: number | null = null;
  protected cashRegisters: CashRegister[] = [];
  protected editForm = this.fb.group({
    cashFundAmount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0), Validators.max(1000000)],
      nonNullable: true,
    }),
  });

  protected readonly left = left;
  protected readonly OPEN = CashRegisterStatut.OPEN;
  protected readonly VALIDATED = CashRegisterStatut.VALIDATED;
  protected readonly PENDING = CashRegisterStatut.PENDING;
  protected readonly CLOSED = CashRegisterStatut.CLOSED;
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly entityService = inject(CashRegisterService);
  private readonly configService = inject(ConfigurationService);

  ngAfterViewInit(): void {
    if (this.openCaisse || this.cashRegisters.length === 0) {
      this.setCashFundControlFocus();
    }
  }

  ngOnInit(): void {
    this.configService.find('APP_CASH_FUND').subscribe(res => {
      if (res.body) {
        const otherValue = res.body.otherValue;
        if (otherValue) {
          this.cashFundAmount = parseInt(otherValue);
        }
        this.overtureCaisseAuto = res.body.value === '1';
        this.editForm.get(['cashFundAmount']).setValue(this.cashFundAmount);
      }
    });
    this.fetchCashRegisters();
  }

  protected fetchCashRegisters(): void {
    this.entityService.getConnectedUserNonClosedCashRegisters().subscribe(res => {
      this.cashRegisters = res.body || [];
    });
  }

  protected doTicketing(cashRegister: CashRegister): void {
    this.entityService.doTicketing({ cashRegisterId: cashRegister.id }).subscribe({
      next: res => {
        if (res.body) {
          this.alert().showInfo('Billetage effectué avec succès');
          this.fetchCashRegisters();
        }
      },
      error: () => {
        this.alert().showError('Impossible de faire le ticketing');
      },
    });
  }

  protected openCashRegister(): void {
    if (this.editForm.valid) {
      this.entityService.openCashRegister({ cashFundAmount: this.editForm.get(['cashFundAmount']).value }).subscribe({
        next: res => {
          if (res.body) {
            this.alert().showInfo('Caisse ouverte avec succès');
            this.openCaisse = false;
            this.fetchCashRegisters();
          }
        },
        error: err => {
          this.alert().showError(this.errorService.getErrorMessage(err));
        },
      });
    }
  }

  protected previousState(): void {
    window.history.back();
  }

  protected onOpenCashRegister(): void {
    this.openCaisse = true;
    this.setCashFundControlFocus();
  }

  protected closeCashRegister(cashRegister: CashRegister): void {
    this.confimDialog().onConfirm(
      () => () => {
        this.entityService.closeCashRegister(cashRegister.id).subscribe({
          next: () => {
            this.alert().showInfo('Caisse fermée avec succès');
            this.fetchCashRegisters();
          },
          error: err => {
            this.alert().showError(this.errorService.getErrorMessage(err));
          },
        });
      },
      'Fermeture de caisse',
      'Êtes-vous sûr de vouloir fermer cette caisse sans billetage ?',
      'pi pi-exclamation-triangle',
    );
  }

  protected hasOpingCashRegister(): boolean {
    return this.cashRegisters.some(cr => cr.statut === CashRegisterStatut.OPEN);
  }

  private setCashFundControlFocus(): void {
    setTimeout(() => {
      this.cashFundAmountInput()?.nativeElement.focus();
      this.editForm.get(['cashFundAmount'])?.setValue(this.cashFundAmount);
      this.cashFundAmountInput()?.nativeElement.select();
    }, 100);
  }
}
