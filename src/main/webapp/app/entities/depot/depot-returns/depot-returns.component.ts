import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { Select, SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { AutoComplete } from 'primeng/autocomplete';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { QuantiteProdutSaisieComponent } from 'app/shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { RetourDepot } from 'app/shared/model/retour-depot.model';
import { IRetourDepotItem, RetourDepotItem } from 'app/shared/model/retour-depot-item.model';
import { RetourDepotService } from '../retour-depot.service';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { debounceTime, Subject, Subscription } from 'rxjs';
import { Tooltip } from 'primeng/tooltip';
import { finalize } from 'rxjs/operators';
import { IMagasin } from '../../../shared/model/magasin.model';
import { MagasinService } from '../../magasin/magasin.service';
import { IProduit } from '../../../shared/model/produit.model';
import { StockDepotService } from '../stock-depot/stock-depot.service';

@Component({
  selector: 'jhi-depot-returns',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    ToolbarModule,
    SelectModule,
    InputTextModule,
    InputNumberModule,
    AutoComplete,
    ToastModule,
    WarehouseCommonModule,
    QuantiteProdutSaisieComponent,
    Tooltip
  ],
  providers: [MessageService],
  templateUrl: './depot-returns.component.html',
  styleUrl: './depot-returns.component.scss'
})
export class DepotReturnsComponent implements OnInit, OnDestroy {
  private readonly retourDepotService = inject(RetourDepotService);
  private readonly stockDepotService = inject(StockDepotService);
  private readonly magasinService = inject(MagasinService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  @ViewChild('productAutoComplete') productAutoComplete: AutoComplete | undefined;
  @ViewChild('depotSelect') depotSelect: Select | undefined;
  @ViewChild('quantiteBox') quantiteBox: QuantiteProdutSaisieComponent | undefined;

  protected depots = signal<IMagasin[]>([]);
  protected products = signal<IProduit[]>([]);
  protected filteredProducts = signal<IProduit[]>([]);
  protected selectedDepot = signal<IMagasin | null>(null);
  protected selectedProduct = signal<IProduit | null>(null);
  protected returnQuantity = signal<number>(1);
  protected retourDepotItems = signal<IRetourDepotItem[]>([]);
  protected isSaving = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;

  private readonly searchTrigger$ = new Subject<string>();
  private searchSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.loadDepots();

    // Setup debounced search for products
    this.searchSubscription = this.searchTrigger$
      .pipe(debounceTime(300))
      .subscribe(search => this.filterProducts(search));
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  protected loadDepots(): void {
    this.magasinService
      .fetchAllDepots()
      .subscribe({
        next: (res: HttpResponse<IMagasin[]>) => {
          this.depots.set(res.body || []);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors du chargement des dépôts'
          });
        }
      });
  }

  protected onDepotChange(): void {
    const depot = this.selectedDepot();
    this.products.set([]);
    this.filteredProducts.set([]);
    this.retourDepotItems.set([]);

    if (depot && depot.id) {
      this.loadProducts(depot.id);
    }
  }

  protected loadProducts(depotId: number): void {
    this.stockDepotService
      .query({ magasinId: depotId })
      .subscribe({
        next: (res: HttpResponse<IProduit[]>) => {
          this.products.set(res.body || []);
          this.filteredProducts.set(res.body || []);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors du chargement des produits'
          });
        }
      });
  }

  protected searchProducts(event: any): void {
    this.searchTrigger$.next(event.query);
  }

  protected filterProducts(search: string): void {
    const searchLower = search.toLowerCase();
    const filtered = this.products().filter(product => {
      return (
        product.codeCip?.toLowerCase().includes(searchLower) ||
        product.libelle?.toLowerCase().includes(searchLower)
      );
    });
    this.filteredProducts.set(filtered);
  }

  protected onProductSelect(): void {
    const product = this.selectedProduct();
    if (product) {
      const maxQuantity = product.totalQuantity || 1;
      this.returnQuantity.set(Math.min(1, maxQuantity));

      setTimeout(() => {
        this.quantiteBox?.focusProduitControl();
      }, 100);
    }
  }

  protected onAddReturnLine(quantity: number): void {
    this.returnQuantity.set(quantity);
    this.addReturnLine();
  }

  protected addReturnLine(): void {
    const product = this.selectedProduct();
    const quantity = this.returnQuantity();

    if (!product) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'Veuillez sélectionner un produit'
      });
      return;
    }

    if (quantity < 1) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: 'La quantité doit être supérieure à 0'
      });
      return;
    }

    const maxQuantity = product.totalQuantity || 0;
    if (quantity > maxQuantity) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: `La quantité ne peut pas dépasser ${maxQuantity}`
      });
      return;
    }

    this.createReturnItem(product, quantity);
    this.resetSelection();
    this.messageService.add({
      severity: 'success',
      summary: 'Succès',
      detail: 'Ligne ajoutée aux retours'
    });

    this.focusProductInput();
  }

  private createReturnItem(product: IProduit, quantity: number): void {
    const existingItemIndex = this.retourDepotItems().findIndex(item =>
      item.produitId === product.id
    );

    if (existingItemIndex !== -1) {
      this.retourDepotItems.update(items => {
        const updatedItems = [...items];
        const existingItem = updatedItems[existingItemIndex];
        const maxQuantity = product.totalQuantity || 0;
        const newQuantity = (existingItem.qtyMvt || 0) + quantity;

        if (newQuantity > maxQuantity) {
          this.messageService.add({
            severity: 'warn',
            summary: 'Attention',
            detail: `La quantité totale ne peut pas dépasser ${maxQuantity}. Quantité ajustée.`
          });
          existingItem.qtyMvt = maxQuantity;
        } else {
          existingItem.qtyMvt = newQuantity;
        }

        return updatedItems;
      });
    } else {
      const newItem = new RetourDepotItem();
      newItem.produitLibelle = product.libelle;
      newItem.produitCip = product.codeCip;
      newItem.produitId = product.id;
      newItem.qtyMvt = quantity;
      newItem.regularUnitPrice = product.regularUnitPrice;
      newItem.totalQuantity = product.totalQuantity;

      this.retourDepotItems.update(items => [...items, newItem]);
    }
  }

  private resetSelection(): void {
    this.selectedProduct.set(null);
    this.returnQuantity.set(1);

    if (this.quantiteBox) {
      this.quantiteBox.reset();
    }
  }

  protected removeReturnLine(index: number): void {
    this.retourDepotItems.update(items => items.filter((_, i) => i !== index));
  }

  protected onQuantityChange(item: IRetourDepotItem): void {
    const maxQuantity = item.totalQuantity || 0;
    if (item.qtyMvt && item.qtyMvt > maxQuantity) {
      item.qtyMvt = maxQuantity;
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: `La quantité de retour ne peut pas dépasser ${maxQuantity}`
      });
    }
    if (item.qtyMvt && item.qtyMvt < 1) {
      item.qtyMvt = 1;
    }
  }

  protected canSave(): boolean {
    const depot = this.selectedDepot();
    const items = this.retourDepotItems();

    if (!depot || items.length === 0) {
      return false;
    }

    return items.every(item => item.qtyMvt && item.qtyMvt >= 1);
  }

  protected save(): void {
    if (!this.canSave()) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs requis'
      });
      return;
    }

    const depot = this.selectedDepot();
    const retourDepot = new RetourDepot();
    retourDepot.depotId = depot!.id;
    retourDepot.retourDepotItems = this.retourDepotItems();

    this.isSaving.set(true);

    this.retourDepotService.create(retourDepot).pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Retour dépôt créé avec succès'
        });
        this.reset();
        setTimeout(() => {
          this.navigateToList();
        }, 1500);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors de la création du retour dépôt'
        });
      }
    });
  }

  protected navigateToList(): void {
    this.router.navigate(['/depot']);
  }

  protected reset(): void {
    this.selectedDepot.set(null);
    this.products.set([]);
    this.filteredProducts.set([]);
    this.selectedProduct.set(null);
    this.returnQuantity.set(1);
    this.retourDepotItems.set([]);

    if (this.quantiteBox) {
      this.quantiteBox.reset();
    }
  }

  protected getMaxReturnQuantity(item: IRetourDepotItem): number {
    return item.totalQuantity || 0;
  }

  private focusProductInput(): void {
    setTimeout(() => {
      this.productAutoComplete?.inputEL?.nativeElement?.focus();
    }, 100);
  }
}
