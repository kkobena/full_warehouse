import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { SelectModule } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { IFournisseur } from 'app/shared/model/fournisseur.model';
import { ICommande } from 'app/shared/model/commande.model';
import { RetourBon } from 'app/shared/model/retour-bon.model';
import { IRetourBonItem, RetourBonItem } from 'app/shared/model/retour-bon-item.model';
import { IMotifRetourProduit } from 'app/shared/model/motif-retour-produit.model';
import { AbstractOrderItem } from 'app/shared/model/abstract-order-item.model';
import { RetourBonService } from './retour-bon.service';
import { FournisseurService } from 'app/entities/fournisseur/fournisseur.service';
import { CommandeService } from '../commande.service';
import { ModifRetourProduitService } from 'app/entities/motif-retour-produit/motif-retour-produit.service';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { debounceTime, Subject, Subscription } from 'rxjs';
import { Textarea } from 'primeng/textarea';
import { Tooltip } from 'primeng/tooltip';

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
    Textarea,
    Tooltip
  ],
  providers: [MessageService],
  templateUrl: './supplier-returns.component.html',
  styleUrl: './supplier-returns.component.scss',
})
export class SupplierReturnsComponent implements OnInit, OnDestroy {
  private readonly retourBonService = inject(RetourBonService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly commandeService = inject(CommandeService);
  private readonly motifRetourProduitService = inject(ModifRetourProduitService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  @ViewChild('orderSelect') orderSelect: AutoComplete | undefined;
  @ViewChild('orderLineAutoComplete') orderLineAutoComplete: AutoComplete | undefined;

  protected fournisseurs = signal<IFournisseur[]>([]);
  protected commandes = signal<ICommande[]>([]);
  protected filteredCommandes = signal<ICommande[]>([]);
  protected motifRetours = signal<IMotifRetourProduit[]>([]);
  protected selectedFournisseur = signal<IFournisseur | null>(null);
  protected selectedCommande = signal<ICommande | null>(null);
  protected orderLines = signal<AbstractOrderItem[]>([]);
  protected filteredOrderLines = signal<AbstractOrderItem[]>([]);
  protected selectedOrderLine = signal<AbstractOrderItem | null>(null);
  protected returnQuantity = signal<number>(1);
  protected retourBonItems = signal<IRetourBonItem[]>([]);
  protected commentaire = signal<string>('');
  protected isSaving = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;

  private readonly searchTrigger$ = new Subject<string>();
  private readonly commandeSearchTrigger$ = new Subject<string>();
  private searchSubscription: Subscription | undefined;
  private commandeSearchSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.loadFournisseurs();
    this.loadMotifRetours();
    this.loadAllCommandes();

    // Setup debounced search for order lines
    this.searchSubscription = this.searchTrigger$
      .pipe(debounceTime(300))
      .subscribe(search => this.filterOrderLines(search));

    // Setup debounced search for commandes
    this.commandeSearchSubscription = this.commandeSearchTrigger$
      .pipe(debounceTime(300))
      .subscribe(search => this.filterCommandes(search));
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    this.commandeSearchSubscription?.unsubscribe();
  }

  protected loadFournisseurs(search?: string): void {
    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
        search: search || '',
      })
      .subscribe({
        next: (res: HttpResponse<IFournisseur[]>) => {
          this.fournisseurs.set(res.body || []);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors du chargement des fournisseurs',
          });
        },
      });
  }

  protected loadMotifRetours(search?: string): void {
    this.motifRetourProduitService
      .query({
        page: 0,
        size: 9999,
        search: search || '',
      })
      .subscribe({
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

  protected onFournisseurChange(): void {
    const fournisseur = this.selectedFournisseur();
    if (fournisseur) {
      this.loadCommandesByFournisseur(fournisseur.id!);
      this.selectedCommande.set(null);
      this.orderLines.set([]);
      this.retourBonItems.set([]);
    }
  }

  protected loadCommandesByFournisseur(fournisseurId: number): void {
    this.commandeService
      .query({
        page: 0,
        size: 100,
        fournisseurId: fournisseurId,
      })
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => {
          this.commandes.set(res.body || []);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors du chargement des commandes',
          });
        },
      });
  }

  protected loadAllCommandes(): void {
    this.commandeService
      .query({
        page: 0,
        size: 1000,
      })
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => {
          this.commandes.set(res.body || []);
          this.filteredCommandes.set(res.body || []);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors du chargement des commandes',
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
      .query({
        page: 0,
        size: 100,
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
      this.returnQuantity.set(1);
    }
  }

  protected loadOrderLines(commandeId: number, orderDate: string): void {
    this.commandeService.find({ id: commandeId, orderDate: orderDate }).subscribe({
      next: (res: HttpResponse<ICommande>) => {
        const commande = res.body;
        if (commande && commande.orderLines) {
          this.orderLines.set(commande.orderLines);
        }
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
      return (
        line.produitCip?.toLowerCase().includes(searchLower) ||
        line.produitLibelle?.toLowerCase().includes(searchLower)
      );
    });
    this.filteredOrderLines.set(filtered);
  }

  protected onOrderLineSelect(): void {
    const orderLine = this.selectedOrderLine();
    if (orderLine) {
      // Reset quantity to default when selecting a new line
      const maxQuantity = orderLine.quantityReceived || orderLine.quantityRequested || 1;
      this.returnQuantity.set(Math.min(1, maxQuantity));
    }
  }

  protected addReturnLine(): void {
    const orderLine = this.selectedOrderLine();
    const quantity = this.returnQuantity();

    if (!orderLine) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez sélectionner une ligne de commande',
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

    // Check if this order line is already in the return items
    const existingIndex = this.retourBonItems().findIndex(
      item => item.orderLineId === orderLine.id && item.orderLineOrderDate === orderLine.orderDate,
    );

    if (existingIndex !== -1) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Cette ligne est déjà ajoutée aux retours',
      });
      return;
    }

    const newItem = new RetourBonItem();
    newItem.orderLineId = orderLine.id;
    newItem.orderLineOrderDate = orderLine.orderDate;
    newItem.produitLibelle = orderLine.produitLibelle;
    newItem.produitCip = orderLine.produitCip;
    newItem.produitId = orderLine.produitId;
    newItem.orderLineQuantityRequested = orderLine.quantityRequested;
    newItem.orderLineQuantityReceived = orderLine.quantityReceived;
    newItem.qtyMvt = quantity;
    newItem.initStock = 0;
    newItem.afterStock = 0;

    this.retourBonItems.update(items => [...items, newItem]);

    // Reset selection
    this.selectedOrderLine.set(null);
    this.returnQuantity.set(1);

    this.messageService.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Ligne ajoutée aux retours',
    });
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

    this.retourBonService.create(retourBon).subscribe({
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
      complete: () => {
        this.isSaving.set(false);
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
    this.returnQuantity.set(1);
    this.retourBonItems.set([]);
    this.commentaire.set('');
    this.loadAllCommandes();
  }

  protected getMaxReturnQuantity(item: IRetourBonItem): number {
    return item.orderLineQuantityReceived || item.orderLineQuantityRequested || 0;
  }

  protected getSelectedOrderLineMaxQuantity(): number {
    const orderLine = this.selectedOrderLine();
    if (!orderLine) {
      return 1;
    }
    return orderLine.quantityReceived || orderLine.quantityRequested || 1;
  }
}
