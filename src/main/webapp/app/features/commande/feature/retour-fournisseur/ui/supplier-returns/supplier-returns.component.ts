import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { Select, SelectModule } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { ToastModule } from 'primeng/toast';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { QuantiteProdutSaisieComponent } from 'app/shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { NotificationService } from 'app/shared/services/notification.service';
import { IFournisseur } from 'app/shared/model/fournisseur.model';
import { RetourBon } from 'app/shared/model/retour-bon.model';
import { IRetourBonItem, RetourBonItem } from 'app/shared/model/retour-bon-item.model';
import { IMotifRetourProduit } from 'app/shared/model/motif-retour-produit.model';
import { AbstractOrderItem } from 'app/shared/model/abstract-order-item.model';
import { ModifRetourProduitService } from 'app/entities/motif-retour-produit/motif-retour-produit.service';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { debounceTime, Subject, Subscription } from 'rxjs';
import { Textarea } from 'primeng/textarea';
import { Tooltip } from 'primeng/tooltip';
import { IDeliveryItem } from 'app/shared/model/delivery-item';
import { finalize } from 'rxjs/operators';
import { LotSelection, LotSelectionDialogComponent } from '../lot-selection-dialog.component';
import { InlineLotSelection, InlineLotSelectionComponent } from '../inline-lot-selection.component';
import { RetourBonService } from "../../../../../../entities/commande/retour_fournisseur/retour-bon.service";
import { DeliveryService } from "../../../../../../entities/commande/delevery/delivery.service";
import { ICommande } from "../../../../../../shared/model/commande.model";
import { ConfigurationService } from "../../../../../../shared/configuration.service";

@Component({
  selector: 'app-supplier-returns',
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
    QuantiteProdutSaisieComponent,
    Textarea,
    Tooltip,
    InlineLotSelectionComponent,
  ],
  templateUrl: './supplier-returns.component.html',
  styleUrl: './supplier-returns.component.scss',
})
export class SupplierReturnsComponent implements OnInit, OnDestroy {
  private readonly retourBonService = inject(RetourBonService);
  private readonly commandeService = inject(DeliveryService);
  private readonly configurationService = inject(ConfigurationService);

  protected delaiRetourSeuil = 365;
  private readonly motifRetourProduitService = inject(ModifRetourProduitService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
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
  protected delayWarning = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;

  protected isEditMode = signal<boolean>(false);
  protected editRetourBonId = signal<number | null>(null);
  protected lotSelectionMode = signal<'dialog' | 'inline'>('dialog');

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
    this.loadDelaiSeuil();
    this.searchSubscription = this.searchTrigger$.pipe(debounceTime(300)).subscribe(search => this.filterOrderLines(search));
    this.commandeSearchSubscription = this.commandeSearchTrigger$.pipe(debounceTime(300)).subscribe(search => this.filterCommandes(search));

    const id = this.route.snapshot.params['id'];
    if (id) {
      this.isEditMode.set(true);
      this.editRetourBonId.set(Number(id));
      this.loadRetourForEdit(Number(id));
    }
  }

  private loadDelaiSeuil(): void {
    this.configurationService.find('APP_DELAI_RETOUR_FOURNISSEUR').subscribe({
      next: res => {
        const val = parseInt(res.body?.value ?? '', 10);
        if (!isNaN(val)) this.delaiRetourSeuil = val;
      },
    });
  }

  private computeDelayWarning(orderDate: string | null | undefined): void {
    if (!orderDate) { this.delayWarning.set(false); return; }
    const order = new Date(orderDate);
    const today = new Date();
    const diffDays = Math.floor((today.getTime() - order.getTime()) / 86_400_000);
    this.delayWarning.set(diffDays > this.delaiRetourSeuil);
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    this.commandeSearchSubscription?.unsubscribe();
  }

