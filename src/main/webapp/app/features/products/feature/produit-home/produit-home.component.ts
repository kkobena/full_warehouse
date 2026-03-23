import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpHeaders } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent } from 'primeng/table';
import { MenuItem, SelectItem } from 'primeng/api';
import { TooltipModule } from 'primeng/tooltip';
import { Authority } from 'app/shared/constants/authority.constants';
import { IProduit } from 'app/shared/model/produit.model';
import { IFamilleProduit } from 'app/shared/model/famille-produit.model';
import { IRayon } from 'app/shared/model/rayon.model';
import { FamilleProduitService } from 'app/entities/famille-produit/famille-produit.service';
import { RayonService } from 'app/entities/rayon/rayon.service';
import { ConfigurationService } from 'app/shared/configuration.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { ToastAlertComponent } from 'app/shared/toast-alert/toast-alert.component';
import { ProductsApiService } from '../../data-access/services/products-api.service';
import { ProduitListComponent, ProduitMenuAction } from '../../ui/produit-list/produit-list.component';
import { ProduitDetailPanelComponent } from '../../ui/produit-detail-panel/produit-detail-panel.component';

@Component({
  selector: 'app-produit-home',
  templateUrl: './produit-home.component.html',
  styleUrls: ['./produit-home.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    SplitButtonModule,
    ToolbarModule,
    IconField,
    InputIcon,
    TooltipModule,
    WarehouseCommonModule,
    ProduitListComponent,
    ProduitDetailPanelComponent,
  ],
})
export class ProduitHomeComponent implements OnInit {
  protected readonly Authority = Authority;

  protected produits = signal<IProduit[]>([]);
  protected totalItems = signal(0);
  protected loading = signal(false);
  protected selectedProduit = signal<IProduit | null>(null);
  protected panelOpen = computed(() => this.selectedProduit() !== null);
  protected selectedProduits = signal<IProduit[]>([]);
  protected hasSelection = computed(() => this.selectedProduits().length > 0);
  protected clearSelectionTrigger = signal(0);

  protected familles = signal<IFamilleProduit[]>([]);
  protected rayons = signal<IRayon[]>([]);
  protected filterOptions: SelectItem[] = [
    { label: 'Produits actifs', value: 'ENABLE' },
    { label: 'Produits désactivés', value: 'DISABLE' },
    { label: 'Déconditionnables', value: 'DECONDITIONNABLE' },
    { label: 'Déconditionnés', value: 'DECONDITIONNE' },
    { label: 'Tous', value: 'ALL' },
  ];

  protected search = '';
  protected selectedFilter = 'ENABLE';
  protected selectedFamilleId: number | null = null;
  protected selectedRayonId: number | null = null;
  protected page = 0;
  protected rows = 15;
  protected sortField = 'libelle';
  protected sortOrder = 1;

  protected importMenuItems: MenuItem[] = [
    { label: 'Nouvelle installation', icon: 'pi pi-file-excel', command: () => this.onImport('NOUVELLE_INSTALLATION') },
    { label: 'Basculement', icon: 'pi pi-filter', command: () => this.onImport('BASCULEMENT') },
    { label: 'Basculement prestige', icon: 'pi pi-file', command: () => this.onImport('BASCULEMENT_PRESTIGE') },
  ];

