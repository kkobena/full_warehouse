import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { IProduit } from 'app/shared/model/produit.model';
import { EtaProduitComponent } from 'app/shared/eta-produit/eta-produit.component';
import { ProductsApiService, ILotPeremption } from '../../data-access/services/products-api.service';
import { IProduitIndicateurs } from '../../models/produit-indicateurs.model';
import { IVenteMois } from '../../models/vente-mois.model';
import { ProduitSyntheseTabComponent } from '../produit-synthese-tab/produit-synthese-tab.component';
import { ProduitStockTabComponent } from '../produit-stock-tab/produit-stock-tab.component';
import { ProduitFournisseursTabComponent } from '../produit-fournisseurs-tab/produit-fournisseurs-tab.component';
import { ProduitHistoriqueTabComponent } from '../produit-historique-tab/produit-historique-tab.component';
import { ProduitMouvementsTabComponent } from '../produit-mouvements-tab/produit-mouvements-tab.component';

@Component({
  selector: 'app-produit-detail-panel',
  templateUrl: './produit-detail-panel.component.html',
  styleUrl: './produit-detail-panel.component.scss',
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    TooltipModule,
    NgbNavModule,
    EtaProduitComponent,
    ProduitSyntheseTabComponent,
    ProduitStockTabComponent,
    ProduitFournisseursTabComponent,
    ProduitHistoriqueTabComponent,
    ProduitMouvementsTabComponent,
  ],
})
export class ProduitDetailPanelComponent {
  readonly produit = input.required<IProduit>();

  readonly closePanel = output<void>();
  readonly editRequested = output<IProduit>();

  /** Données complètes (avec stockProduits + fournisseurProduits) chargées au clic */
  protected fullProduit = signal<IProduit | null>(null);
  protected loadingFull = signal(false);

  protected indicateurs = signal<IProduitIndicateurs | null>(null);
  protected lots = signal<ILotPeremption[]>([]);
  protected ventes = signal<IVenteMois[]>([]);
  protected loadingIndicateurs = signal(false);
  protected loadingVentes = signal(false);
  protected activeTab = signal<string>('synthese');

  /** Produit enrichi : données complètes si chargées, sinon données liste */
  protected richProduit = computed(() => this.fullProduit() ?? this.produit());

  private readonly api = inject(ProductsApiService);

  constructor() {
    effect(() => {
      const p = this.produit();
      if (p?.id) {
        // Reset à chaque changement de produit
        this.fullProduit.set(null);
        this.indicateurs.set(null);
        this.lots.set([]);
        this.ventes.set([]);
        this.activeTab.set('synthese');
        this.loadFull(p.id);
        this.loadIndicateurs(p.id);
        this.loadLots(p.id);
      }
    });
  }

  protected onTabChange(tab: string | number): void {
    const tabId = String(tab);
    this.activeTab.set(tabId);
    if (tabId === 'historique' && this.ventes().length === 0) {
      this.loadVentes(this.produit().id!);
    }
  }

  protected onEdit(): void {
    this.editRequested.emit(this.produit());
  }

  protected onRefreshRequested(): void {
    const id = this.produit().id;
    if (id) this.loadFull(id);
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

  private loadVentes(id: number): void {
    this.loadingVentes.set(true);
    this.api.getVentesMensuelles(id, 12).subscribe({
      next: data => {
        this.ventes.set(data);
        this.loadingVentes.set(false);
      },
      error: () => this.loadingVentes.set(false),
    });
  }
}