  protected loadRetourForEdit(id: number): void {
    this.retourBonService.find(id).subscribe({
      next: (res: HttpResponse<any>) => {
        const retour = res.body;
        if (!retour) return;

        // Reconstitute a minimal ICommande to satisfy selectedCommande
        const commande: ICommande = {
          id: retour.commandeId,
          orderDate: retour.commandeOrderDate,
          receiptReference: retour.receiptReference,
          fournisseurLibelle: retour.fournisseurLibelle,
        };
        this.selectedCommande.set(commande);
        this.computeDelayWarning(commande.orderDate);

        this.commentaire.set(retour.commentaire ?? '');

        // Pre-fill items directly from the DTO
        const items = (retour.retourBonItems ?? []).map((itemDto: any) => {
          const item = new RetourBonItem();
          item.id = itemDto.id;
          item.orderLineId = itemDto.orderLineId;
          item.orderLineOrderDate = itemDto.orderLineOrderDate;
          item.produitLibelle = itemDto.produitLibelle;
          item.produitCip = itemDto.produitCip;
          item.produitId = itemDto.produitId;
          item.orderLineQuantityRequested = itemDto.orderLineQuantityRequested;
          item.orderLineQuantityReceived = itemDto.orderLineQuantityReceived;
          item.qtyMvt = itemDto.qtyMvt;
          item.motifRetourId = itemDto.motifRetourId;
          item.motifRetourLibelle = itemDto.motifRetourLibelle;
          item.lotId = itemDto.lotId;
          item.lotNumero = itemDto.lotNumero;
          return item;
        });
        this.retourBonItems.set(items);

        // Load order lines for the commande so the user can add more
        if (commande.id && commande.orderDate) {
          this.loadOrderLines(commande.id, commande.orderDate);
        }
      },
      error: () => {
        this.notificationService.error('Impossible de charger le retour');
      },
    });
  }

