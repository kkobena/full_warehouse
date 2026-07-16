import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { Toast } from 'primeng/toast';
import { ProduitSearchAutocompleteScannerComponent } from 'app/shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
import { QuantiteProdutSaisieComponent } from 'app/shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { NotificationService } from 'app/shared/services/notification.service';
import { IStockProduit } from 'app/shared/model/stock-produit.model';
import { IStorage } from 'app/shared/model/magasin.model';
import { ProduitSearch } from 'app/shared/model/produit.model';
import { RepartitionStockService } from '../../../../../../entities/repartition-stock/repartition-stock.service';
import { StorageService } from '../../../../../../entities/storage/storage.service';

export interface IManualRepartitionRequest {
  stockSourceId: number;
  stockDestinationId: number | null;
  quantity: number;
  seuilMini?: number;
  createNewDestination: boolean;
}

export interface IRepartitionRow {
  id: string;
  stockSource?: IStockProduit;
  stockDestination?: IStockProduit;
  quantity: number;
  isValid: boolean;
  errors: string[];
  availableDestinations: IStockProduit[];
  createNewDestination?: boolean;
  newDestinationStorageId?: number;
  seuilMini?: number;
}

@Component({
  selector: 'app-manual-repartition',
  templateUrl: './manual-repartition.component.html',
  styleUrls: ['./manual-repartition.scss'],
  imports: [
    FormsModule,
    ButtonModule,
    Select,
    TableModule,
    TooltipModule,
    Toast,
    ProduitSearchAutocompleteScannerComponent,
    QuantiteProdutSaisieComponent,
  ],
})
export class AppManualRepartitionComponent implements OnInit {
  protected repartitionService = inject(RepartitionStockService);
  protected storageService = inject(StorageService);
  private readonly notificationService = inject(NotificationService);
  protected produitbox = viewChild<ProduitSearchAutocompleteScannerComponent>('produitbox');
  protected quantityBox = viewChild<QuantiteProdutSaisieComponent>('quantityBox');

  repartitionRows: IRepartitionRow[] = [];
  isSaving = false;
  produitSelected: ProduitSearch | null = null;
  selectedStorageId?: number | null = null;
  storages: IStorage[] = [];

  ngOnInit(): void {
    this.loadStorages();
  }

  protected loadStorages(): void {
    this.storageService.fetchUserStorages().subscribe({
      next: (res: HttpResponse<IStorage[]>) => {
        this.storages = res.body || [];
      },
    });
  }

  protected onStorageChange(): void {
    this.produitSelected = null;
    this.repartitionRows = [];
    this.setProductBoxFocus();
  }

  protected onSelectProduct(produit: ProduitSearch | null): void {
    if (!produit) {
      return;
    }
    if (!this.selectedStorageId) {
      this.notificationService.warning('Veuillez sélectionner un emplacement', 'Emplacement');
      this.produitSelected = null;
      return;
    }
    const stockInSelectedStorage = produit.stocks?.find(s => s.storage === this.selectedStorageId);
    if (!stockInSelectedStorage) {
      this.notificationService.info("Aucun stock trouvé pour ce produit dans l'emplacement sélectionné", 'Recherche');
      this.produitSelected = null;
      this.setProductBoxFocus();
      return;
    }
    if (!stockInSelectedStorage.quantite || stockInSelectedStorage.quantite <= 0) {
      this.notificationService.warning(
        'La quantité disponible dans cet emplacement est insuffisante pour effectuer une répartition',
        'Stock insuffisant',
      );
      this.produitSelected = null;
      this.setProductBoxFocus();
      return;
    }
    const otherStocks = produit.stocks?.filter(s => s.storage !== this.selectedStorageId) || [];
    if (otherStocks.length === 0) {
      this.notificationService.warning(
        'Ce produit ne possède de réserve. Vous devez en créer une pour pouvoir effectuer une répartition.',
        'Stock Réserve',
      );
      this.produitSelected = null;
      this.setProductBoxFocus();
    } else {
      this.quantityBox()?.focusProduitControl();
    }
  }

  protected addQuantity(quantity: number): void {
    if (!this.produitSelected || !quantity || quantity <= 0 || !this.selectedStorageId) {
      return;
    }
    const stockInSelectedStorage = this.produitSelected.stocks?.find(s => s.storage === this.selectedStorageId);
    if (!stockInSelectedStorage) {
      this.notificationService.warning('Stock source non trouvé', 'Stock');
      return;
    }
    if (!stockInSelectedStorage.quantite || stockInSelectedStorage.quantite <= 0) {
      this.notificationService.warning(
        'La quantité disponible dans cet emplacement est insuffisante pour effectuer une répartition',
        'Stock insuffisant',
      );
      return;
    }
    const otherStocks = this.produitSelected.stocks?.filter(s => s.storage !== this.selectedStorageId) || [];
    const availableDestinations: IStockProduit[] = otherStocks.map(stock => ({
      id: stock.id,
      qtyStock: stock.quantite,
      storage: this.storages.find(s => s.id === stock.storage),
      produit: {
        id: this.produitSelected.id,
        libelle: this.produitSelected.libelle,
        codeCip: this.produitSelected.fournisseurProduit?.codeCip,
        stockProduits: [] as IStockProduit[],
      },
    }));
    const stockSource: IStockProduit = {
      id: stockInSelectedStorage.id,
      qtyStock: stockInSelectedStorage.quantite,
      storage: this.storages.find(s => s.id === stockInSelectedStorage.storage),
      produit: {
        id: this.produitSelected.id,
        libelle: this.produitSelected.libelle,
        codeCip: this.produitSelected.fournisseurProduit?.codeCip,
        stockProduits: availableDestinations,
      },
    };
    this.addRow(stockSource, quantity);
    this.produitSelected = null;
    this.quantityBox()?.reset();
    this.setProductBoxFocus();
  }

