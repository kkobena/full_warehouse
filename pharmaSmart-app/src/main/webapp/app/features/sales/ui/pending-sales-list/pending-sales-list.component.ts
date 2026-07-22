import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  output,
  signal
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {
  AppBadgeSeverity,
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  RowTogglerDirective,
  SelectComponent
} from '../../../../shared/ui';
import {ISales, SalesStatut} from '../../../../shared/model';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {UserVendeurService} from '../../../../entities/sales/service/user-vendeur.service';

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
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    DataTableComponent,
    ButtonComponent,
    SelectComponent,
    IconFieldComponent,
    NgbTooltip,
    BadgeComponent,
    RowTogglerDirective,

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
    return this.pendingSales();
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

  getSaleTypeSeverity(type: string): AppBadgeSeverity {
    switch (type) {
      case 'COMPTANT':
        return 'success';
      case 'ASSURANCE':
        return 'info';
      case 'CARNET':
        return 'warn';
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
      params.userId = sellerId;
    }

    const search = this.searchTerm();
    if (search) {
      params.search = search;
    }
    return params;
  }
}
