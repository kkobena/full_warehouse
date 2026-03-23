import { Component, effect, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { ProduitStatService } from 'app/entities/produit/stat/produit-stat.service';
import { MagasinService } from 'app/entities/magasin/magasin.service';
import { StorageService } from 'app/entities/storage/storage.service';
import { DatePickerComponent } from 'app/shared/date-picker/date-picker.component';
import { ProduitAuditingParam, ProduitAuditingState, ProduitAuditingSum } from 'app/shared/model/produit-record.model';
import { MouvementProduit } from 'app/shared/model/enumerations/mouvement-produit.model';
import { IStorage } from 'app/shared/model/magasin.model';

@Component({
  selector: 'app-produit-mouvements-tab',
  templateUrl: './produit-mouvements-tab.component.html',
  styleUrls: ['./produit-mouvements-tab.scss'],
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    TooltipModule,
    DatePickerComponent,
  ],
})
export class ProduitMouvementsTabComponent {
  readonly produitId = input.required<number>();

  protected entites = signal<ProduitAuditingState[]>([]);
  protected loading = signal(false);
  protected hasDepot = signal(false);
  protected storages = signal<IStorage[]>([]);

  protected fromDate: Date = new Date(new Date().getFullYear(), new Date().getMonth() - 2, 1);
  protected toDate: Date = new Date();
  protected selectedStorage: IStorage | null = null;

  protected saleQuantity: number | null = null;
  protected deleveryQuantity: number | null = null;
  protected retourFournisseurQuantity: number | null = null;
  protected perimeQuantity: number | null = null;
  protected ajustementPositifQuantity: number | null = null;
  protected ajustementNegatifQuantity: number | null = null;
  protected deconPositifQuantity: number | null = null;
  protected deconNegatifQuantity: number | null = null;
  protected canceledQuantity: number | null = null;
  protected mouvementStockIn: number | null = null;
  protected mouvementStockOut: number | null = null;
  protected retourDepot: number | null = null;
  protected storeInventoryQuantity: number | null = null;

  private readonly statService = inject(ProduitStatService);
  private readonly magasinService = inject(MagasinService);
  private readonly storageService = inject(StorageService);

  private fromDateStr = '';
  private toDateStr = '';

  constructor() {
    this.magasinService.hasDepot().subscribe(res => this.hasDepot.set(res.body ?? false));
    this.storageService.fetchUserStorages().subscribe(res => this.storages.set(res.body ?? []));

    effect(() => {
      const id = this.produitId();
      if (id) {
        this.load();
      }
    });
  }

  protected onFromDateChange(dateStr: string): void {
    this.fromDateStr = dateStr;
  }

  protected onToDateChange(dateStr: string): void {
    this.toDateStr = dateStr;
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    const param = this.buildParam();
    this.statService.fetchTransactions(param).subscribe({
      next: res => {
        this.entites.set(res.body ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.statService.fetchTransactionsSum(param).subscribe({
      next: res => this.computeTotaux(res.body ?? []),
    });
  }

  protected exportPdf(): void {
    this.statService.exportToPdf(this.buildParam()).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        window.open(url);
      },
    });
  }

  private buildParam(): ProduitAuditingParam {
    return {
      produitId: this.produitId(),
      fromDate: this.fromDateStr || this.toIsoDate(this.fromDate),
      toDate: this.toDateStr || this.toIsoDate(this.toDate),
      storageId: this.selectedStorage?.id,
    };
  }

  private toIsoDate(d: Date): string {
    return d.toISOString().substring(0, 10);
  }

  private computeTotaux(summaries: ProduitAuditingSum[]): void {
    const find = (type: MouvementProduit) => summaries.find(s => s.mouvementProduitType === type)?.quantity ?? null;
    this.saleQuantity = find(MouvementProduit.SALE);
    this.deleveryQuantity = find(MouvementProduit.ENTREE_STOCK);
    this.retourFournisseurQuantity = find(MouvementProduit.RETOUR_FOURNISSEUR);
    this.perimeQuantity = find(MouvementProduit.RETRAIT_PERIME);
    this.ajustementPositifQuantity = find(MouvementProduit.AJUSTEMENT_IN);
    this.ajustementNegatifQuantity = find(MouvementProduit.AJUSTEMENT_OUT);
    this.deconPositifQuantity = find(MouvementProduit.DECONDTION_IN);
    this.deconNegatifQuantity = find(MouvementProduit.DECONDTION_OUT);
    this.canceledQuantity = find(MouvementProduit.CANCEL_SALE);
    this.mouvementStockIn = find(MouvementProduit.MOUVEMENT_STOCK_IN);
    this.mouvementStockOut = find(MouvementProduit.MOUVEMENT_STOCK_OUT);
    this.retourDepot = find(MouvementProduit.RETOUR_DEPOT);
    this.storeInventoryQuantity = find(MouvementProduit.INVENTAIRE);
  }
}
