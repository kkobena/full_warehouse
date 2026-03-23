import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { IProduit } from 'app/shared/model/produit.model';
import { IProduitIndicateurs } from '../../models/produit-indicateurs.model';
import { ILotPeremption } from '../../data-access/services/products-api.service';

@Component({
  selector: 'app-produit-synthese-tab',
  templateUrl: './produit-synthese-tab.component.html',
  styleUrls: ['./produit-synthese-tab.scss'],
  imports: [CommonModule, TooltipModule],
})
export class ProduitSyntheseTabComponent {
  readonly produit = input.required<IProduit>();
  readonly indicateurs = input<IProduitIndicateurs | null>(null);
  readonly lots = input<ILotPeremption[]>([]);
  readonly loadingIndicateurs = input<boolean>(false);

  protected readonly margeAbsolue = computed(() => {
    const p = this.produit();
    if (!p.regularUnitPrice || !p.costAmount) return null;
    return p.regularUnitPrice - p.costAmount;
  });

  protected readonly tauxMarge = computed(() => {
    const ind = this.indicateurs();
    if (ind?.tauxMarge != null) return ind.tauxMarge;
    const p = this.produit();
    if (!p.regularUnitPrice || p.regularUnitPrice === 0) return null;
    return Math.round(((p.regularUnitPrice - (p.costAmount ?? 0)) / p.regularUnitPrice) * 100);
  });

  /** couvertureStockJours sanitisé : null si négatif ou invalide */
  protected readonly couvertureJours = computed(() => {
    const v = this.indicateurs()?.couvertureStockJours;
    if (v == null || v < 0) return null;
    return v;
  });

  /** rotationAnnuelleQte sanitisée : null si négative */
  protected readonly rotationAnnuelle = computed(() => {
    const v = this.indicateurs()?.rotationAnnuelleQte;
    if (v == null || v < 0) return null;
    return v;
  });

  /** Couleur du stock actuel : danger si rupture, warning si sous seuil, success sinon */
  protected readonly stockClass = computed(() => {
    const qty = this.produit().totalQuantity ?? 0;
    const seuil = this.produit().seuilMini ?? 0;
    if (qty <= 0) return 'kpi-danger';
    if (seuil > 0 && qty < seuil) return 'kpi-warning';
    return 'kpi-success';
  });

  protected readonly joursStockClass = computed(() => {
    const jours = this.couvertureJours();
    if (jours == null) return '';
    if (jours < 7) return 'kpi-danger';
    if (jours < 30) return 'kpi-warning';
    return 'kpi-success';
  });

  /** Lot avec la date de péremption la plus proche */
  protected readonly lotLeProchePrime = computed(() => {
    const lots = this.lots();
    if (!lots.length) return null;
    return lots.reduce((nearest, lot) => {
      if (!nearest?.peremptionStatut) return lot;
      if (!lot.peremptionStatut) return nearest;
      return lot.peremptionStatut.days < nearest.peremptionStatut.days ? lot : nearest;
    });
  });

  protected readonly alertePeremption = computed((): 'critical' | 'warning' | null => {
    const lot = this.lotLeProchePrime();
    if (!lot?.peremptionStatut) return null;
    const mois = lot.peremptionStatut.mouths;
    if (mois < 3) return 'critical';
    if (mois < 6) return 'warning';
    return null;
  });

  protected formatPrix(montant?: number | null): string {
    if (montant == null) return '—';
    return montant.toLocaleString('fr-FR', { minimumFractionDigits: 0 }) + ' FCFA';
  }

  protected formatDate(date?: any): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-FR');
  }
}
