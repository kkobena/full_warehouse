import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { Select, SelectModule } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { QuantiteProdutSaisieComponent } from 'app/shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { IFournisseur } from 'app/shared/model/fournisseur.model';
import { ICommande } from 'app/shared/model/commande.model';
import { RetourBon } from 'app/shared/model/retour-bon.model';
import { IRetourBonItem, RetourBonItem } from 'app/shared/model/retour-bon-item.model';
import { IMotifRetourProduit } from 'app/shared/model/motif-retour-produit.model';
import { AbstractOrderItem } from 'app/shared/model/abstract-order-item.model';
import { RetourBonService } from './retour-bon.service';
import { ModifRetourProduitService } from 'app/entities/motif-retour-produit/motif-retour-produit.service';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { debounceTime, Subject, Subscription } from 'rxjs';
import { Textarea } from 'primeng/textarea';
import { Tooltip } from 'primeng/tooltip';
import { DeliveryService } from '../delevery/delivery.service';
import { IDeliveryItem } from '../../../shared/model/delivery-item';
import { LotSelection, LotSelectionDialogComponent } from './lot-selection-dialog.component';
import { InlineLotSelection, InlineLotSelectionComponent } from './inline-lot-selection.component';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'jhi-supplier-returns',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    ToolbarModule,
    SelectModule,
    FloatLabel,
    InputTextModule,
    InputNumberModule,
    AutoComplete,
    ToastModule,
    WarehouseCommonModule,
    QuantiteProdutSaisieComponent,
    Textarea,
    Tooltip,
    InlineLotSelectionComponent,
  ],
  providers: [MessageService],
  templateUrl: './supplier-returns.component.html',
  styleUrl: './supplier-returns.component.scss',
})
export class SupplierReturnsComponent implements OnInit, OnDestroy {
  private readonly retourBonService = inject(RetourBonService);
  private readonly commandeService = inject(DeliveryService);
  private readonly motifRetourProduitService = inject(ModifRetourProduitService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);

  @ViewChild('orderSelect') orderSelect: AutoComplete | undefined;
  @ViewChild('orderLineAutoComplete') orderLineAutoComplete: AutoComplete | undefined;
  @ViewChild('motifSelect') motifSelect: Select | undefined;
  @ViewChild('quantiteBox') quantiteBox: QuantiteProdutSaisieComponent | undefined;

  protected fournisseurs = signal<IFournisseur[]>([]);
  protected commandes = signal<ICommande[]>([]);
  protected filteredCommandes = signal<ICommande[]>([]);
  protected motifRetours = signal<IMotifRetourProduit[]>([]);
  protected selectedFournisseur = signal<IFournisseur | null>(null);
  protected selectedCommande = signal<ICommande | null>(null);
  protected orderLines = signal<AbstractOrderItem[]>([]);
  protected filteredOrderLines = signal<AbstractOrderItem[]>([]);
  protected selectedOrderLine = signal<AbstractOrderItem | null>(null);
  protected selectedMotifRetourId = signal<number | null>(null);
  protected returnQuantity = signal<number>(1);
  protected retourBonItems = signal<IRetourBonItem[]>([]);
  protected commentaire = signal<string>('');
  protected isSaving = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;

  // Lot selection mode: 'dialog' or 'inline'
  protected lotSelectionMode = signal<'dialog' | 'inline'>('dialog');

  // Temporary values for lot selection
  protected tempReturnQuantity = signal<number>(0);
  protected tempSelectedOrderLine = signal<AbstractOrderItem | null>(null);
  protected tempMotifRetourId = signal<number | null>(null);
  protected showInlineLotSelection = signal<boolean>(false);

  private readonly searchTrigger$ = new Subject<string>();
  private readonly commandeSearchTrigger$ = new Subject<string>();
  private searchSubscription: Subscription | undefined;
  private commandeSearchSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.loadMotifRetours();