  protected loadMotifRetours(): void {
    this.motifRetourProduitService.query().subscribe({
      next: (res: HttpResponse<IMotifRetourProduit[]>) => {
        this.motifRetours.set(res.body || []);
      },
      error: () => {
        this.notificationService.error('Erreur lors du chargement des motifs de retour');
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

    this.commandeService.queryWithoutDetail({ page: 0, size: 10, search: search.trim() }).subscribe({
      next: (res: HttpResponse<ICommande[]>) => {
        this.filteredCommandes.set(res.body || []);
      },
      error: () => {
        this.notificationService.error('Erreur lors de la recherche des commandes');
      },
    });
  }

  protected onCommandeChange(): void {
    const commande = this.selectedCommande();
    if (commande && commande.id && commande.orderDate) {
      this.loadOrderLines(commande.id, commande.orderDate);
      this.computeDelayWarning(commande.orderDate);
      this.retourBonItems.set([]);
      this.selectedOrderLine.set(null);
      this.selectedMotifRetourId.set(null);
      this.returnQuantity.set(1);

      this.focusOrderLineInput();
    }
  }

  protected loadOrderLines(commandeId: number, orderDate: string): void {
    this.commandeService.filterItems({ id: commandeId, orderDate }).subscribe({
      next: (res: HttpResponse<IDeliveryItem[]>) => {
        this.orderLines.set(res.body);
      },
      error: () => {
        this.notificationService.error('Erreur lors du chargement des lignes de commande');
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
      const maxQuantity = orderLine.quantityReceived || orderLine.quantityRequested || 1;
      this.returnQuantity.set(Math.min(1, maxQuantity));
      this.selectedMotifRetourId.set(null);

      setTimeout(() => {
        this.motifSelect?.focus();
      }, 100);
    }
  }

  protected onMotifSelect(): void {
    setTimeout(() => {
      this.quantiteBox?.focusProduitControl();
    }, 100);
  }

  protected onAddReturnLine(quantity: number): void {
    this.returnQuantity.set(quantity);
    this.addReturnLine();
  }

  protected addReturnLine(): void {
    const orderLine = this.selectedOrderLine();
    const quantity = this.returnQuantity();
    const motifRetourId = this.selectedMotifRetourId();

    if (!orderLine) {
      this.notificationService.warning('Veuillez sélectionner une ligne de commande', 'Attention');
      return;
    }

    if (!motifRetourId) {
      this.notificationService.warning('Veuillez sélectionner un motif de retour', 'Attention');
      return;
    }

    if (quantity < 1) {
      this.notificationService.warning('La quantité doit être supérieure à 0', 'Attention');
      return;
    }

    const maxQuantity = orderLine.quantityReceived || orderLine.quantityRequested || 0;
    if (quantity > maxQuantity) {
      this.notificationService.warning(`La quantité ne peut pas dépasser ${maxQuantity}`, 'Attention');
      return;
    }

    if (orderLine.lots && orderLine.lots.length > 0) {
      this.tempReturnQuantity.set(quantity);
      this.tempSelectedOrderLine.set(orderLine);
      this.tempMotifRetourId.set(motifRetourId);

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
        this.onLotSelectionCancelled();
      },
    );
  }

  protected onLotsConfirmed(lotSelections: LotSelection[] | InlineLotSelection[]): void {
    const orderLine = this.tempSelectedOrderLine();
    const motifRetourId = this.tempMotifRetourId();

    if (!orderLine || !motifRetourId) return;

    lotSelections.forEach(selection => {
      if (selection.selectedQuantity > 0) {
        this.createReturnItem(orderLine, selection.selectedQuantity, motifRetourId, selection.lot.id, selection.lot.numLot);
      }
    });

    this.resetSelection();
    this.showInlineLotSelection.set(false);
    this.notificationService.success(`${lotSelections.length} ligne(s) ajoutée(s) aux retours`);
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
    const existingItemIndex = this.retourBonItems().findIndex(
      item =>
        item.orderLineId === orderLine.id &&
        item.orderLineOrderDate === orderLine.orderDate &&
        item.motifRetourId === motifRetourId &&
        (item.lotId === lotId || (item.lotId == null && lotId == null)),
    );

    if (existingItemIndex !== -1) {
      this.retourBonItems.update(items => {
        const updatedItems = [...items];
        const existingItem = updatedItems[existingItemIndex];
        const maxQuantity = existingItem.orderLineQuantityReceived || existingItem.orderLineQuantityRequested || 0;
        const newQuantity = (existingItem.qtyMvt || 0) + quantity;

        if (newQuantity > maxQuantity) {
          this.notificationService.warning(`La quantité totale ne peut pas dépasser ${maxQuantity}. Quantité ajustée.`, 'Attention');
          existingItem.qtyMvt = maxQuantity;
        } else {
          existingItem.qtyMvt = newQuantity;
        }

        return updatedItems;
      });
    } else {
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
      this.notificationService.warning(`La quantité de retour ne peut pas dépasser ${maxQuantity}`, 'Attention');
    }
    if (item.qtyMvt && item.qtyMvt < 1) {
      item.qtyMvt = 1;
    }
  }

  protected canSave(): boolean {
    const items = this.retourBonItems();
    if (items.length === 0) return false;

    // In edit mode, commande may be absent (horsCommande retour) — skip that check
    if (!this.isEditMode() && !this.selectedCommande()) return false;

    return items.every(item => item.qtyMvt && item.qtyMvt >= 1 && item.motifRetourId);
  }

  protected save(): void {
    if (!this.canSave()) {
      this.notificationService.error('Veuillez remplir tous les champs requis (quantité et motif de retour)');
      return;
    }

    const commande = this.selectedCommande();
    const retourBon = new RetourBon();
    if (commande) {
      retourBon.commandeId = commande.id;
      retourBon.commandeOrderDate = commande.orderDate;
    }
    retourBon.commentaire = this.commentaire();
    retourBon.retourBonItems = this.retourBonItems();

    this.isSaving.set(true);

    const isEdit = this.isEditMode();
    const editId = this.editRetourBonId();
    const request$ = isEdit && editId
      ? this.retourBonService.update(editId, retourBon)
      : this.retourBonService.create(retourBon);

    request$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.notificationService.success(
          isEdit ? 'Retour fournisseur modifié avec succès' : 'Retour fournisseur créé avec succès'
        );
        this.reset();
        setTimeout(() => this.navigateToList(), 1500);
      },
      error: () => {
        this.notificationService.error(
          isEdit ? 'Erreur lors de la modification du retour fournisseur' : 'Erreur lors de la création du retour fournisseur'
        );
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
