import { Component, inject, input, signal, viewChild } from '@angular/core';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { TypePrescriptionService } from '../../service/type-prescription.service';
import { UserCaissierService } from '../../service/user-caissier.service';
import { UserVendeurService } from '../../service/user-vendeur.service';
import { SelectModeReglementService } from '../../service/select-mode-reglement.service';
import { VoSalesService } from '../../service/vo-sales.service';
import { CurrentSaleService } from '../../service/current-sale.service';
import { RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { BaseSaleService } from '../../service/base-sale.service';
import { IPayment } from '../../../../shared/model/payment.model';
import { IRemise } from '../../../../shared/model/remise.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { HttpResponse } from '@angular/common/http';
import { FinalyseSale, ISales, SaleId } from '../../../../shared/model/sales.model';
import { finalize, switchMap } from 'rxjs/operators';
import { IClientTiersPayant } from '../../../../shared/model/client-tiers-payant.model';
import { IPaymentMode, PaymentModeControl } from '../../../../shared/model/payment-mode.model';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ProductTableComponent } from '../product-table/product-table.component';
import { Authority } from '../../../../shared/constants/authority.constants';
import { HasAuthorityService } from '../../service/has-authority.service';
import {
  FormActionAutorisationComponent
} from '../../form-action-autorisation/form-action-autorisation.component';
import {
  ConfirmDialogComponent
} from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { CardModule } from 'primeng/card';
import { SpinnerComponent } from '../../../../shared/spinner/spinner.component';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';

