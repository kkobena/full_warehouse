import { Component, inject, input, output, signal, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';

import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductTableComponent } from '../product-table/product-table.component';
import { IPaymentMode, PaymentModeControl } from '../../../../shared/model/payment-mode.model';
import { IRemise } from '../../../../shared/model/remise.model';
import { FinalyseSale, InputToFocus, ISales, SaveResponse } from '../../../../shared/model/sales.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { AmountComputingComponent } from './amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { Authority } from '../../../../shared/constants/authority.constants';
import { HasAuthorityService } from '../../service/has-authority.service';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ComptantFacadeService } from './comptant-facade.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormActionAutorisationComponent } from '../../form-action-autorisation/form-action-autorisation.component';
import { CurrentSaleService } from '../../service/current-sale.service';
import { BaseSaleService } from '../../service/base-sale.service';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { SelectModeReglementService } from '../../service/select-mode-reglement.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UninsuredCustomerListComponent } from '../../uninsured-customer-list/uninsured-customer-list.component';

@Component({
  selector: 'jhi-comptant',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
    `,
  ],
  imports: [
    WarehouseCommonModule,
    RouterModule,
    ButtonModule,
    FormsModule,
    DividerModule,
    ProductTableComponent,
    AmountComputingComponent,
    ModeReglementComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './comptant.component.html',
})
export class ComptantComponent {
  readonly isPresale = input(false);
  readonly appendTo = 'body';
  readonly inputToFocusEvent = output<InputToFocus>();
  readonly saveResponse = output<SaveResponse>();
  readonly responseEvent = output<FinalyseSale>();
  readonly CASH = 'CASH';
  canFocusLastModeInput = input(false);
  modeReglementComponent = viewChild<ModeReglementComponent>('modeReglement');
  amountComputingComponent = viewChild<AmountComputingComponent>('amountComputing');
  protected remise?: IRemise | null;
  protected event: any;
  protected readonly currentSaleService = inject(CurrentSaleService);
  protected readonly hasAuthorityService = inject(HasAuthorityService);
  readonly canRemoveItem = signal(this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE));
  readonly canApplyDiscount = signal(this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE));
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly modalService = inject(NgbModal);
  private readonly facade = inject(ComptantFacadeService);
  private readonly baseSaleService = inject(BaseSaleService);
  private readonly selectedCustomerService = inject(SelectedCustomerService);
  private readonly selectModeReglementService = inject(SelectModeReglementService);

  constructor() {
    this.facade.saveResponse$.pipe(takeUntilDestroyed()).subscribe(res => {
      this.saveResponse.emit(res);
      if (res.success) {
        this.amountComputingComponent().computeMonnaie(null);
      }
    });
    this.facade.finalyseSale$.pipe(takeUntilDestroyed()).subscribe(res => {
      this.responseEvent.emit(res);
    });
    this.facade.openUninsuredCustomer$.pipe(takeUntilDestroyed()).subscribe(({ isVenteDefferee, putsOnStandby }) => {
      const modalRef = this.modalService.open(UninsuredCustomerListComponent, {
        size: '60%',
        backdrop: 'static',
        centered: true,
      });
      modalRef.componentInstance.header = 'CLIENTS NON ASSURES';
      modalRef.result.then(() => {
        if (isVenteDefferee && this.selectedCustomerService.selectedCustomerSignal()) {
          this.currentSaleService.currentSale().differe = isVenteDefferee;
          this.modeReglementComponent().commentaireInputGetFocus();
        } else {
          if (!isVenteDefferee) {
            this.finalyseSale(putsOnStandby);
          }
        }
      });
    });
  }

  protected get entryAmount(): number {
    return this.modeReglementComponent()?.getInputSum() || 0;
  }

  protected get isValidDiffere(): boolean {
    return this.currentSaleService.currentSale()?.differe;
  }

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
  }

  finalyseSale(putsOnStandby = false): void {
    this.facade.finalizeSale(
      putsOnStandby,
      this.entryAmount,
      this.modeReglementComponent().commentaire,
      this.baseSaleService.isAvoir(),
      this.modeReglementComponent().buildPayment(this.entryAmount),
    );
  }

  putCurrentSaleOnStandBy(): void {
    this.finalyseSale(true);
  }

  onKeyDown(event: any): void {
    this.save();
  }

  save(): void {
    const restToPay = this.currentSaleService.currentSale().amountToBePaid - this.entryAmount;
    this.currentSaleService.currentSale().montantVerse = this.facade.getCashAmount(
      this.selectModeReglementService.modeReglements(),
      this.CASH,
    );
    if (restToPay > 0 && !this.isValidDiffere) {
      this.confimDialog().onConfirm(() => this.facade.confirmDiffereSale(), 'Vente différé', 'Voullez-vous regler le reste en différé ?');
    } else {
      this.finalyseSale();
    }
  }

  createComptant(salesLine: ISalesLine): void {
    this.facade.createComptant(salesLine);
  }

  onAddProduit(salesLine: ISalesLine): void {
    this.facade.addItemToSale(salesLine);
  }

  removeLine(salesLine: ISalesLine): void {
    this.facade.removeItemFromSale(salesLine.id);
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
    if (salesLine) {
      this.facade.updateItemQtyRequested(salesLine);
    }
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    this.facade.updateItemQtySold(salesLine);
  }

  updateItemPrice(salesLine: ISalesLine): void {
    this.facade.updateItemPrice(salesLine);
  }

  printInvoice(): void {
    if (this.selectedCustomerService.selectedCustomerSignal()) {
      this.facade.printInvoice(this.currentSaleService.currentSale().id);
    }
  }

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    const modes = this.selectModeReglementService.modeReglements();
    if (modes.length >= this.baseSaleService.maxModePayementNumber()) {
      const amount = this.entryAmount;
      modes.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        this.currentSaleService.currentSale().amountToBePaid - paymentModeControl.paymentMode.amount;

      this.amountComputingComponent().computeMonnaie(amount);
    } else {
      const inputAmount = Number(paymentModeControl.control.target.value?.replace(/\D/g, ''));
      this.amountComputingComponent().computeMonnaie(inputAmount);
      this.modeReglementComponent().manageShowAddButton(inputAmount);
    }
  }

  onLoadPrevente(): void {
    this.modeReglementComponent().buildPreventeReglementInput();
  }

  print(sale: ISales | null): void {
    this.facade.printInvoice(sale.id);
  }

  printSale(saleId: number): void {
    this.facade.printReceipt(saleId);
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

  onAddRemise(remise: IRemise): void {
    if (this.canApplyDiscount()) {
      this.addRemise(remise);
    } else {
      if (remise) {
        this.onAddRmiseOpenActionAutorisationDialog(remise);
      } else {
        if (this.currentSaleService.currentSale().remise) {
          this.onAddRmiseOpenActionAutorisationDialog(remise);
        }
      }
    }
  }

  addRemise(remise: IRemise): void {
    if (remise) {
      this.facade.addRemise(remise);
    } else {
      if (this.currentSaleService.currentSale().remise) {
        this.facade.removeRemise();
      }
    }
  }
}
