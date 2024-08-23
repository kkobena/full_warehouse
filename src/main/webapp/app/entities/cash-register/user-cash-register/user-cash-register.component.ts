import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CashRegisterService } from '../cash-register.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfigurationService } from '../../../shared/configuration.service';
import { CashRegister, CashRegisterStatut } from '../model/cash-register.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { SortDirective } from '../../../shared/sort';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { DialogModule } from 'primeng/dialog';
import { left } from '@popperjs/core';
import { TicketingComponent } from '../ticketing/ticketing.component';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'jhi-user-cash-register',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    PanelModule,
    ButtonModule,
    RouterModule,
    SortDirective,
    CardModule,
    TableModule,
    ReactiveFormsModule,
    KeyFilterModule,
    InputTextModule,
    RippleModule,
    DialogModule,
    TicketingComponent,
    ToastModule,
    ConfirmDialogModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './user-cash-register.component.html',
})
export class UserCashRegisterComponent implements OnInit {
  @ViewChild('cashFundAmountInput') cashFundAmountInput: ElementRef;
  protected overtureCaisseAuto: boolean = false;
  protected isSaving = false;
  protected openCaisse: boolean = false;
  protected cashFundAmount: number | null = null;
  protected cashRegisters: CashRegister[] = [];
  protected selectedCashRegister: CashRegister | null = null;
  protected editForm = this.fb.group({
    cashFundAmount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0), Validators.max(100000)],
      nonNullable: true,
    }),
  });

  protected readonly left = left;
  protected readonly OPEN = CashRegisterStatut.OPEN;
  protected readonly VALIDETED = CashRegisterStatut.VALIDETED;
  protected readonly PENDING = CashRegisterStatut.PENDING;
  protected readonly CLOSED = CashRegisterStatut.CLOSED;

  constructor(
    protected entityService: CashRegisterService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private messageService: MessageService,
    private configService: ConfigurationService,
    protected modalService: ConfirmationService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.configService.find('APP_CASH_FUND').subscribe(res => {
      if (res.body) {
        const otherValue = res.body.otherValue;
        if (otherValue) {
          this.cashFundAmount = parseInt(otherValue);
        }
        this.overtureCaisseAuto = res.body.value === '1';
        this.editForm.get(['cashFundAmount'])!.setValue(this.cashFundAmount);
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
          this.messageService.add({
            severity: 'success',
            summary: 'Billetage ',
            detail: 'Billetage effectué avec succès',
          });
          this.fetchCashRegisters();
        }
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Billetage',
          detail: 'Impossible de faire le ticketing',
        });
      },
    });
  }

  protected openCashRegister(): void {
    if (this.editForm.valid) {
      this.entityService.openCashRegister({ cashFundAmount: this.editForm.get(['cashFundAmount'])!.value }).subscribe({
        next: res => {
          if (res.body) {
            this.messageService.add({
              severity: 'success',
              summary: 'Ouverture de caisse',
              detail: 'Caisse ouverte avec succès',
            });
            this.openCaisse = false;
            this.fetchCashRegisters();
          }
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Ouverture de caisse',
            detail: "Impossible d'ouvrir la caisse",
          });
        },
      });
    }
  }

  protected previousState(): void {
    window.history.back();
  }

  protected onOpenCashRegister(): void {
    this.openCaisse = true;

    setTimeout(() => {
      this.cashFundAmountInput.nativeElement.focus();
      this.editForm.get(['cashFundAmount'])!.setValue(this.cashFundAmount);
      this.cashFundAmountInput.nativeElement.select();
    }, 50);
  }

  protected closeCashRegister(cashRegister: CashRegister): void {
    this.modalService.confirm({
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text ',
      rejectLabel: 'Non',
      acceptLabel: 'Oui',
      message: 'Êtes-vous sûr de vouloir fermer cette caisse sans billetage ?',
      accept: () => {
        this.entityService.closeCashRegister(cashRegister.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Fermeture de caisse',
              detail: 'Caisse fermée avec succès',
            });
            this.fetchCashRegisters();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Fermeture de caisse',
              detail: 'Impossible de fermer la caisse',
            });
          },
        });
      },
    });
  }
}
