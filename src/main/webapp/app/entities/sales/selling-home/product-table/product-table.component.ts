import { Component, computed, inject, output, Signal, viewChild } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { NgbAlertModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CurrentSaleService } from '../../service/current-sale.service';
import { ISales } from '../../../../shared/model/sales.model';
import { HasAuthorityService } from '../../service/has-authority.service';
import { BaseSaleService } from '../../service/base-sale.service';
import { Authority } from '../../../../shared/constants/authority.constants';
import { SplitButtonModule } from 'primeng/splitbutton';
import { RemiseCacheService } from '../../service/remise-cache.service';
import { IRemise, Remise } from '../../../../shared/model/remise.model';
import { FormsModule } from '@angular/forms';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputGroupModule } from 'primeng/inputgroup';
import { Select } from 'primeng/select';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { showCommonError } from '../sale-helper';
import { InputIcon } from 'primeng/inputicon';
import { IconField } from 'primeng/iconfield';

@Component({
  selector: 'jhi-product-table',
  imports: [
    WarehouseCommonModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    SplitButtonModule,
    NgbAlertModule,
    FormsModule,
    InputGroupAddonModule,
    InputGroupModule,
    Select,
    ConfirmDialogComponent,
    InputIcon,
    IconField
  ],
  templateUrl: './product-table.component.html',
  styleUrls: ['./styles-table-produits.scss'],
})
export class ProductTableComponent {
  readonly itemQtySoldEvent = output<ISalesLine>();
  readonly itemPriceEvent = output<ISalesLine>();
  readonly deleteItemEvent = output<ISalesLine>();
  readonly itemQtyRequestedEvent = output<ISalesLine>();
  readonly addRemiseEvent = output<Remise>();
  canModifiePrice: boolean;
  baseSaleService = inject(BaseSaleService);
  currentSaleService = inject(CurrentSaleService);
  remiseCacheService = inject(RemiseCacheService);

  protected typeAlert = 'success';
  protected canApplyDiscount = true;
  protected selectedRemise: any;
  private readonly canRemoveItem: boolean;
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly hasAuthorityService = inject(HasAuthorityService);
  private readonly modalService = inject(NgbModal);

  sale: Signal<ISales> = this.currentSaleService.currentSale;
  totalQtyProduit: Signal<number> = computed(() => this.sale().salesLines.reduce((sum, current) => sum + current.quantityRequested, 0));
  totalQtyServi: Signal<number> = computed(() => this.sale().salesLines.reduce((sum, current) => sum + current.quantitySold, 0));
  totalTtc: Signal<number> = computed(() => this.sale().salesLines.reduce((sum, current) => sum + current.salesAmount, 0));
  remiseTaux: Signal<string> = computed(() => {
    const currentSale = this.sale();
    if (currentSale.remise) {
      const remise = currentSale.remise;
      if (remise.type === 'remiseProduit') {
        return this.getTaux(remise, currentSale.type);
      }
      return remise.remiseValue + ' %';
    }
    return '';
  });

  constructor() {
    this.canApplyDiscount = this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE);
    this.canModifiePrice = this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFIER_PRIX);
    this.canRemoveItem = this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE);
  }

  onSelectRemise(): void {
    this.addRemiseEvent.emit(this.selectedRemise);
  }

  updateItemQtySold(salesLine: ISalesLine, event: any): void {
    const newQty = Number(event.target.value);
    if (newQty >= 0) {
      if (newQty > salesLine.quantityRequested) {
        this.openInfoDialog(
          `La quantité saisie  ${newQty}  ne doit pas être supérieure à la quantité demandée ${salesLine.quantityRequested}`,
          'alert alert-danger'
        );
      } else {
        salesLine.quantitySold = newQty;
        this.itemQtySoldEvent.emit(salesLine);
      }
    }
  }

  openInfoDialog(message: string, infoClass: string): void {
    showCommonError(this.modalService, message, infoClass);
  }

  updateItemQtyRequested(salesLine: ISalesLine, event: any): void {
    const newQty = Number(event.target.value);

    if (newQty > 0) {
      if (newQty > this.baseSaleService.quantityMax()) {
        this.onUpdateConfirmForceStock(salesLine, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
      } else {
        salesLine.quantityRequested = newQty;
        this.itemQtyRequestedEvent.emit(salesLine);
      }
    }
  }

  updateItemPrice(salesLine: ISalesLine, event: any): void {
    const newPrice = Number(event.target.value);
    if (newPrice > 0) {
      salesLine.regularUnitPrice = newPrice;
      this.itemPriceEvent.emit(salesLine);
    }
  }

  onDeleteItem(item: ISalesLine): void {
    if (this.canRemoveItem) {
      this.confimDialog().onConfirm(
        () => this.deleteItemEvent.emit(item),
        'Supprimer Produit',
        ' Voullez-vous supprimer  ce produit ?',
        null,
        () => this.deleteItemEvent.emit(null)
      );
    } else {
      this.deleteItemEvent.emit(item);
    }
  }

  protected getTaux(entity: IRemise, type: string): string {
    const taut = entity.grilles.filter(grille => grille.grilleType === type)[0]?.remiseValue;
    if (taut) {
      return taut + ' %';
    }
    return '';
  }

  private onUpdateConfirmForceStock(salesLine: ISalesLine, message: string): void {
    this.confimDialog().onConfirm(
      () => this.itemQtyRequestedEvent.emit(salesLine),
      'Forcer le stock',
      message,
      null,
      () => this.itemQtyRequestedEvent.emit(null)
    );
  }
}