  protected addRow(stockSource: IStockProduit, quantity = 1): void {
    const availableDestinations = this.getAvailableDestinations(stockSource);
    const newRow: IRepartitionRow = {
      id: `row-${Date.now()}-${Math.random()}`,
      stockSource,
      stockDestination: availableDestinations.length > 0 ? availableDestinations[0] : undefined,
      quantity,
      isValid: false,
      errors: [],
      availableDestinations,
    };
    this.validateRow(newRow);
    this.repartitionRows.push(newRow);
  }

  protected getAvailableDestinations(stockSource: IStockProduit): IStockProduit[] {
    if (!stockSource?.produit?.stockProduits) {
      return [];
    }
    return stockSource.produit.stockProduits.filter(sp => sp.id !== stockSource.id);
  }

  protected removeRow(row: IRepartitionRow): void {
    this.repartitionRows = this.repartitionRows.filter(r => r.id !== row.id);
  }

  protected validateRow(row: IRepartitionRow): void {
    row.errors = [];
    row.isValid = true;
    if (!row.stockSource) {
      row.errors.push('Stock source requis');
      row.isValid = false;
    }
    // Destination valide si : stockDestination sélectionné OU createNewDestination = true
    if (!row.stockDestination && !row.createNewDestination) {
      row.errors.push('Stock destination requis');
      row.isValid = false;
    }
    if (row.stockSource?.id && row.stockDestination?.id && row.stockSource.id === row.stockDestination.id) {
      row.errors.push('Source et destination doivent être différents');
      row.isValid = false;
    }
    if (!row.quantity || row.quantity < 1) {
      row.errors.push('Quantité doit être >= 1');
      row.isValid = false;
    }
    if (row.stockSource && row.quantity > (row.stockSource.qtyStock || 0)) {
      row.errors.push(`Quantité max: ${row.stockSource.qtyStock || 0}`);
      row.isValid = false;
    }
  }

  protected get isFormValid(): boolean {
    return this.repartitionRows.length > 0 && this.repartitionRows.every(row => row.isValid);
  }

  protected save(): void {
    if (!this.isFormValid) {
      this.notificationService.warning('Veuillez corriger les erreurs avant de soumettre', 'Validation');
      return;
    }
    const requests: IManualRepartitionRequest[] = this.repartitionRows
      .filter(row => row.isValid && row.stockSource && (row.stockDestination || row.createNewDestination))
      .map(row => ({
        stockSourceId: row.stockSource!.id,
        stockDestinationId: row.stockDestination?.id ?? null,
        quantity: row.quantity,
        seuilMini: row.seuilMini,
        createNewDestination: row.createNewDestination === true,
      }));
    this.isSaving = true;
    this.repartitionService.processManualRepartition(requests).subscribe({
      next: () => {
        this.isSaving = false;
        this.notificationService.success(`${requests.length} répartition(s) effectuée(s) avec succès`);
        this.repartitionRows = [];
        this.produitSelected = null;
        this.setProductBoxFocus();
      },
      error: () => {
        this.isSaving = false;
        this.notificationService.error('Erreur lors du traitement des répartitions');
      },
    });
  }

  get validRowsCount(): number {
    return this.repartitionRows?.filter(r => r.isValid)?.length ?? 0;
  }

  protected canCreateReserve(row: IRepartitionRow): boolean {
    if (!row.stockSource || row.stockSource.storage?.type !== 'PRINCIPAL') {
      return false;
    }
    return !row.stockSource.produit?.stockProduits?.some(sp => sp.storage?.type === 'SAFETY_STOCK');
  }

  protected enableCreateDestination(row: IRepartitionRow): void {
    row.createNewDestination = true;
    row.stockDestination = undefined;
    this.validateRow(row);
  }

  protected cancelCreateDestination(row: IRepartitionRow): void {
    row.createNewDestination = false;
    row.stockDestination = row.availableDestinations.length > 0 ? row.availableDestinations[0] : undefined;
    this.validateRow(row);
  }

  getDestinationAfterQty(row: IRepartitionRow): number {
    return Number(row?.stockDestination?.qtyStock ?? 0) + Number(row?.quantity ?? 0);
  }

  private setProductBoxFocus(): void {
    setTimeout(() => this.produitbox()?.getFocus(), 50);
  }
}
