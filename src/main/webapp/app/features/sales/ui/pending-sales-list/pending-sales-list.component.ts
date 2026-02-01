import { Component, inject, OnInit, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { SelectModule } from 'primeng/select';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { ISales, SaleId } from '../../../../shared/model/sales.model';
import { IUser } from '../../../../core/user/user.model';
import { SalesFacade } from '../../data-access/facades/sales.facade';

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

  // ===== Outputs =====
  readonly saleResumed = output<ISales>();
  readonly closed = output<void>();

  // ===== State from Facade =====
  readonly pendingSales = this.facade.pendingSales;
  readonly loading = this.facade.loading;
  readonly seller = this.facade.seller;

  // ===== Local State Signals =====
  readonly selectedSale = signal<ISales | null>(null);
  readonly searchTerm = signal<string>('');
  readonly saleTypeFilter = signal<string>('TOUT');

  // ===== Computed =====
  readonly filteredSales = computed(() => {
    let sales = this.pendingSales();
    const search = this.searchTerm().toLowerCase();
    const typeFilter = this.saleTypeFilter();
    
    if (search) {
      sales = sales.filter(sale => 
        sale.numberTransaction?.toLowerCase().includes(search) ||
        sale.customer?.fullName?.toLowerCase().includes(search) ||
        sale.seller?.abbrName?.toLowerCase().includes(search)
      );
    }

    if (typeFilter !== 'TOUT') {
      sales = sales.filter(sale => sale.natureVente === typeFilter);
    }
    
    return sales;
  });

  readonly totalAmount = computed(() => {
    return this.filteredSales().reduce((sum, sale) => sum + (sale.salesAmount || 0), 0);
  });

  readonly totalCount = computed(() => this.filteredSales().length);

  // ===== Data =====
  readonly saleTypeOptions = [
    { label: 'Tous', value: 'TOUT' },
    { label: 'Comptant', value: 'VO' },
    { label: 'Assurance', value: 'VA' },
  ];

  // ===== Lifecycle =====

  ngOnInit(): void {
    this.facade.loadPendingSales();
  }

  // ===== Actions =====

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchTerm.set(target.value);
  }

  onSaleTypeChange(): void {
    // Filter is applied via computed signal
  }

  onSelectSale(sale: ISales): void {
    this.selectedSale.set(sale);
  }

  onResumeSale(sale: ISales): void {
    if (!sale.saleId) {
      console.error('Sale has no saleId');
      return;
    }

    this.facade.resumePendingSale(sale.saleId);
    this.saleResumed.emit(sale);
    this.closed.emit();
  }

  onDeleteSale(sale: ISales, event: Event): void {
    event.stopPropagation();
    
    if (!sale.saleId) {
      console.error('Sale has no saleId');
      return;
    }

    const confirmDelete = confirm(
      `Voulez-vous vraiment supprimer la vente ${sale.numberTransaction} ?\n\n` +
      `Cette action est irréversible.`
    );

    if (!confirmDelete) {
      return;
    }

    this.facade.deletePendingSale(sale.saleId);
  }

  onRefresh(): void {
    this.facade.loadPendingSales();
  }

  onClose(): void {
    this.closed.emit();
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
}
