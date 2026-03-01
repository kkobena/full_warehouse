import {Component, computed, inject, OnInit, output, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {ToolbarModule} from 'primeng/toolbar';
import {DividerModule} from 'primeng/divider';
import {SelectModule} from 'primeng/select';
import {IconFieldModule} from 'primeng/iconfield';
import {InputIconModule} from 'primeng/inputicon';
import {InputGroupModule} from 'primeng/inputgroup';
import {InputGroupAddonModule} from 'primeng/inputgroupaddon';
import {TooltipModule} from 'primeng/tooltip';
import {TagModule} from 'primeng/tag';
import {ISales} from '../../../../shared/model/sales.model';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {UserVendeurService} from '../../../../entities/sales/service/user-vendeur.service';
import {SalesStatut} from '../../../../shared/model';

/**
 * PendingSalesListComponent
 *
 * Composant pour afficher et gérer les ventes en attente (préventes)
 *
 * Fonctionnalités :
 * - Liste des ventes en attente avec recherche
 * - Filtrage par vendeur et type de vente
 * - Reprise d'une vente (double-clic)
 * - Suppression de vente
 * - Expansion pour voir les détails produits
 *
 * @example
 * <app-pending-sales-list
 *   (saleResumed)="onSaleResumed($event)"
 *   (closed)="onClose()"
 * />
 */
@Component({
  selector: 'app-pending-sales-list',
  templateUrl: './pending-sales-list.component.html',
  styleUrls: ['./pending-sales-list.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    ToolbarModule,
    DividerModule,
    SelectModule,
    IconFieldModule,
    InputIconModule,
    InputGroupModule,
    InputGroupAddonModule,
    TooltipModule,
    TagModule,
  ],
})
export class PendingSalesListComponent implements OnInit {
  // ===== Services =====
  readonly facade = inject(SalesFacade);
  readonly userVendeurService = inject(UserVendeurService);

  // ===== Outputs =====
  readonly saleResumed = output<ISales>();
  readonly closed = output<void>();

  // ===== State from Facade =====
  readonly pendingSales = this.facade.pendingSales;
  readonly loading = this.facade.pendingSalesLoading;
  readonly seller = this.facade.seller;

  // ===== Local State Signals =====
  readonly selectedSale = signal<ISales | null>(null);
  readonly searchTerm = signal<string>('');
  readonly sellerFilter = signal<number | null>(null);
  readonly sellers = this.userVendeurService.vendeurs;

  // ===== Computed =====
  readonly filteredSales = computed(() => {
    const sales = this.pendingSales();
    /* const search = this.searchTerm().toLowerCase();
     const sellerId = this.sellerFilter();

     if (search) {
       sales = sales.filter(
         sale =>
           sale.numberTransaction?.toLowerCase().includes(search) ||
           sale.customer?.fullName?.toLowerCase().includes(search) ||
           sale.seller?.abbrName?.toLowerCase().includes(search),
       );
     }

     if (sellerId) {
       sales = sales.filter(sale => sale.seller?.id === sellerId);
     }
 */
    return sales;
  });

  readonly totalAmount = computed(() => {
    return this.filteredSales().reduce((sum, sale) => sum + (sale.salesAmount || 0), 0);
  });

  readonly totalCount = computed(() => this.filteredSales().length);

  // ===== Lifecycle =====

  ngOnInit(): void {
    // Sélectionner l'utilisateur connecté par défaut
    const currentSeller = this.seller();
    if (currentSeller) {
      this.sellerFilter.set(currentSeller.id);
    }

    this.facade.loadPendingSales(this.buildParameters());
  }

  // ===== Actions =====

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchTerm.set(target.value);
    this.onRefresh();
  }

  onSellerChange(): void {
    this.onRefresh();
  }

  onSelectSale(sale: ISales): void {
    this.selectedSale.set(sale);
  }

  onResumeSale(sale: ISales): void {
    if (!sale.saleId) {
      return;
    }

    this.facade.resumePendingSale(sale.saleId);

    this.saleResumed.emit(sale);
    this.closed.emit();
  }

  onRefresh(): void {
    this.facade.loadPendingSales(this.buildParameters());
  }



  // ===== Helpers =====

  getSaleTypeLabel(type: string): string {
    switch (type) {
      case 'COMPTANT':
        return 'Comptant';
      case 'ASSURANCE':
        return 'Assurance';
      case 'CARNET':
        return 'Carnet';
      default:
        return type;
    }
  }

  getSaleTypeSeverity(type: string): string {
    switch (type) {
      case 'COMPTANT':
        return 'success';
      case 'ASSURANCE':
        return 'info';
      case 'CARNET':
        return 'warning';
      default:
        return 'secondary';
    }
  }

  getRowClass(sale: ISales): string {
    return this.selectedSale()?.id === sale.id ? 'selected-row' : '';
  }

  private buildParameters(): any {
    const params: any = {statut: [SalesStatut.ACTIVE]};

    const sellerId = this.sellerFilter();
    if (sellerId) {
      params.sellerId = sellerId;
    }

    const search = this.searchTerm();
    if (search) {
      params.search = search;
    }
    return params;
  }
}
