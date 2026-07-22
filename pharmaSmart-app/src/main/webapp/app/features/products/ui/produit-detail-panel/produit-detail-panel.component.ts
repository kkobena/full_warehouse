import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {NgbNavModule, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent} from 'app/shared/ui';
import {IProduit} from 'app/shared/model/produit.model';
import {EtaProduitComponent} from 'app/shared/eta-produit/eta-produit.component';
import {ILotProduit, ProductsApiService} from "../../data-access/services/products-api.service";
import {IProduitIndicateurs} from '../../models/produit-indicateurs.model';
import {ProduitSyntheseTabComponent} from '../produit-synthese-tab/produit-synthese-tab.component';
import {ProduitStockTabComponent} from '../produit-stock-tab/produit-stock-tab.component';
import {
  ProduitFournisseursTabComponent
} from '../produit-fournisseurs-tab/produit-fournisseurs-tab.component';
import {
  ProduitMouvementsTabComponent
} from '../produit-mouvements-tab/produit-mouvements-tab.component';
import {
  ProduitDeconditionsTabComponent
} from '../produit-deconditions-tab/produit-deconditions-tab.component';
import {ProduitVentesTabComponent} from '../produit-ventes-tab/produit-ventes-tab.component';
import {ProduitAchatsTabComponent} from '../produit-achats-tab/produit-achats-tab.component';
import {ProduitRayonsTabComponent} from '../produit-rayons-tab/produit-rayons-tab.component';

@Component({
  selector: 'app-produit-detail-panel',
  templateUrl: './produit-detail-panel.component.html',
  styleUrl: './produit-detail-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    RouterModule,
    ButtonComponent,
    NgbTooltip,
    NgbNavModule,
    EtaProduitComponent,
    ProduitSyntheseTabComponent,
    ProduitStockTabComponent,
    ProduitFournisseursTabComponent,
    ProduitMouvementsTabComponent,
    ProduitDeconditionsTabComponent,
    ProduitVentesTabComponent,
    ProduitAchatsTabComponent,
    ProduitRayonsTabComponent,
  ],
})
export class ProduitDetailPanelComponent {
  readonly produit = input.required<IProduit>();
  readonly canEdit = input<boolean>(true);

  readonly closePanel = output<void>();
  readonly editRequested = output<IProduit>();

  /** Données complètes (avec stockProduits + fournisseurProduits) chargées au clic */
  protected fullProduit = signal<IProduit | null>(null);
  protected loadingFull = signal(false);

  protected indicateurs = signal<IProduitIndicateurs | null>(null);
  protected lots = signal<ILotProduit[]>([]);
  protected loadingIndicateurs = signal(false);
  protected activeTab = signal<string>('synthese');

  /** Produit enrichi : données complètes si chargées, sinon données liste */
  protected richProduit = computed(() => this.fullProduit() ?? this.produit());

  private readonly api = inject(ProductsApiService);

  /** Track produit ID to avoid resetting the active tab on same-produit refresh */
  private currentProduitId: number | null = null;

  constructor() {
    effect(() => {
      const p = this.produit();
      if (!p?.id) {
        return;
      }

      const isNewProduit = p.id !== this.currentProduitId;
      this.currentProduitId = p.id;

      // Reset data
      this.fullProduit.set(null);
      this.indicateurs.set(null);
      this.lots.set([]);

      // Only reset tab when switching to a different produit
      if (isNewProduit) {
        this.activeTab.set('synthese');
      }

      this.loadFull(p.id);
      this.loadIndicateurs(p.id);
      this.loadLots(p.id);
    });
  }

  protected onTabChange(tab: string | number): void {
    const tabId = String(tab);
    this.activeTab.set(tabId);
  }

  protected onEdit(): void {
    this.editRequested.emit(this.produit());
  }

  protected onRefreshRequested(): void {
    const id = this.produit().id;
    if (!id) {
      return;
    }
    this.loadFull(id);
    this.loadLots(id);
  }

  protected classeLabel(classe?: string): string {
    return classe?.replace('_', '+') ?? '—';
  }

  private loadFull(id: number): void {
    this.loadingFull.set(true);
    this.api.getById(id).subscribe({
      next: p => {
        this.fullProduit.set(p);
        this.loadingFull.set(false);
      },
      error: () => this.loadingFull.set(false),
    });
  }

  private loadIndicateurs(id: number): void {
    this.loadingIndicateurs.set(true);
    this.api.getIndicateurs(id).subscribe({
      next: ind => {
        this.indicateurs.set(ind);
        this.loadingIndicateurs.set(false);
      },
      error: () => this.loadingIndicateurs.set(false),
    });
  }

  private loadLots(id: number): void {
    this.api.getLots(id).subscribe({
      next: lots => this.lots.set(lots),
    });
  }
}
