import { ChangeDetectionStrategy, Component, inject, OnInit, signal, computed, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, CardComponent, RadioComponent } from 'app/shared/ui';
import { SpinnerComponent } from 'app/shared/spinner/spinner.component';
import { IProduit } from 'app/shared/model/produit.model';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { ProductsApiService } from '../../data-access/services/products-api.service';
import { ILotConflict, IStockConflict, LotConflictAction } from '../../models/produit-merge.model';
import { IProduitIndicateurs } from '../../models/produit-indicateurs.model';

interface ComparisonRow {
  produitId: number;
  libelle: string;
  codeCip?: string;
  stockActuel?: number;
  prix?: number;
  derniereVente?: string;
  derniereEntreeStock?: string;
  derniereModification?: string;
  qteVendue12Mois?: number;
}

const ENTITY_LABELS: Record<string, string> = {
  stockProduit: 'Stocks',
  lot: 'Lots',
  optionPrixProduit: 'Options de prix',
  fournisseurProduit: 'Fournisseurs',
  rayonProduit: 'Rayons',
  salesLine: 'Lignes de vente',
  produitDetailReparente: 'Produit détail (déconditionné) re-parenté vers la cible',
  produitDetailFusionne: 'Produit détail (déconditionné) fusionné (ventes) et archivé',
  salesLineDetail: 'Lignes de vente du déconditionné fusionnées',
  substitut: 'Génériques/substituts',
  semoisConfiguration: 'Configurations de semis',
  ventesMensuellesAgregees: 'Ventes mensuelles agrégées',
  storeInventoryLine: "Lignes d'inventaire",
  AvoirClient: 'Avoirs client',
  ClassificationCriticiteLog: 'Historique de criticité',
  Decondition: 'Déconditionnements',
  InventoryTransaction: "Transactions d'inventaire",
  ProduitPerime: 'Produits périmés',
  RetourClientLine: 'Lignes de retour client',
  RetourDepotItem: 'Lignes de retour dépôt',
  Rupture: 'Ruptures',
};

@Component({
  selector: 'app-produit-merge-modal',
  templateUrl: './produit-merge-modal.component.html',
  styleUrls: ['./produit-merge-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, ButtonComponent, CardComponent, RadioComponent, SpinnerComponent],
})
export class ProduitMergeModalComponent implements OnInit {
  produits!: IProduit[];

  activeModal = inject(NgbActiveModal);

  private readonly api = inject(ProductsApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  protected targetId = signal<number | null>(null);
  protected loadingPreview = signal(false);
  protected isConfirming = signal(false);
  protected previewError = signal<string | null>(null);
  protected entityCounts = signal<Record<string, number>>({});
  protected rejectedSourceIds = signal<number[]>([]);
  protected rejectionReasons = signal<Record<string, string>>({});
  protected lotConflicts = signal<ILotConflict[]>([]);
  protected lotResolutions = signal<Map<number, LotConflictAction>>(new Map());
  protected stockConflicts = signal<IStockConflict[]>([]);
  protected loadingComparison = signal(false);
  protected comparisonRows = signal<ComparisonRow[]>([]);

  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  protected sourceIds = computed(() => this.produits.filter(p => p.id !== this.targetId()).map(p => p.id!));

  protected entityCountEntries = computed(() =>
    Object.entries(this.entityCounts())
      .filter(([, count]) => count > 0)
      .map(([key, count]) => ({ label: ENTITY_LABELS[key] ?? key, count })),
  );

  protected allConflictsResolved = computed(() => {
    const resolutions = this.lotResolutions();
    return this.lotConflicts().every(c => resolutions.has(c.sourceLotId!));
  });

  protected canConfirm = computed(
    () =>
      this.targetId() !== null &&
      this.sourceIds().length > 0 &&
      !this.loadingPreview() &&
      !this.previewError() &&
      this.allConflictsResolved(),
  );

  ngOnInit(): void {
    // Aucune présélection : l'utilisateur doit choisir explicitement le produit cible.
    this.loadComparisonData();
  }

  /** Charge les indicateurs de chaque produit candidat pour orienter le choix de la cible. */
  private loadComparisonData(): void {
    this.loadingComparison.set(true);
    this.spinner().show();
    const requests = this.produits.map(p =>
      this.api.getIndicateurs(p.id!).pipe(catchError(() => of(null))),
    );

    forkJoin(requests)
      .pipe(
        finalize(() => {
          this.loadingComparison.set(false);
          this.spinner().hide();
        }),
      )
      .subscribe(results => {
        this.comparisonRows.set(
          this.produits.map((p, i) => {
            const indicateurs: IProduitIndicateurs | null = results[i];
            return {
              produitId: p.id!,
              libelle: p.libelle ?? `#${p.id}`,
              codeCip: p.codeCip,
              stockActuel: p.totalQuantity,
              prix: p.regularUnitPrice,
              derniereVente: p.lastDateOfSale,
              derniereEntreeStock: p.lastOrderDate,
              derniereModification: p.updatedAt,
              qteVendue12Mois: indicateurs?.qteVendue12Mois,
            };
          }),
        );
      });
  }

  protected onTargetChange(): void {
    this.runPreview();
  }

  protected resolutionFor(conflict: ILotConflict): LotConflictAction | undefined {
    return this.lotResolutions().get(conflict.sourceLotId!);
  }

  protected setResolution(conflict: ILotConflict, action: LotConflictAction): void {
    this.lotResolutions.update(map => {
      const next = new Map(map);
      next.set(conflict.sourceLotId!, action);
      return next;
    });
  }

  protected runPreview(): void {
    const targetId = this.targetId();
    const sourceIds = this.sourceIds();
    if (targetId === null || sourceIds.length === 0) {
      return;
    }

    this.loadingPreview.set(true);
    this.previewError.set(null);
    this.lotResolutions.set(new Map());

    this.api.previewMerge(targetId, sourceIds).subscribe({
      next: preview => {
        this.entityCounts.set(preview.entityCounts ?? {});
        this.rejectedSourceIds.set(preview.rejectedSourceIds ?? []);
        this.rejectionReasons.set(preview.rejectionReasons ?? {});
        this.lotConflicts.set(preview.lotConflicts ?? []);
        this.stockConflicts.set(preview.stockConflicts ?? []);
        this.loadingPreview.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.previewError.set(this.errorService.getErrorMessage(err));
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
        this.loadingPreview.set(false);
      },
    });
  }

  protected produitLibelle(id: number): string {
    return this.produits.find(p => p.id === id)?.libelle ?? `#${id}`;
  }

  protected confirm(): void {
    const targetId = this.targetId();
    if (targetId === null || !this.canConfirm()) {
      return;
    }

    const lotResolutions = Array.from(this.lotResolutions(), ([lotId, action]) => ({ lotId, action }));

    this.isConfirming.set(true);
    this.api
      .confirmMerge({ targetId, sourceIds: this.sourceIds(), lotResolutions })
      .subscribe({
        next: result => {
          this.isConfirming.set(false);
          this.activeModal.close(result);
        },
        error: (err: HttpErrorResponse) => {
          this.isConfirming.set(false);
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
        },
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
