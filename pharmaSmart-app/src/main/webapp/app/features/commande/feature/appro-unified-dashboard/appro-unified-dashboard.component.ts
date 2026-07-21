import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule, DatePipe, NgClass } from "@angular/common";
import { Router } from "@angular/router";
import { HttpResponse } from "@angular/common/http";

import { NgbTooltip } from "@ng-bootstrap/ng-bootstrap";

import { CommandeService, ICommandeDashboard, ICommandeResumee } from "app/entities/commande/commande.service";
import { SemoisService } from "app/entities/semois/semois.service";
import { BudgetCommande, SemoisFraicheur, SuggestionService } from "app/entities/commande/suggestion/suggestion.service";
import { CommandCommonService } from "app/entities/commande/command-common.service";
import { IReapproDashboard, ITopUrgentDTO } from "app/shared/model/semois/semois-dashboard.model";
import { AlertBadgeService } from "app/shared/services/alert-badge.service";
import {
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  KpiItemComponent,
  KpiStripComponent,
  SkeletonComponent,
  ToolbarComponent,
} from "app/shared/ui";

@Component({
  selector: "app-appro-unified-dashboard",
  templateUrl: "./appro-unified-dashboard.component.html",
  styleUrls: ["./appro-unified-dashboard.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    DatePipe, CommonModule,
    NgbTooltip, KpiStripComponent, KpiItemComponent,
    BadgeComponent, ButtonComponent, DataTableComponent, SkeletonComponent, ToolbarComponent,
  ],
})
export class ApproUnifiedDashboardComponent implements OnInit {
  readonly loadingCommandes = signal(true);
  readonly loadingSemois = signal(true);
  readonly lastRefresh = signal<Date | null>(null);

  readonly commandeDashboard = signal<ICommandeDashboard | null>(null);
  readonly semoisDashboard = signal<IReapproDashboard | null>(null);
  readonly semoisFraicheur = signal<SemoisFraicheur | null>(null);
  readonly budget = signal<BudgetCommande | null>(null);

  private readonly commandeService = inject(CommandeService);
  private readonly semoisService = inject(SemoisService);
  private readonly suggestionService = inject(SuggestionService);
  private readonly router = inject(Router);
  private readonly commandCommonService = inject(CommandCommonService);
  /** Service partagé — source unique de vérité pour tous les compteurs d'alertes */
  readonly alertBadgeService = inject(AlertBadgeService);

  /** Compatible avec l'usage template existant : peremptionCount() */
  peremptionCount(): number {
    return this.alertBadgeService.peremptionCount();
  }

  ngOnInit(): void {
    this.alertBadgeService.init();
    this.loadAll();
  }

  loadAll(): void {
    this.loadingCommandes.set(true);
    this.loadingSemois.set(true);
    this.lastRefresh.set(null);

    this.commandeService.getDashboard().subscribe({
      next: data => { this.commandeDashboard.set(data); this.loadingCommandes.set(false); this.checkRefreshDone(); },
      error: () => this.loadingCommandes.set(false),
    });

    this.semoisService.getDashboard().subscribe({
      next: (res: HttpResponse<IReapproDashboard>) => { this.semoisDashboard.set(res.body); this.loadingSemois.set(false); this.checkRefreshDone(); },
      error: () => this.loadingSemois.set(false),
    });

    this.suggestionService.getSemoisFraicheur().subscribe({
      next: f => this.semoisFraicheur.set(f),
      error: () => this.semoisFraicheur.set(null),
    });

    this.suggestionService.getBudget().subscribe({
      next: b => this.budget.set(b),
      error: () => this.budget.set(null),
    });

    this.alertBadgeService.refresh();
  }

  navigateToPeremptions(): void { this.router.navigate(['/gestion-peremption']); }

  private checkRefreshDone(): void {
    if (!this.loadingCommandes() && !this.loadingSemois()) {
      this.lastRefresh.set(new Date());
    }
  }

  get isLoading(): boolean { return this.loadingCommandes() || this.loadingSemois(); }

  // ─── Navigation ──────────────────────────────────────────────────────────

  openCommande(row: ICommandeResumee): void {
    const commandeId = { id: row.id, orderDate: row.orderDate };
    if (row.orderStatus === "RECEIVED") {
      this.commandCommonService.pendingOpenDeliveryId.set(commandeId);
      this.commandCommonService.navigateToBonsLivraison();
    } else {
      this.commandCommonService.pendingOpenCommandeId.set(commandeId);
      this.commandCommonService.navigateToCommandesAPasser();
    }
  }

  navigateToSemoisSuggestions(): void { this.commandCommonService.navigateToAnalyse(); }
  navigateToCommandeEnCours(): void {
    this.commandCommonService.pendingNewCommande.set(true);
    this.commandCommonService.navigateToCommandesAPasser();
  }
  navigateToReceptionEnAttente(): void { this.commandCommonService.navigateToBonsLivraison(); }

  // ─── Calculs VMM ─────────────────────────────────────────────────────────

  getTauxOk(): number {
    const d = this.semoisDashboard();
    if (!d || d.totalProduits === 0) return 0;
    return (d.nbOk / d.totalProduits) * 100;
  }

  getTauxRisque(): number {
    const d = this.semoisDashboard();
    if (!d || d.totalProduits === 0) return 0;
    return ((d.nbRupture + d.nbSousSeuil) / d.totalProduits) * 100;
  }

  getCouvertureMois(produit: ITopUrgentDTO): number {
    if (!produit.vmm || produit.vmm === 0 || produit.stockActuel < 0) return 0;
    return produit.stockActuel / produit.vmm;
  }

  getCouvertureClass(produit: ITopUrgentDTO): string {
    const mois = this.getCouvertureMois(produit);
    if (mois < 0.5) return "text-danger fw-bold";
    if (mois < 1.0) return "text-warning fw-semibold";
    return "text-success";
  }

  getUrgenceSeverity(produit: ITopUrgentDTO): "danger" | "warn" {
    return produit.stockActuel < produit.margeSecurite ? "danger" : "warn";
  }

  // ─── Fraîcheur VMM ───────────────────────────────────────────────────────

  get semoisFraicheurLabel(): string {
    const f = this.semoisFraicheur();
    if (!f) return "VMM inconnue";
    if (f.calculeRecent) return "VMM · À jour";
    if (f.dernierCalcul) return `VMM · ${new Date(f.dernierCalcul).toLocaleDateString("fr-FR")}`;
    return "VMM · Non initialisée";
  }

  get semoisFraicheurSeverity(): "success" | "warn" | "danger" | "secondary" {
    const f = this.semoisFraicheur();
    if (!f) return "secondary";
    if (f.calculeRecent) return "success";
    if (f.dernierCalcul) return "warn";
    return "danger";
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat("fr-FR").format(Math.round(amount / 100));
  }
}

