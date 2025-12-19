import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RepartitionStockService } from '../repartition-stock.service';
import { IStockProduit } from '../../../shared/model/stock-produit.model';
import { ButtonModule } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { StorageService } from '../../../entities/storage/storage.service';
import { HttpResponse } from '@angular/common/http';
import { IStorage } from '../../../shared/model/magasin.model';
import { Toast } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import {
  ProduitSearchAutocompleteScannerComponent
} from '../../../shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
import { IProduit, ProduitSearch, ProduitStockSearch } from '../../../shared/model/produit.model';
import { QuantiteProdutSaisieComponent } from '../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ConfirmDialog } from 'primeng/confirmdialog';

export interface IManualRepartitionRequest {
  stockSourceId: number;
  stockDestinationId: number;
  quantity: number;
  seuilMini?: number;
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
  selector: 'jhi-manual-repartition',
  templateUrl: './manual-repartition.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ButtonModule,
    InputNumber,
    Select,
    TableModule,
    TooltipModule,
    Toast,
    ProduitSearchAutocompleteScannerComponent,
    QuantiteProdutSaisieComponent,
    ConfirmDialogComponent,
    ConfirmDialog
  ],
  providers: [MessageService, ConfirmationService],
  styleUrl: './manual-repartition.component.scss'
})
export class ManualRepartitionComponent implements OnInit {
  protected repartitionService = inject(RepartitionStockService);
  protected storageService = inject(StorageService);
  protected messageService = inject(MessageService);
  protected confirmationService = inject(ConfirmationService);
  protected produitbox = viewChild<ProduitSearchAutocompleteScannerComponent>('produitbox');
  protected quantityBox = viewChild<QuantiteProdutSaisieComponent>('quantityBox');
  protected confirmDialog = viewChild<ConfirmDialogComponent>('confirmDialog');

