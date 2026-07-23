import { Component, effect, ElementRef, inject, input, OnDestroy, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { ProduitStatService } from 'app/entities/produit/stat/produit-stat.service';
import { MagasinService } from 'app/entities/magasin/magasin.service';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { ProduitAuditingParam, ProduitAuditingState, ProduitAuditingSum } from 'app/shared/model/produit-record.model';
import { MouvementProduit } from 'app/shared/model/enumerations/mouvement-produit.model';
import { IStorage } from 'app/shared/model/magasin.model';
import { IProduit } from 'app/shared/model/produit.model';
import { ButtonComponent, DataTableComponent, MultiSelectComponent, PillSelectorComponent, SelectComponent } from 'app/shared/ui';
import { BlobDownloadService } from '../../../../shared/services/blob-download.service';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { createPeriodDateFilter } from '../period-date-filter';

Chart.register(...registerables);

@Component({
  selector: 'app-produit-mouvements-tab',
  templateUrl: './produit-mouvements-tab.component.html',
  styleUrls: ['./produit-mouvements-tab.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    DataTableComponent,
    ButtonComponent,
    SelectComponent,
    NgbTooltip,
    PharmaDatePickerComponent,
    PillSelectorComponent,
    TranslatePipe,
    MultiSelectComponent
  ]
})
export class ProduitMouvementsTabComponent implements OnDestroy {
  readonly produitId = input.required<number>();
  /** Produit courant transmis par le parent — utilisé pour déterminer le nombre d'emplacements */
  readonly produit = input<IProduit>();

  protected entites = signal<ProduitAuditingState[]>([]);
  protected loading = signal(false);
  protected exporting = signal(false);
  protected hasDepot = signal(false);
  protected storages = signal<IStorage[]>([]);
  protected showChart = signal(false);

  private readonly canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('stockChart');
  private chart?: Chart;

  protected readonly periodFilter = createPeriodDateFilter({ defaultKey: 'today', onChange: () => this.load() });

  protected selectedStorage: IStorage | null = null;

  /** Filtre multi-types de mouvement (null / [] = tous) — lié via ngModel */
  protected selectedTypes: string[] | null = [];

  // Totaux footer
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

  // ── Options de filtre type (depuis enum MouvementProduit Java) ─
  protected readonly MOVEMENT_TYPE_OPTIONS: { label: string; value: string }[] = [
    { label: 'Vente', value: 'SALE' },
    { label: 'Annulation vente', value: 'CANCEL_SALE' },
    { label: 'Retour fournisseur', value: 'RETOUR_FOURNISSEUR' },
    { label: 'Entrée stock', value: 'ENTREE_STOCK' },
    { label: 'Produit périmé', value: 'RETRAIT_PERIME' },
    { label: 'Ajustement +', value: 'AJUSTEMENT_IN' },
    { label: 'Ajustement −', value: 'AJUSTEMENT_OUT' },
    { label: 'Déconditionnement +', value: 'DECONDTION_IN' },
    { label: 'Déconditionnement −', value: 'DECONDTION_OUT' },
    { label: 'Déplacement entrant', value: 'MOUVEMENT_STOCK_IN' },
    { label: 'Déplacement sortant', value: 'MOUVEMENT_STOCK_OUT' },
    { label: 'Retour dépôt', value: 'RETOUR_DEPOT' },
    { label: 'Inventaire', value: 'INVENTAIRE' },
  ];

  // ── Mapping field → type pour la visibilité colonnes ──────────
  private readonly FIELD_TYPE_MAP: Record<string, string> = {
    saleQuantity: 'SALE',
    retourFournisseurQuantity: 'RETOUR_FOURNISSEUR',
    perimeQuantity: 'RETRAIT_PERIME',
    ajustementNegatifQuantity: 'AJUSTEMENT_OUT',
    deconNegatifQuantity: 'DECONDTION_OUT',
    mouvementStockOut: 'MOUVEMENT_STOCK_OUT',
    deleveryQuantity: 'ENTREE_STOCK',
    ajustementPositifQuantity: 'AJUSTEMENT_IN',
    deconPositifQuantity: 'DECONDTION_IN',
    canceledQuantity: 'CANCEL_SALE',
    mouvementStockIn: 'MOUVEMENT_STOCK_IN',
    retourDepot: 'RETOUR_DEPOT',
    storeInventoryQuantity: 'INVENTAIRE',
    inventoryGap: 'INVENTAIRE',
  };

  private readonly OUT_FIELDS = [
    'saleQuantity', 'retourFournisseurQuantity', 'perimeQuantity',
    'ajustementNegatifQuantity', 'deconNegatifQuantity', 'mouvementStockOut',
  ];
  private readonly IN_FIELDS = [
    'deleveryQuantity', 'ajustementPositifQuantity', 'deconPositifQuantity',
    'canceledQuantity', 'mouvementStockIn',
  ];

  private readonly statService = inject(ProduitStatService);
  private readonly magasinService = inject(MagasinService);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  constructor() {
    this.magasinService.hasDepot().subscribe(res => this.hasDepot.set(res.body ?? false));
    effect(() => {
      if (this.produitId()) {
        this.load();
      }
    });
    // Rebuild chart when visibility or data changes
    effect(() => {
      const visible = this.showChart();
      const rows = this.entites();
      if (visible && rows.length > 0) {
        // Defer to let Angular render the canvas first
        setTimeout(() => this.buildChart(rows), 0);
      } else if (!visible) {
        this.chart?.destroy();
        this.chart = undefined;
      }
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  // ── Emplacements du produit (dérivés de stockProduits) ────────

  /** Emplacements de stockage de CE produit, mappés depuis stockProduits */
  protected get productStorages(): IStorage[] {
    return (this.produit()?.stockProduits ?? [])
      .filter(sp => sp.storageId != null && sp.storageName)
      .map(sp => ({ id: sp.storageId, name: sp.storageName }) as IStorage);
  }

  /** Afficher le sélecteur d'emplacement seulement si le produit a 2+ stocks */
  protected get hasMultipleStorages(): boolean {
    return this.productStorages.length > 1;
  }

  // ── Visibilité des colonnes ───────────────────────────────────

  /** Normalise null → [] (p-multiselect retourne null lors du clear) */
  private get activeTypes(): string[] {
    return this.selectedTypes ?? [];
  }

  /** Retourne true si la colonne doit être affichée ([] = tous visibles) */
  protected isFieldVisible(field: string): boolean {
    if (!this.activeTypes.length) return true;
    return this.activeTypes.includes(this.FIELD_TYPE_MAP[field]);
  }

  /** Nb de colonnes du groupe "Sorties" visibles */
  protected get outColspan(): number {
    if (!this.activeTypes.length) return 6;
    return this.OUT_FIELDS.filter(f => this.activeTypes.includes(this.FIELD_TYPE_MAP[f])).length;
  }

  /** Nb de colonnes du groupe "Entrées" visibles (dépôt inclus si applicable) */
  protected get inColspan(): number {
    const base = !this.activeTypes.length
      ? this.IN_FIELDS.length
      : this.IN_FIELDS.filter(f => this.activeTypes.includes(this.FIELD_TYPE_MAP[f])).length;
    const includeDepot = this.hasDepot() && (!this.activeTypes.length || this.activeTypes.includes('RETOUR_DEPOT'));
    return base + (includeDepot ? 1 : 0);
  }

  /** Colspan total pour emptymessage */
  protected get totalColspan(): number {
    return 2 + this.outColspan + this.inColspan + 3;
  }

  // ── P1 : helpers affichage tableau ───────────────────────────

  /**
   * Formate une quantité de mouvement :
   * - 0 / null / undefined → '—' (aucun mouvement ce jour)
   * - non-zéro → nombre formaté locale française
   */
  protected qty(val: number | undefined | null): string {
    if (!val) return '—';
    return val.toLocaleString('fr-FR');
  }

  /**
   * Classe CSS de la ligne selon la variation nette du stock.
   * Priorité : inventaire > gain > perte.
   */
  protected rowClass(row: ProduitAuditingState): string {
    if (row.storeInventoryQuantity) return 'mvt-row-inventory';
    if ((row.afterStock ?? 0) > (row.initStock ?? 0)) return 'mvt-row-positive';
    if ((row.afterStock ?? 0) < (row.initStock ?? 0)) return 'mvt-row-negative';
    return '';
  }

  /** Appelé par (selectionChange) de app-multi-select — couvre aussi le clic sur la croix de remise à zéro. */
  protected onTypesChange(value: string[] | null): void {
    this.selectedTypes = value;
    this.load();
  }

  // ── Données ──────────────────────────────────────────────────

  protected load(): void {
    this.loading.set(true);
    const param = this.buildParam();
    this.statService.fetchTransactions(param).subscribe({
      next: res => {
        this.entites.set(res.body ?? []);
        this.loading.set(false);
        if (this.showChart() && (res.body ?? []).length > 0) {
          setTimeout(() => this.buildChart(res.body!), 0);
        }
      },
      error: () => this.loading.set(false),
    });
    this.statService.fetchTransactionsSum(param).subscribe({
      next: res => this.computeTotaux(res.body ?? []),
    });
  }

  protected exportPdf(): void {
    this.statService.exportToPdf(this.buildParam()).subscribe({
      next: blob => this.downloadDocumentService.downloadPdf(blob, 'mouvement-produit'),
    });
  }

  protected exportExcel(): void {
    this.exporting.set(true);
    this.statService.exportToExcel(this.buildParam()).subscribe({
      next: blob => {
        this.downloadDocumentService.downloadExcel(blob, 'mouvement-produit');
        this.exporting.set(false);
      },
      error: () => this.exporting.set(false),
    });
  }

  protected toggleChart(): void {
    this.showChart.update(v => !v);
  }

  private buildChart(rows: ProduitAuditingState[]): void {
    const canvas = this.canvasRef()?.nativeElement;
    if (!canvas) return;
    this.chart?.destroy();

    const fmtDate = (d: any): string => {
      if (!d) return '';
      return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
    };

    const labels = rows.map(r => fmtDate(r.mvtDate));
    const stockData = rows.map(r => r.afterStock ?? 0);
    const inData = rows.map(r =>
      (r.deleveryQuantity ?? 0) + (r.ajustementPositifQuantity ?? 0) +
      (r.deconPositifQuantity ?? 0) + (r.canceledQuantity ?? 0) +
      (r.mouvementStockIn ?? 0) + (r.retourDepot ?? 0),
    );
    const outData = rows.map(r =>
      (r.saleQuantity ?? 0) + (r.retourFournisseurQuantity ?? 0) +
      (r.perimeQuantity ?? 0) + (r.ajustementNegatifQuantity ?? 0) +
      (r.deconNegatifQuantity ?? 0) + (r.mouvementStockOut ?? 0),
    );

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            type: 'line' as any,
            label: 'Stock final',
            data: stockData,
            borderColor: 'rgba(59, 130, 246, 1)',
            backgroundColor: 'rgba(59, 130, 246, 0.08)',
            borderWidth: 2,
            fill: true,
            tension: 0.3,
            yAxisID: 'yStock',
            pointRadius: rows.length > 60 ? 0 : 3,
            order: 0,
          },
          {
            label: 'Entrées',
            data: inData,
            backgroundColor: 'rgba(34, 197, 94, 0.7)',
            borderColor: 'rgba(34, 197, 94, 1)',
            borderWidth: 1,
            yAxisID: 'yMvt',
            stack: 'mvt',
            order: 1,
          },
          {
            label: 'Sorties',
            data: outData,
            backgroundColor: 'rgba(239, 68, 68, 0.7)',
            borderColor: 'rgba(239, 68, 68, 1)',
            borderWidth: 1,
            yAxisID: 'yMvt',
            stack: 'mvt',
            order: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { position: 'top', labels: { boxWidth: 12, font: { size: 11 } } },
          tooltip: { mode: 'index', intersect: false },
        },
        scales: {
          x: {
            ticks: { font: { size: 10 }, maxRotation: 45, autoSkip: true, maxTicksLimit: 20 },
          },
          yStock: {
            type: 'linear',
            position: 'left',
            title: { display: true, text: 'Stock', font: { size: 10 } },
            ticks: { precision: 0, font: { size: 10 } },
            beginAtZero: false,
          },
          yMvt: {
            type: 'linear',
            position: 'right',
            title: { display: true, text: 'Mouvements', font: { size: 10 } },
            ticks: { precision: 0, font: { size: 10 } },
            beginAtZero: true,
            grid: { drawOnChartArea: false },
          },
        },
      },
    };

    this.chart = new Chart(canvas, config);
  }

  private buildParam(): ProduitAuditingParam {
    return {
      produitId: this.produitId(),
      ...this.periodFilter.dateParams(),
      storageId: this.selectedStorage?.id,
      mouvementTypes: this.activeTypes.length ? this.activeTypes : undefined,
    };
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