  private readonly api = inject(ProductsApiService);
  private readonly familleService = inject(FamilleProduitService);
  private readonly rayonService = inject(RayonService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly configurationService = inject(ConfigurationService);

  ngOnInit(): void {
    this.loadReferentiels();
    this.loadPage();
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;
    if (event.sortField) {
      this.sortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      this.sortOrder = event.sortOrder ?? 1;
    }
    this.loadPage();
  }

  protected onSearch(): void {
    this.page = 0;
    this.loadPage();
  }

  protected onFilterChange(): void {
    this.page = 0;
    this.loadPage();
  }

  protected onProduitSelected(produit: IProduit): void {
    this.selectedProduit.set(produit);
  }

  protected onClosePanel(): void {
    this.selectedProduit.set(null);
  }

  protected onEditRequested(produit: IProduit): void {
    this.router.navigate(['/produit', produit.id, 'edit']);
  }

  protected onDeleteRequested(produit: IProduit): void {
    this.confirmDialog.onConfirm(
      () => this.deleteProduit(produit),
      'Suppression',
      `Voulez-vous supprimer le produit "${produit.libelle}" ?`,
    );
  }

  protected onNewProduit(): void {
    this.router.navigate(['/produit/new']);
  }

  protected onSelectionChanged(produits: IProduit[]): void {
    this.selectedProduits.set(produits);
  }

  protected onBulkDisable(): void {
    const list = this.selectedProduits();
    const count = list.length;
    this.confirmDialog.onConfirm(
      () => this.executeBulk(list, 'DISABLE'),
      'Mettre en veille',
      `Mettre en veille ${count} produit(s) sélectionné(s) ?`,
    );
  }

  protected onBulkEnable(): void {
    const list = this.selectedProduits();
    const count = list.length;
    this.confirmDialog.onConfirm(
      () => this.executeBulk(list, 'ENABLE'),
      'Réactiver',
      `Réactiver ${count} produit(s) sélectionné(s) ?`,
    );
  }

  protected onClearSelection(): void {
    this.selectedProduits.set([]);
    this.clearSelectionTrigger.update(v => v + 1);
  }

  private executeBulk(list: IProduit[], status: 'ENABLE' | 'DISABLE'): void {
    let completed = 0;
    for (const produit of list) {
      this.api.patchStatus(produit.id!, status).subscribe({
        next: () => {
          completed++;
          this.produits.update(all =>
            all.map(p => p.id === produit.id ? { ...p, status: status === 'ENABLE' ? 0 : 1 } : p),
          );
          if (completed === list.length) {
            this.selectedProduits.set([]);
            this.clearSelectionTrigger.update(v => v + 1);
          }
        },
      });
    }
  }

  protected onMenuAction(event: { action: ProduitMenuAction; produit: IProduit }): void {
    const { action, produit } = event;
    switch (action) {
      case 'view':
        this.selectedProduit.set(produit);
        break;
      case 'commander':
        this.router.navigate(['/commande'], { queryParams: { produitId: produit.id } });
        break;
      case 'lots':
        this.router.navigate(['/produit', produit.id, 'lots']);
        break;
      case 'generiques':
        this.router.navigate(['/produit', produit.id, 'generiques']);
        break;
      case 'print-label':
        this.router.navigate(['/produit', produit.id, 'print-label']);
        break;
      case 'suspend':
        this.confirmDialog.onConfirm(
          () => this.changeStatus(produit, 'DISABLE'),
          'Mettre en veille',
          `Mettre en veille "${produit.libelle}" ? Le produit sera masqué des ventes et des commandes.`,
        );
        break;
      case 'activate':
        this.confirmDialog.onConfirm(
          () => this.changeStatus(produit, 'ENABLE'),
          'Réactiver',
          `Réactiver "${produit.libelle}" ?`,
        );
        break;
      case 'archive':
        this.confirmDialog.onConfirm(
          () => this.changeStatus(produit, 'DISABLE'),
          'Archiver',
          `Archiver "${produit.libelle}" ? Cette action désactivera le produit.`,
        );
        break;
    }
  }

  private changeStatus(produit: IProduit, status: 'ENABLE' | 'DISABLE'): void {
    this.api.patchStatus(produit.id!, status).subscribe({
      next: () => {
        // Mise à jour locale immédiate sans rechargement complet
        this.produits.update(list =>
          list.map(p => p.id === produit.id ? { ...p, status: status === 'ENABLE' ? 0 : 1 } : p),
        );
        if (this.selectedProduit()?.id === produit.id) {
          this.selectedProduit.update(p => p ? { ...p, status: status === 'ENABLE' ? 0 : 1 } : null);
        }
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const req: any = {
      page: this.page,
      size: this.rows,
      search: this.search || '',
      sort: [`${this.sortField},${this.sortOrder === 1 ? 'asc' : 'desc'}`],
    };

    if (this.selectedFilter === 'DECONDITIONNABLE') {
      req.deconditionnable = true;
      req.status = 'ENABLE';
    } else if (this.selectedFilter === 'DECONDITIONNE') {
      req.deconditionne = true;
      req.status = 'ENABLE';
    } else if (this.selectedFilter !== 'ALL') {
      req.status = this.selectedFilter;
    }

    if (this.selectedFamilleId) req.familleId = this.selectedFamilleId;
    if (this.selectedRayonId) req.rayonId = this.selectedRayonId;

    this.api.query(req).subscribe({
      next: (res) => this.onSuccess(res.body ?? [], res.headers),
      error: () => this.loading.set(false),
    });
  }

  private onSuccess(data: IProduit[], headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count') ?? 0));
    this.produits.set(data);
    this.loading.set(false);
    this.selectedProduits.set([]);
    this.clearSelectionTrigger.update(v => v + 1);
  }

  private deleteProduit(produit: IProduit): void {
    this.router.navigate(['/produit', produit.id, 'edit']);
  }

  private loadReferentiels(): void {
    this.familleService.query({ search: '' }).subscribe({
      next: res => this.familles.set(res.body ?? []),
    });
    this.rayonService.query({ search: '', page: 0, size: 9999 }).subscribe({
      next: res => this.rayons.set(res.body ?? []),
    });
  }

  private onImport(type: string): void {
    this.router.navigate(['/produit'], { queryParams: { import: type } });
  }
}