    // Setup debounced search for order lines
    this.searchSubscription = this.searchTrigger$.pipe(debounceTime(300)).subscribe(search => this.filterOrderLines(search));

    // Setup debounced search for commandes
    this.commandeSearchSubscription = this.commandeSearchTrigger$.pipe(debounceTime(300)).subscribe(search => this.filterCommandes(search));
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    this.commandeSearchSubscription?.unsubscribe();
  }

  protected loadMotifRetours(): void {
    this.motifRetourProduitService.query().subscribe({
      next: (res: HttpResponse<IMotifRetourProduit[]>) => {
        this.motifRetours.set(res.body || []);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du chargement des motifs de retour',
        });
      },
    });
  }

  protected searchCommandes(event: AutoCompleteCompleteEvent): void {
    this.commandeSearchTrigger$.next(event.query);
  }

  protected filterCommandes(search: string): void {
    if (!search || search.trim() === '') {
      this.filteredCommandes.set(this.commandes());
      return;
    }

    this.commandeService
      .queryWithoutDetail({
        page: 0,
        size: 10,
        search: search.trim(),
      })
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => {
          this.filteredCommandes.set(res.body || []);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors de la recherche des commandes',
          });
        },
      });
  }

  protected onCommandeChange(): void {
    const commande = this.selectedCommande();
    if (commande && commande.id && commande.orderDate) {
      this.loadOrderLines(commande.id, commande.orderDate);
      this.retourBonItems.set([]);
      this.selectedOrderLine.set(null);
      this.selectedMotifRetourId.set(null);
      this.returnQuantity.set(1);

      this.focusOrderLineInput();
    }
  }

  protected loadOrderLines(commandeId: number, orderDate: string): void {
    this.commandeService.filterItems({ id: commandeId, orderDate: orderDate }).subscribe({
      next: (res: HttpResponse<IDeliveryItem[]>) => {
        this.orderLines.set(res.body);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du chargement des lignes de commande',
        });
      },
    });
  }

  protected searchOrderLines(event: any): void {
    this.searchTrigger$.next(event.query);
  }

  protected filterOrderLines(search: string): void {
    const searchLower = search.toLowerCase();
    const filtered = this.orderLines().filter(line => {
      return line.produitCip?.toLowerCase().includes(searchLower) || line.produitLibelle?.toLowerCase().includes(searchLower);
    });
    this.filteredOrderLines.set(filtered);
  }

  protected onOrderLineSelect(): void {
    const orderLine = this.selectedOrderLine();
    if (orderLine) {
      // Reset quantity to default when selecting a new line
      const maxQuantity = orderLine.quantityReceived || orderLine.quantityRequested || 1;
      this.returnQuantity.set(Math.min(1, maxQuantity));
      // Reset motif selection
      this.selectedMotifRetourId.set(null);

      // Set focus to motif select
      setTimeout(() => {
        this.motifSelect?.focus();
      }, 100);
    }
  }

  protected onMotifSelect(): void {
    // Set focus to quantity box when motif is selected
    setTimeout(() => {
      this.quantiteBox?.focusProduitControl();
    }, 100);
  }

  protected onAddReturnLine(quantity: number): void {
    this.returnQuantity.set(quantity);
    this.addReturnLine();
    // Note: Focus is handled in addReturnLine() for both lot and non-lot cases
  }

  protected addReturnLine(): void {
    const orderLine = this.selectedOrderLine();
    const quantity = this.returnQuantity();
    const motifRetourId = this.selectedMotifRetourId();

    if (!orderLine) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez sélectionner une ligne de commande',
      });
      return;
    }

    if (!motifRetourId) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez sélectionner un motif de retour',
      });
      return;
    }

    if (quantity < 1) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'La quantité doit être supérieure à 0',
      });
      return;
    }

    const maxQuantity = orderLine.quantityReceived || orderLine.quantityRequested || 0;
    if (quantity > maxQuantity) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: `La quantité ne peut pas dépasser ${maxQuantity}`,
      });
      return;
    }

    // Check if lots exist
    if (orderLine.lots && orderLine.lots.length > 0) {
      // Store temporary values
      this.tempReturnQuantity.set(quantity);
      this.tempSelectedOrderLine.set(orderLine);
      this.tempMotifRetourId.set(motifRetourId);

      // Show lot selection based on mode
      if (this.lotSelectionMode() === 'dialog') {
        this.openLotSelectionDialog();
      } else {
        this.showInlineLotSelection.set(true);
      }
    } else {
      this.createReturnItem(orderLine, quantity, motifRetourId, null, null);
      this.resetSelection();
      this.focusOrderLineInput();
    }
  }

  protected openLotSelectionDialog(): void {
    const orderLine = this.tempSelectedOrderLine();
    if (!orderLine) return;

    const modalRef = this.modalService.open(LotSelectionDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });

    modalRef.componentInstance.lots = orderLine.lots || [];
    modalRef.componentInstance.requestedQuantity = this.tempReturnQuantity();
    modalRef.componentInstance.productLabel = orderLine.produitLibelle || '';

    modalRef.result.then(
      (selectedLots: LotSelection[]) => {
        this.onLotsConfirmed(selectedLots);
      },
      () => {
        // Dismissed
        this.onLotSelectionCancelled();
      },
    );
  }

  protected onLotsConfirmed(lotSelections: LotSelection[] | InlineLotSelection[]): void {
    const orderLine = this.tempSelectedOrderLine();
    const motifRetourId = this.tempMotifRetourId();

    if (!orderLine || !motifRetourId) return;

    // Create one return item per lot
    lotSelections.forEach(selection => {
      if (selection.selectedQuantity > 0) {
        this.createReturnItem(orderLine, selection.selectedQuantity, motifRetourId, selection.lot.id, selection.lot.numLot);
      }
    });

    // Reset selection
    this.resetSelection();
    this.showInlineLotSelection.set(false);

    this.messageService.add({
      severity: 'success',
      summary: 'Succès',
      detail: `${lotSelections.length} ligne(s) ajoutée(s) aux retours`,
    });

    this.focusOrderLineInput();
  }

  protected onLotSelectionCancelled(): void {
    this.tempReturnQuantity.set(0);
    this.tempSelectedOrderLine.set(null);
    this.tempMotifRetourId.set(null);
    this.showInlineLotSelection.set(false);
  }

  private createReturnItem(
    orderLine: AbstractOrderItem,
    quantity: number,
    motifRetourId: number,
    lotId: number | null | undefined,
    lotNumero: string | null | undefined,
  ): void {
    // Check if an item with the same orderLineId, lotId, and motifRetourId already exists
    const existingItemIndex = this.retourBonItems().findIndex(
      item =>
        item.orderLineId === orderLine.id &&
        item.orderLineOrderDate === orderLine.orderDate &&
        item.motifRetourId === motifRetourId &&
        (item.lotId === lotId || (item.lotId == null && lotId == null)),
    );

    if (existingItemIndex !== -1) {
      // Item exists - increment the quantity
      this.retourBonItems.update(items => {
        const updatedItems = [...items];
        const existingItem = updatedItems[existingItemIndex];
        const maxQuantity = existingItem.orderLineQuantityReceived || existingItem.orderLineQuantityRequested || 0;
        const newQuantity = (existingItem.qtyMvt || 0) + quantity;

        // Check if new quantity exceeds max
        if (newQuantity > maxQuantity) {
          this.messageService.add({
            severity: 'warn',
            summary: 'Attention',
            detail: `La quantité totale ne peut pas dépasser ${maxQuantity}. Quantité ajustée.`,
          });
          existingItem.qtyMvt = maxQuantity;
        } else {
          existingItem.qtyMvt = newQuantity;
        }

        return updatedItems;
      });
    } else {
      // Item doesn't exist - create new
      const selectedMotif = this.motifRetours().find(m => m.id === motifRetourId);

      const newItem = new RetourBonItem();
      newItem.orderLineId = orderLine.id;
      newItem.orderLineOrderDate = orderLine.orderDate;
      newItem.produitLibelle = orderLine.produitLibelle;
      newItem.produitCip = orderLine.produitCip;
      newItem.produitId = orderLine.produitId;
      newItem.orderLineQuantityRequested = orderLine.quantityRequested;
      newItem.orderLineQuantityReceived = orderLine.quantityReceived;
      newItem.qtyMvt = quantity;
      newItem.motifRetourId = motifRetourId;
      newItem.motifRetourLibelle = selectedMotif?.libelle;
      newItem.lotId = lotId || undefined;
      newItem.lotNumero = lotNumero || undefined;

      this.retourBonItems.update(items => [...items, newItem]);
    }
  }

  private resetSelection(): void {
    this.selectedOrderLine.set(null);
    this.selectedMotifRetourId.set(null);
    this.returnQuantity.set(1);

    if (this.quantiteBox) {
      this.quantiteBox.reset();
    }
  }

  protected toggleLotSelectionMode(): void {
    this.lotSelectionMode.update(mode => (mode === 'dialog' ? 'inline' : 'dialog'));
  }

  protected removeReturnLine(index: number): void {
    this.retourBonItems.update(items => items.filter((_, i) => i !== index));
  }

  protected onQuantityChange(item: IRetourBonItem): void {
    const maxQuantity = item.orderLineQuantityReceived || item.orderLineQuantityRequested || 0;
    if (item.qtyMvt && item.qtyMvt > maxQuantity) {
      item.qtyMvt = maxQuantity;
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: `La quantité de retour ne peut pas dépasser ${maxQuantity}`,
      });
    }
    if (item.qtyMvt && item.qtyMvt < 1) {
      item.qtyMvt = 1;
    }
  }

  protected canSave(): boolean {
    const commande = this.selectedCommande();
    const items = this.retourBonItems();

    if (!commande || items.length === 0) {
      return false;
    }

    // Check if all items have valid quantity and motif
    return items.every(item => {
      return item.qtyMvt && item.qtyMvt >= 1 && item.motifRetourId;
    });
  }

  protected save(): void {
    if (!this.canSave()) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs requis (quantité et motif de retour)',
      });
      return;
    }

    const commande = this.selectedCommande();
    const retourBon = new RetourBon();
    retourBon.commandeId = commande!.id;
    retourBon.commandeOrderDate = commande!.orderDate;
    retourBon.commentaire = this.commentaire();
    retourBon.retourBonItems = this.retourBonItems();

    this.isSaving.set(true);

    this.retourBonService
      .create(retourBon)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Retour fournisseur créé avec succès',
          });
          this.reset();
          // Navigate back to the list
          setTimeout(() => {
            this.navigateToList();
          }, 1500);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors de la création du retour fournisseur',
          });
        },
      });
  }

  protected navigateToList(): void {
    this.router.navigate(['/commande'], { queryParams: { tab: 'RETOUR_FOURNISSEUR' } });
  }

  protected reset(): void {
    this.selectedFournisseur.set(null);
    this.selectedCommande.set(null);
    this.orderLines.set([]);
    this.filteredOrderLines.set([]);
    this.filteredCommandes.set([]);
    this.selectedOrderLine.set(null);
    this.selectedMotifRetourId.set(null);
    this.returnQuantity.set(1);
    this.retourBonItems.set([]);
    this.commentaire.set('');

    // Reset the quantity component
    if (this.quantiteBox) {
      this.quantiteBox.reset();
    }
  }

  protected getMaxReturnQuantity(item: IRetourBonItem): number {
    return item.orderLineQuantityReceived || item.orderLineQuantityRequested || 0;
  }

  private focusOrderLineInput(): void {
    setTimeout(() => {
      this.orderLineAutoComplete?.inputEL?.nativeElement?.focus();
    }, 100);
  }
}
