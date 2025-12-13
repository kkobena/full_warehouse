import { Component, inject, OnInit } from '@angular/core';
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
import { Toolbar } from 'primeng/toolbar';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { IStorage } from '../../../shared/model/magasin.model';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

export interface IManualRepartitionRequest {
  stockSourceId: number;
  stockDestinationId: number;
  quantity: number;
}

export interface IRepartitionRow {
  id: string;
  stockSource?: IStockProduit;
  stockDestination?: IStockProduit;
  quantity: number;
  isValid: boolean;
  errors: string[];
  availableDestinations: IStockProduit[];
}

@Component({
  selector: 'jhi-manual-repartition',
  templateUrl: './manual-repartition.component.html',
  imports: [WarehouseCommonModule, FormsModule, ButtonModule, InputNumber, Select, TableModule, TooltipModule, Toolbar, IconField, InputIcon, InputText, Toast],
  providers: [MessageService],
  styleUrl: './manual-repartition.component.scss',
})
export class ManualRepartitionComponent implements OnInit {
  protected repartitionService = inject(RepartitionStockService);
  protected storageService = inject(StorageService);
  protected messageService = inject(MessageService);

  allStocks: IStockProduit[] = [];
  repartitionRows: IRepartitionRow[] = [];
  isSaving = false;
  searchTerm = '';
  selectedStorageId?: number;
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
    this.searchTerm = '';
    this.repartitionRows = [];
  }

  protected onSearchProduct(): void {
    if (!this.searchTerm || this.searchTerm.length < 3) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Recherche',
        detail: 'Veuillez saisir au moins 3 caractères pour la recherche',
      });
      return;
    }

    if (!this.selectedStorageId) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Emplacement',
        detail: 'Veuillez sélectionner un emplacement',
      });
      return;
    }

    this.isSaving = true;
    this.repartitionService.searchStockProduitsForRepartition(this.selectedStorageId, this.searchTerm).subscribe({
      next: res => {
        this.isSaving = false;
        const results = res.body || [];

        if (results.length === 0) {
          this.messageService.add({
            severity: 'info',
            summary: 'Recherche',
            detail: 'Aucun produit trouvé avec ce critère de recherche',
          });
          return;
        }

        // Convert search results to stock produits and add rows
        results.forEach(result => {
          const stockSource = this.mapSearchResultToStockProduit(result);
          this.addRow(stockSource);
        });

        this.messageService.add({
          severity: 'success',
          summary: 'Recherche',
          detail: `${results.length} produit(s) trouvé(s)`,
        });
      },
      error: () => {
        this.isSaving = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors de la recherche des produits',
        });
      },
    });
  }

  protected mapSearchResultToStockProduit(result: any): IStockProduit {
    const stockProduit: IStockProduit = {
      id: result.id,
      qtyStock: result.qtyStock,
      seuilMini: result.seuilMini,
      storage: {
        id: result.storageId,
        name: result.storageName,
        storageType: result.storageType,
      },
      produit: {
        id: result.produitId,
        libelle: result.produitLibelle,
        codeCip: result.produitCodeCip,
        stockProduits: result.allStocks || [],
      },
    };

    return stockProduit;
  }

  protected addRow(stockSource: IStockProduit): void {
    const availableDestinations = this.getAvailableDestinations(stockSource);

    const newRow: IRepartitionRow = {
      id: `row-${Date.now()}-${Math.random()}`,
      stockSource,
      quantity: 1,
      isValid: false,
      errors: [],
      availableDestinations,
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

  protected onDestinationChange(row: IRepartitionRow, stockId: number): void {
    row.stockDestination = row.availableDestinations.find(s => s.id === stockId);
    this.validateRow(row);
  }

  protected onQuantityChange(row: IRepartitionRow): void {
    this.validateRow(row);
  }

  protected validateRow(row: IRepartitionRow): void {
    row.errors = [];
    row.isValid = true;

    if (!row.stockSource) {
      row.errors.push('Stock source requis');
      row.isValid = false;
    }

    if (!row.stockDestination) {
      row.errors.push('Stock destination requis');
      row.isValid = false;
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
        detail: 'Veuillez corriger les erreurs avant de soumettre',
      });
      return;
    }

    const requests: IManualRepartitionRequest[] = this.repartitionRows
      .filter(row => row.isValid && row.stockSource && row.stockDestination)
      .map(row => ({
        stockSourceId: row.stockSource!.id!,
        stockDestinationId: row.stockDestination!.id!,
        quantity: row.quantity,
      }));

    this.isSaving = true;
    this.repartitionService.processManualRepartition(requests).subscribe({
      next: () => {
        this.isSaving = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `${requests.length} répartition(s) effectuée(s) avec succès`,
        });
        this.repartitionRows = [];
        this.searchTerm = '';
      },
      error: () => {
        this.isSaving = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du traitement des répartitions',
        });
      },
    });
  }

  protected reset(): void {
    this.repartitionRows = [];
    this.searchTerm = '';
  }

  get validRowsCount(): number {
    return this.repartitionRows?.filter(r => r.isValid)?.length ?? 0;
  }
}