  allStocks: IStockProduit[] = [];
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
      }
    });
  }

  protected onStorageChange(): void {
    this.produitSelected = null;
    this.repartitionRows = [];
    this.produitbox()?.getFocus();
  }

  /**
   * Handle product selection from autocomplete
   * Use stocks from ProduitSearch and check for destination
   */
  protected onSelectProduct(produit: ProduitSearch | null): void {
    if (!produit) {
      return;
    }

    if (!this.selectedStorageId) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Emplacement',
        detail: 'Veuillez sélectionner un emplacement'
      });
      this.produitSelected = null;
      return;
    }

    // Find stock in selected storage
    const stockInSelectedStorage = produit.stocks?.find(s => s.storage === this.selectedStorageId);

    if (!stockInSelectedStorage) {
      this.messageService.add({
        severity: 'info',
        summary: 'Recherche',
        detail: 'Aucun stock trouvé pour ce produit dans l\'emplacement sélectionné'
      });
      this.produitSelected = null;
      this.produitbox()?.getFocus();
      return;
    }

    // Check if stock quantity is > 0
    if (!stockInSelectedStorage.quantite || stockInSelectedStorage.quantite <= 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Stock insuffisant',
        detail: 'La quantité disponible dans cet emplacement est insuffisante pour effectuer une répartition'
      });
      this.produitSelected = null;
      this.produitbox()?.getFocus();
      return;
    }

    // Check if there are other stocks (destinations)
    const otherStocks = produit.stocks?.filter(s => s.storage !== this.selectedStorageId) || [];

    if (otherStocks.length === 0) {
      // No destination found - show confirm dialog
      this.confirmDialog()?.onConfirm(
        () => this.createNewDestinationRow(produit, stockInSelectedStorage),
        'Création de destination',
        'Aucune destination disponible pour ce produit. Voulez-vous créer une nouvelle destination ?',
        'pi pi-question-circle',
        () => {
          this.produitSelected = null;
          this.produitbox()?.getFocus();
        }
      );
    } else {
      // Stock destination found - focus quantity input
      this.quantityBox()?.focusProduitControl();
    }
  }

  /**
   * Create a row with new destination creation form
   */
  protected createNewDestinationRow(produit: ProduitSearch, stockSource: ProduitStockSearch): void {
    // Safety check: ensure stock quantity is > 0
    if (!stockSource.quantite || stockSource.quantite <= 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Stock insuffisant',
        detail: 'La quantité disponible est insuffisante pour créer une répartition'
      });
      this.produitSelected = null;
      this.produitbox()?.getFocus();
      return;
    }

    const stockProduit: IStockProduit = {
      id: stockSource.storage,
      qtyStock: stockSource.quantite,
      storage: this.storages.find(s => s.id === stockSource.storage),
      produit: {
        id: produit.id,
        libelle: produit.libelle,
        codeCip: produit.fournisseurProduit?.codeCip,
        stockProduits: [] as IStockProduit[]
      }
    };

    const newRow: IRepartitionRow = {
      id: `row-${Date.now()}-${Math.random()}`,
      stockSource: stockProduit,
      quantity: 1,
      isValid: false,
      errors: [],
      availableDestinations: [],
      createNewDestination: true,
      seuilMini: 1
    };

    this.validateRow(newRow);
    this.repartitionRows.push(newRow);

    this.produitSelected = null;
  }

  /**
   * Handle quantity input and add rows
   */
  protected addQuantity(quantity: number): void {
    if (!this.produitSelected || !quantity || quantity <= 0 || !this.selectedStorageId) {
      return;
    }

    // Find stock in selected storage
    const stockInSelectedStorage = this.produitSelected.stocks?.find(s => s.storage === this.selectedStorageId);

    if (!stockInSelectedStorage) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Stock',
        detail: 'Stock source non trouvé'
      });
      return;
    }

    // Check if stock quantity is > 0
    if (!stockInSelectedStorage.quantite || stockInSelectedStorage.quantite <= 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Stock insuffisant',
        detail: 'La quantité disponible dans cet emplacement est insuffisante pour effectuer une répartition'
      });
      return;
    }

    // Map other stocks to IStockProduit for destinations
    const otherStocks = this.produitSelected.stocks?.filter(s => s.storage !== this.selectedStorageId) || [];
    const availableDestinations: IStockProduit[] = otherStocks.map(stock => ({
      id: stock.storage,
      qtyStock: stock.quantite,
      storage: this.storages.find(s => s.id === stock.storage),
      produit: {
        id: this.produitSelected!.id,
        libelle: this.produitSelected!.libelle,
        codeCip: this.produitSelected!.fournisseurProduit?.codeCip,
        stockProduits: [] as IStockProduit[]
      }
    }));

    const stockSource: IStockProduit = {
      id: stockInSelectedStorage.storage,
      qtyStock: stockInSelectedStorage.quantite,
      storage: this.storages.find(s => s.id === stockInSelectedStorage.storage),
      produit: {
        id: this.produitSelected.id,
        libelle: this.produitSelected.libelle,
        codeCip: this.produitSelected.fournisseurProduit?.codeCip,
        stockProduits: availableDestinations
      }
    };

    this.addRow(stockSource, quantity);

    this.messageService.add({
      severity: 'success',
      summary: 'Ajout',
      detail: `Produit ajouté avec quantité ${quantity}`
    });

    // Clear selection and focus back to search
    this.produitSelected = null;
    this.quantityBox()?.reset();
    this.produitbox()?.getFocus();
  }

  protected addRow(stockSource: IStockProduit, quantity: number = 1): void {
    const availableDestinations = this.getAvailableDestinations(stockSource);

    const newRow: IRepartitionRow = {
      id: `row-${Date.now()}-${Math.random()}`,
      stockSource,
      quantity: quantity,
      isValid: false,
      errors: [],
      availableDestinations
    };

    this.validateRow(newRow);
    this.repartitionRows.push(newRow);
  }

  protected getAvailableDestinations(stockSource: IStockProduit): IStockProduit[] {
    if (!stockSource || !stockSource.produit?.stockProduits) {
      return [];
    }

    const produitStocks = stockSource.produit.stockProduits;

    // Si la source est SAFETY_STOCK (Reserve), afficher uniquement le rayon (PRINCIPAL)
    if (stockSource.storage?.storageType === 'SAFETY_STOCK') {
      return produitStocks.filter(sp => sp.storage?.storageType === 'PRINCIPAL' && sp.id !== stockSource.id);
    }

    // Sinon, afficher tous les autres stocks sauf la source
    return produitStocks.filter(sp => sp.id !== stockSource.id);
  }

  protected removeRow(row: IRepartitionRow): void {
    this.repartitionRows = this.repartitionRows.filter(r => r.id !== row.id);
  }



  protected onQuantityChange(row: IRepartitionRow): void {
    this.validateRow(row);
  }

  protected onSeuilMiniChange(row: IRepartitionRow): void {
    this.validateRow(row);
  }

  protected validateRow(row: IRepartitionRow): void {
    row.errors = [];
    row.isValid = true;

    if (!row.stockSource) {
      row.errors.push('Stock source requis');
      row.isValid = false;
    }

    // Check destination only if not creating a new one
    if (!row.createNewDestination && !row.stockDestination) {
      row.errors.push('Stock destination requis');
      row.isValid = false;
    }

    // Check seuil mini if creating new destination
    if (row.createNewDestination) {
      if (row.seuilMini === undefined || row.seuilMini === null) {
        row.errors.push('Seuil minimum requis');
        row.isValid = false;
      } else if (row.seuilMini <= 0) {
        row.errors.push('Seuil minimum doit être > 0');
        row.isValid = false;
      }
    }

    if (row.stockSource && row.stockDestination && row.stockSource.id === row.stockDestination.id) {
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

  protected get maxQuantityForRow(): (row: IRepartitionRow) => number {
    return (row: IRepartitionRow) => row.stockSource?.qtyStock || 0;
  }

  protected get isFormValid(): boolean {
    return this.repartitionRows.length > 0 && this.repartitionRows.every(row => row.isValid);
  }

  protected save(): void {
    if (!this.isFormValid) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation',
        detail: 'Veuillez corriger les erreurs avant de soumettre'
      });
      return;
    }

    const requests: IManualRepartitionRequest[] = this.repartitionRows
      .filter(row => row.isValid && row.stockSource && (row.stockDestination || row.createNewDestination))
      .map(row => ({
        stockSourceId: row.stockSource!.id!,
        stockDestinationId: row.stockDestination?.id ,
        quantity: row.quantity,
        seuilMini: row.seuilMini
      }));

    this.isSaving = true;
    this.repartitionService.processManualRepartition(requests).subscribe({
      next: () => {
        this.isSaving = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `${requests.length} répartition(s) effectuée(s) avec succès`
        });
        this.repartitionRows = [];
        this.produitSelected = null;
        this.produitbox()?.getFocus();
      },
      error: () => {
        this.isSaving = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du traitement des répartitions'
        });
      }
    });
  }



  get validRowsCount(): number {
    return this.repartitionRows?.filter(r => r.isValid)?.length ?? 0;
  }

  /**
   * Check if we can create a reserve for this row
   * Can create if source is PRINCIPAL (Rayon) and no SAFETY_STOCK exists
   */
  protected canCreateReserve(row: IRepartitionRow): boolean {
    if (!row.stockSource || row.stockSource.storage?.storageType !== 'PRINCIPAL') {
      return false;
    }

    // Check if there's already a SAFETY_STOCK for this product
    const hasReserve = row.stockSource.produit?.stockProduits?.some(sp => sp.storage?.storageType === 'SAFETY_STOCK');
    return !hasReserve;
  }

  /**
   * Enable creation of new destination
   */
  protected enableCreateDestination(row: IRepartitionRow): void {
    row.createNewDestination = true;
    row.stockDestination = undefined;
    this.validateRow(row);
  }




}
