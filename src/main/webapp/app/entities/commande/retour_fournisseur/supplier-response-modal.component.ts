import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { IRetourBon } from 'app/shared/model/retour-bon.model';
import { IReponseRetourBonItem, ReponseRetourBonItem } from 'app/shared/model/reponse-retour-bon-item.model';
import { ReponseRetourBon } from 'app/shared/model/reponse-retour-bon.model';
import { Tooltip } from 'primeng/tooltip';

@Component({
  selector: 'jhi-supplier-response-modal',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    InputNumberModule,
    ToastModule,
    WarehouseCommonModule,
    Tooltip
  ],
  providers: [MessageService],
  templateUrl: './supplier-response-modal.component.html',
  styleUrl: './supplier-response-modal.component.scss',
})
export class SupplierResponseModalComponent implements OnInit {
  protected readonly activeModal = inject(NgbActiveModal);
  protected readonly messageService = inject(MessageService);

  retourBon: IRetourBon | null = null;
  title = 'Saisir la réponse fournisseur';

  protected responseItems = signal<IReponseRetourBonItem[]>([]);
  protected isSaving = signal<boolean>(false);

  ngOnInit(): void {
    if (this.retourBon && this.retourBon.retourBonItems) {
      // Initialize response items from retour bon items
      const items: IReponseRetourBonItem[] = this.retourBon.retourBonItems.map(item => {
        const responseItem = new ReponseRetourBonItem();
        responseItem.retourBonItemId = item.id;
        responseItem.produitLibelle = item.produitLibelle;
        responseItem.produitCip = item.produitCip;
        responseItem.lotNumero = item.lotNumero;
        responseItem.requestedQty = item.qtyMvt;
        responseItem.qtyMvt = item.qtyMvt; // Default to requested quantity
        responseItem.acceptedQty= item.acceptedQty;
        return responseItem;
      });
      this.responseItems.set(items);
    }
  }

  protected isItemAlreadyProcessed(item: IReponseRetourBonItem): boolean {
    if (item.acceptedQty && item.acceptedQty > 0) {
      return item.acceptedQty === item.qtyMvt;
    }
    return false;
  }

  protected onQuantityChange(item: IReponseRetourBonItem): void {
    const maxQuantity = item.requestedQty || 0;
    if (item.qtyMvt && item.qtyMvt > maxQuantity) {
      item.qtyMvt = maxQuantity;
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: `La quantité acceptée ne peut pas dépasser la quantité demandée (${maxQuantity})`,
      });
    }
    if (item.qtyMvt && item.qtyMvt < 0) {
      item.qtyMvt = 0;
    }
  }

  protected canSave(): boolean {
    const items = this.responseItems();
    if (items.length === 0) {
      return false;
    }

    // Check if all items have valid quantity
    return items.every(item => item.qtyMvt !== undefined && item.qtyMvt !== null && item.qtyMvt >= 0);
  }

  protected getTotalRequested(): number {
    return this.responseItems().reduce((sum, item) => sum + (item.requestedQty || 0), 0);
  }

  protected getTotalAccepted(): number {
    return this.responseItems().reduce((sum, item) => sum + (item.qtyMvt || 0), 0);
  }

  protected save(): void {
    if (!this.canSave()) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir toutes les quantités acceptées',
      });
      return;
    }

    const reponseRetourBon = new ReponseRetourBon();
    reponseRetourBon.retourBonId = this.retourBon!.id;
    reponseRetourBon.reponseRetourBonItems = this.responseItems();

    this.activeModal.close(reponseRetourBon);
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }


  protected getRowClass(item: IReponseRetourBonItem): string {
    if (item.qtyMvt === 0) {
      return 'rejected-row';
    }
    if (item.qtyMvt && item.requestedQty && item.qtyMvt < item.requestedQty) {
      return 'partial-row';
    }
    return 'accepted-row';
  }
}
