import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';

import { IReapproDashboard, IClasseBreakdown, ITopUrgentDTO } from 'app/shared/model/semois/semois-dashboard.model';
import { ClasseCriticite, getClasseCriticiteInfo } from 'app/shared/model/semois/classe-criticite.model';
import { SemoisService } from 'app/entities/semois/semois.service';
import { CommandCommonService } from 'app/entities/commande/command-common.service';
import {
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  KpiItemComponent,
  KpiStripComponent,
  SkeletonComponent,
  ToolbarComponent,
} from 'app/shared/ui';

@Component({
  selector: 'app-semois-dashboard',
  templateUrl: './semois-dashboard.component.html',
  styleUrls: ['./semois-dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
    NgClass,
    BadgeComponent,
    ButtonComponent,
    DataTableComponent,
    SkeletonComponent,
    ToolbarComponent,
    KpiStripComponent,
    KpiItemComponent,
  ],
})
export class SemoisDashboardComponent implements OnInit {
  readonly dashboard = signal<IReapproDashboard | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly lastRefresh = signal<Date | null>(null);

  private readonly semoisService = inject(SemoisService);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.isLoading.set(true);
    this.semoisService.getDashboard().subscribe({
      next: (res: HttpResponse<IReapproDashboard>) => {
        this.dashboard.set(res.body);
        this.lastRefresh.set(new Date());
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Bascule vers l'onglet « Commandes & Réceptions » de commande-home.
   *
   * <p>L'identifiant est celui d'un `ngbNavItem` de `commande-home` — les cinq valeurs
   * possibles sont les clés de son `TAB_LABELS`. `'SEMOIS_SUGGESTIONS'`, écrit ici avant que
   * les onglets ne soient renommés, ne correspondait plus à aucun d'eux : il ne changeait donc
   * rien, et laissait surtout le signal partagé sur une valeur qu'aucun onglet ne reconnaît,
   * de sorte que l'ouverture suivante de commande-home n'affichait aucun contenu.
   *
   * <p>L'écran vit sur une autre route (`/semois/dashboard`) : le signal seul ne mène nulle
   * part, il faut aussi router vers `/commande`. L'onglet est lu au `ngOnInit` de
   * `commande-home`, qui retombe sur ce signal en l'absence de paramètre `tab`.
   */
  navigateToSuggestions(_classe?: ClasseCriticite): void {
    this.commandCommonService.updateCommandPreviousActiveNav('SUGGESTIONS');
    this.router.navigate(['/commande']);
  }

  // ─── Getters calculés ───────────────────────────────────────────

  getTauxOk(): number {
    const d = this.dashboard();
    if (!d || d.totalProduits === 0) return 0;
    return (d.nbOk / d.totalProduits) * 100;
  }

  getTauxRisque(): number {
    const d = this.dashboard();
    if (!d || d.totalProduits === 0) return 0;
    return ((d.nbRupture + d.nbSousSeuil) / d.totalProduits) * 100;
  }

  // ─── Utilitaires d'affichage ────────────────────────────────────

  getClasseLabel(classe?: ClasseCriticite | null): string {
    return getClasseCriticiteInfo(classe ?? undefined)?.label ?? '-';
  }

  getClasseSeverity(classe?: ClasseCriticite | null): 'danger' | 'success' | 'info' | 'warn' | 'secondary' {
    return getClasseCriticiteInfo(classe ?? undefined)?.severity ?? 'secondary';
  }

  getUrgenceSeverity(produit: ITopUrgentDTO): 'danger' | 'warn' {
    return produit.stockActuel < produit.margeSecurite ? 'danger' : 'warn';
  }

  getUrgenceLabel(produit: ITopUrgentDTO): string {
    return produit.stockActuel < produit.margeSecurite ? 'RUPTURE' : 'SOUS SEUIL';
  }

  getCouvertureClass(mois: number): string {
    if (mois < 0.5) return 'text-danger fw-bold';
    if (mois < 1.0) return 'text-warning fw-semibold';
    if (mois <= 2.0) return 'text-success';
    return 'text-info';
  }

  /** Couverture cible en mois = Stock Objectif / VMM */
  getCouvertureCibleMois(produit: ITopUrgentDTO): number {
    if (!produit.vmm || produit.vmm === 0) return 0;
    return produit.stockObjectif / produit.vmm;
  }

  /** Classe CSS pour la couverture cible */
  getCouvertureCibleClass(produit: ITopUrgentDTO): string {
    const cible = this.getCouvertureCibleMois(produit);
    if (cible < 0.5) return 'text-danger';
    if (cible < 1.0) return 'text-warning';
    if (cible <= 3.0) return 'text-success';
    return 'text-info';
  }

  trackByClasse(_index: number, item: IClasseBreakdown): string {
    return item.classeCriticite;
  }

  trackByProduit(_index: number, item: ITopUrgentDTO): number {
    return item.produitId;
  }
}

