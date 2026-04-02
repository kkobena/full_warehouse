import { Component, computed, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IProduit } from 'app/shared/model/produit.model';
import { IStockProduit } from 'app/shared/model/stock-produit.model';
import { IProduitIndicateurs } from 'app/features/products/models/produit-indicateurs.model';
import { ILotPeremption } from 'app/features/products/data-access/services/products-api.service';
import { FormStockProduitComponent } from 'app/entities/produit/form-stock-produit/form-stock-produit.component';
import { FormTransfertStockComponent } from 'app/entities/produit/form-transfert-stock/form-transfert-stock.component';

@Component({
  selector: 'app-produit-stock-tab',
  templateUrl: './produit-stock-tab.component.html',
  styleUrls: ['./produit-stock-tab.component.scss'],
  imports: [CommonModule, ButtonModule, TooltipModule],
})
export class ProduitStockTabComponent {
  readonly produit = input.required<IProduit>();
  readonly indicateurs = input<IProduitIndicateurs | null>(null);
  readonly lots = input<ILotPeremption[]>([]);
  readonly refreshRequested = output<void>();

  private readonly modalService = inject(NgbModal);

  protected stockPrincipal = computed<IStockProduit | undefined>(
    () => this.produit().stockProduits?.find(sp => sp.type === 'PRINCIPAL'),
  );

  protected stockReserve = computed<IStockProduit | undefined>(
    () => this.produit().stockProduits?.find(sp => sp.type === 'SAFETY_STOCK'),
  );

  protected canAddReserve = computed<boolean>(
    () => !!this.stockPrincipal() && !this.stockReserve(),
  );

  protected canTransfertToReserve = computed<boolean>(() => {
    const rayon = this.stockPrincipal();
    if (!rayon) return false;
    return (rayon.qtyStock ?? 0) > (rayon.seuilMini ?? 0);
  });

  protected canTransfertToRayon = computed<boolean>(() => {
    const reserve = this.stockReserve();
    return !!reserve && (reserve.qtyStock ?? 0) > 0;
  });


  /** Délai livraison du fournisseur principal (jours), fallback 7 j */
  protected delaiLivraison = computed<number>(
    () => this.produit().fournisseurProduit?.delaiLivraisonJours ?? 7,
  );

  /** CMM en unités/mois depuis indicateurs */
  protected cmm = computed<number>(() => this.indicateurs()?.cmm ?? 0);

  /** Consommation journalière = CMM / 30 */
  protected cjour = computed<number>(() => this.cmm() / 30);

  /**
   * Seuil mini calculé = cjour × délai
   * Stock minimal pour ne pas être en rupture pendant le réapprovisionnement
   */
  protected seuilCalcule = computed<number | null>(() => {
    const cj = this.cjour();
    if (cj <= 0) return null;
    return Math.ceil(cj * this.delaiLivraison());
  });

  /**
   * Couverture stock actuel (rayon) en jours selon CMM
   * Repris depuis indicateurs pour cohérence avec la synthèse
   */
  protected couvertureActuelle = computed<number | null>(() => {
    const v = this.indicateurs()?.couvertureStockJours;
    if (v == null || v < 0) return null;
    return Math.min(v, 9999);
  });

  /**
   * Seuil actuel du rayon vs seuil calculé
   * true = seuil paramétré insuffisant
   */
  protected seuilInsuffisant = computed<boolean>(() => {
    const sc = this.seuilCalcule();
    const rayon = this.stockPrincipal();
    if (sc == null || !rayon) return false;
    return (rayon.seuilMini ?? 0) < sc;
  });


  /** Lots triés FEFO : du plus proche au plus lointain */
  protected lotsFEFO = computed<ILotPeremption[]>(() =>
    [...this.lots()].sort((a, b) => {
      if (!a.datePeremption) return 1;
      if (!b.datePeremption) return -1;
      return a.datePeremption.localeCompare(b.datePeremption);
    }),
  );

  /** Lot expirant le plus tôt */
  protected lotUrgent = computed<ILotPeremption | null>(() => {
    const lot = this.lotsFEFO()[0];
    if (!lot) return null;
    const days = lot.peremptionStatut?.days ?? 9999;
    return days <= 90 ? lot : null;   // alerte < 3 mois
  });

  /** Badge couleur FEFO selon les jours restants */
  protected fefoClass(lot: ILotPeremption): string {
    const days = lot.peremptionStatut?.days ?? 9999;
    if (days <= 30) return 'fefo-critical';
    if (days <= 90) return 'fefo-warning';
    return 'fefo-ok';
  }

  /** 0-100 niveau de remplissage */
  protected niveauPct(sp: IStockProduit | undefined): number {
    if (!sp) return 0;
    const qty = Math.max(0, sp.qtyStock ?? 0);
    const max = sp.stockMaxi ?? sp.seuilMini ?? 0;
    if (max <= 0) return qty > 0 ? 100 : 0;
    return Math.min(100, Math.round((qty / max) * 100));
  }

  /** Classe couleur selon rapport qty / seuilMini */
  protected stockClass(sp: IStockProduit | undefined): string {
    if (!sp) return '';
    const qty = sp.qtyStock ?? 0;
    const seuil = sp.seuilMini ?? 0;
    if (qty <= 0) return 'niveau-empty';
    if (seuil > 0 && qty < seuil) return 'niveau-danger';
    if (seuil > 0 && qty < seuil * 1.5) return 'niveau-warning';
    return 'niveau-ok';
  }

  protected onEditStock(sp: IStockProduit): void {
    const modalRef = this.modalService.open(FormStockProduitComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.produit = this.produit();
    modalRef.componentInstance.stockProduit = sp;
    modalRef.result.then(
      () => this.refreshRequested.emit(),
      () => {},
    );
  }

  protected onAddReserve(): void {
    const modalRef = this.modalService.open(FormStockProduitComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.produit = this.produit();
    modalRef.result.then(
      () => this.refreshRequested.emit(),
      () => {},
    );
  }

  protected onTransfert(src: IStockProduit): void {
    const modalRef = this.modalService.open(FormTransfertStockComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.produit = this.produit();
    modalRef.componentInstance.stockProduitSrc = src;
    modalRef.result.then(
      () => this.refreshRequested.emit(),
      () => {},
    );
  }
}