@Component({
  templateUrl: './base-sale.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ProductTableComponent,
    ModeReglementComponent,
    AmountComputingComponent,
    DividerModule,
    RouterModule,
    ButtonModule,
    ConfirmDialogComponent,
    CardModule,
    SpinnerComponent,
  ],
  styleUrls: ['./base-sale.scss'],
})
export class BaseSaleComponent {
  modeReglementComponent = viewChild<ModeReglementComponent>('modeReglement');
  amountComputingComponent = viewChild<AmountComputingComponent>('amountComputing');
  readonly isPresale = input(false);
  appendTo = 'body';
  CASH = 'CASH';
  currentSaleService = inject(CurrentSaleService);
  selectedCustomerService = inject(SelectedCustomerService);
  readonly hasAuthorityService = inject(HasAuthorityService);
  readonly canRemoveItem = signal(this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE));
  readonly canApplyDiscount = signal(this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE));

  protected payments: IPayment[] = [];
  protected remise?: IRemise | null;
  protected isSaving = false;
  private typePrescriptionService = inject(TypePrescriptionService);
  private userCaissierService = inject(UserCaissierService);
  private userVendeurService = inject(UserVendeurService);
  private selectModeReglementService = inject(SelectModeReglementService);
  private salesService = inject(VoSalesService);
  private readonly modalService = inject(NgbModal);
  private baseSaleService = inject(BaseSaleService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly tauriPrinterService = inject(TauriPrinterService);
  protected get entryAmount(): number {
    return this.modeReglementComponent()?.getInputSum() || 0;
  }

  manageAmountDiv(): void {
    this.modeReglementComponent()?.manageAmountDiv();
  }

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    const modes = this.selectModeReglementService.modeReglements();
    if (modes.length >= this.baseSaleService.maxModePayementNumber()) {
      const amount = this.entryAmount;
      modes.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        this.currentSaleService.currentSale().amountToBePaid - paymentModeControl.paymentMode.amount;

      this.amountComputingComponent().computeMonnaie(amount);
    } else {
      const inputAmount = Number(paymentModeControl.control.target.value);
      this.amountComputingComponent().computeMonnaie(inputAmount);
      this.modeReglementComponent().manageShowAddButton(inputAmount);
    }
  }

  finalyseSale(putsOnStandby = false): void {
    this.currentSaleService.currentSale().payments = this.modeReglementComponent().buildPayment(this.entryAmount);
    this.currentSaleService.currentSale().type = 'VO';
    this.currentSaleService.currentSale().avoir = this.baseSaleService.isAvoir();
    this.computExtraInfo();
    if (this.isPresale() || putsOnStandby) {
      this.putCurrentSaleOnHold();
    } else {
      this.saveSale();
    }
  }

  putCurrentSaleOnStandBy(): void {
    this.baseSaleService.onStandby();
  }

  onKeyDown(event: any): void {
    this.onCompleteSale();
  }

  onCompleteSale(): void {
    this.baseSaleService.onCompleteSale();
  }

  onLoadPrevente(): void {
    this.modeReglementComponent().buildPreventeReglementInput();
  }

  computExtraInfo(): void {
    this.currentSaleService.currentSale().commentaire = this.modeReglementComponent().commentaire || null;
  }

  save(): void {
    this.isSaving = true;
    const sale = this.currentSaleService.currentSale();
    if (sale.amountToBePaid > 0) {
      const entryAmount = this.entryAmount;
      const restToPay = sale.amountToBePaid - entryAmount;
      sale.montantVerse = this.baseSaleService.getCashAmount(entryAmount);
      if (restToPay > 0 && !sale.differe) {
        this.differeConfirmDialog();
      } else {
        this.finalyseSale();
      }
    } else {
      sale.montantVerse = 0;
      this.finalyseSale();
    }
  }

  differeConfirmDialog(): void {
    this.confimDialog().onConfirm(
      () => {
        this.currentSaleService.currentSale().differe = true;
        this.finalyseSale();
      },
      'Vente différé',
      'Voullez-vous regler le reste en différé ?',
      null,
      () => {},
    );
  }

  saveSale(): void {
    const sale = this.currentSaleService.currentSale();
    const entryAmount = this.entryAmount;
    const restToPay = sale.amountToBePaid - entryAmount;
    sale.payrollAmount = Math.min(entryAmount, sale.amountToBePaid);
    sale.restToPay = Math.max(restToPay, 0);
    sale.montantRendu = sale.montantVerse - sale.amountToBePaid;
    this.spinner().show();
    this.isSaving = true;
    this.salesService
      .save(sale)
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<FinalyseSale>) => this.baseSaleService.onFinalyseSuccess(res.body),
        error: err => this.baseSaleService.onFinalyseError(err),
      });
  }

  putCurrentSaleOnHold(): void {
    this.spinner().show();
    this.isSaving = true;
    this.salesService
      .putCurrentOnStandBy(this.currentSaleService.currentSale())
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<FinalyseSale>) => this.baseSaleService.onFinalyseSuccess(res.body, true),
        error: err => this.baseSaleService.onFinalyseError(err),
      });
  }

  create(salesLine: ISalesLine, tiersPayants: IClientTiersPayant[]): void {
    this.spinner().show();
    this.isSaving = true;
    this.salesService
      .create(
        this.baseSaleService.createSale(
          salesLine,
          tiersPayants,
          this.typePrescriptionService.typePrescription().code,
          this.userCaissierService.caissier().id,
          this.userVendeurService.vendeur().id,
          this.selectedCustomerService.selectedCustomerSignal().id,
          this.currentSaleService.typeVo(),
        ),
      )
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaleResponseSuccess(res.body),
        error: (err: any) => this.baseSaleService.onSaveError(err, this.currentSaleService.currentSale()),
      });
  }

  onAddProduit(salesLine: ISalesLine): void {
    this.spinner().show();
    const sale = this.currentSaleService.currentSale();
    this.isSaving = true;
    this.salesService
      .addItem({
        ...salesLine,
        saleCompositeId: sale.saleId,
      })
      .pipe(
        switchMap((res: HttpResponse<ISalesLine>) => this.salesService.find(sale.saleId)),
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaveSuccess(res.body),
        error: (err: any) => this.baseSaleService.onSaveError(err, this.currentSaleService.currentSale()),
      });
  }

  removeLine(salesLine: ISalesLine): void {
    this.spinner().show();
    this.isSaving = true;
    const sale = this.currentSaleService.currentSale();
    this.salesService
      .deleteItem(salesLine.saleLineId)
      .pipe(
        switchMap(() => this.salesService.find(sale.saleId)),
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaveSuccess(res.body),
        error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      });
  }

  openActionAutorisationDialog(privilege: string, entityToProccess: any): void {
    const modalRef = this.modalService.open(FormActionAutorisationComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.entity = this.currentSaleService.currentSale();
    modalRef.componentInstance.privilege = privilege;
    modalRef.closed.subscribe(reason => {
      if (reason === true) {
        this.removeLine(entityToProccess);
      }
    });
  }

  confirmDeleteItem(item: ISalesLine): void {
    if (item) {
      if (this.canRemoveItem()) {
        this.removeLine(item);
      } else {
        this.openActionAutorisationDialog(Authority.PR_SUPPRIME_PRODUIT_VENTE, item);
      }
    } else {
      this.baseSaleService.setInputBoxFocus('produitBox');
    }
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    this.spinner().show();
    this.isSaving = true;
    const sale = this.currentSaleService.currentSale();
    this.salesService
      .updateItemQtyRequested({
        ...salesLine,
        saleCompositeId: sale.saleId,
      })
      .pipe(
        switchMap(() => this.salesService.find(sale.saleId)),
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaveSuccess(res.body),
        error: (err: any) => {
          if (['stock', 'stockChInsufisant'].includes(err.error?.errorKey)) {
            this.baseSaleService.onStockError(err, salesLine);
          } else {
            this.baseSaleService.onSaveError(err, sale);
          }
        },
      });
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    this.spinner().show();
    this.isSaving = true;
    const sale = this.currentSaleService.currentSale();
    this.salesService
      .updateItemQtySold({
        ...salesLine,
        saleCompositeId: sale.saleId,
      })
      .pipe(
        switchMap(() => this.salesService.find(sale.saleId)),
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaveSuccess(res.body),
        error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      });
  }

  updateItemPrice(salesLine: ISalesLine): void {
    this.spinner().show();
    this.isSaving = true;
    const sale = this.currentSaleService.currentSale();
    this.salesService
      .updateItemPrice({
        ...salesLine,
        saleCompositeId: sale.saleId,
      })
      .pipe(
        switchMap(() => this.salesService.find(sale.saleId)),
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaveSuccess(res.body),
        error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      });
  }

  printInvoice(): void {
    this.salesService.printInvoice(this.currentSaleService.currentSale().saleId).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  print(sale: ISales | null): void {
    this.salesService.print(sale.saleId).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  printSale(saleId: SaleId): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.printReceiptForTauri(saleId);
    } else {
      this.salesService.printReceipt(saleId).subscribe();
    }
  }

  onAddRmiseOpenActionAutorisationDialog(remise: IRemise): void {
    const modalRef = this.modalService.open(FormActionAutorisationComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.entity = this.currentSaleService.currentSale();
    modalRef.componentInstance.privilege = Authority.PR_AJOUTER_REMISE_VENTE;
    modalRef.closed.subscribe(reason => {
      if (reason === true) {
        this.addRemise(remise);
      }
    });
  }

  addRemise(remise: IRemise): void {
    const sale = this.currentSaleService.currentSale();
    const action$ = remise
      ? this.salesService.addRemise({ id: sale.saleId, value: remise.id })
      : this.salesService.removeRemiseFromSale(sale.saleId);

    action$
      .pipe(
        switchMap(() => this.salesService.find(sale.saleId)),
        finalize(() => {
          this.isSaving = false;
          this.spinner().hide();
        }),
      )
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaveSuccess(res.body),
        error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      });
  }

  onAddRemise(remise: IRemise): void {
    if (this.canApplyDiscount()) {
      this.addRemise(remise);
    } else {
      if (remise || this.currentSaleService.currentSale().remise) {
        this.onAddRmiseOpenActionAutorisationDialog(remise);
      }
    }
  }

  printReceiptForTauri(saleId: SaleId, isEdition: boolean = false): void {
    /*  this.spinner().show(); */
    this.salesService
      .getEscPosReceiptForTauri(saleId, isEdition)
      /* .pipe(finalize(() => this.spinner().hide())) */
      .subscribe({
        next: async (escposData: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
          } catch (error) {
            this.baseSaleService.onSaveError(error, this.currentSaleService.currentSale());
          }
        },
        error: err => this.baseSaleService.onSaveError(err, this.currentSaleService.currentSale()),
      });
  }
}
