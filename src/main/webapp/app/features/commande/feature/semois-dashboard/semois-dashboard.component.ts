import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';

import { IReapproDashboard, IClasseBreakdown, ITopUrgentDTO } from 'app/shared/model/semois/semois-dashboard.model';
import { ClasseCriticite, getClasseCriticiteInfo } from 'app/shared/model/semois/classe-criticite.model';
import { SemoisService } from 'app/entities/semois/semois.service';
import { CommandCommonService } from 'app/entities/commande/command-common.service';

@Component({
  standalone: true,
  selector: 'app-semois-dashboard',
  templateUrl: './semois-dashboard.component.html',
  styleUrls: ['./semois-dashboard.component.scss'],
  imports: [DatePipe, DecimalPipe, NgClass, TableModule, ButtonModule, ToolbarModule, Tag, ProgressBarModule, SkeletonModule],
})
export class SemoisDashboardComponent implements OnInit {
  readonly dashboard = signal<IReapproDashboard | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly lastRefresh = signal<Date | null>(null);

  private readonly semoisService = inject(SemoisService);
  private readonly commandCommonService = inject(CommandCommonService);

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

  /** Bascule vers l'onglet SEMOIS_SUGGESTIONS dans commande-home */
  navigateToSuggestions(_classe?: ClasseCriticite): void {
    this.commandCommonService.updateCommandPreviousActiveNav('SEMOIS_SUGGESTIONS');
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

