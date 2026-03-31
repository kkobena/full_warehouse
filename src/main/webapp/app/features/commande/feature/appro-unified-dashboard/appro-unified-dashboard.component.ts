import { Component, inject, OnInit, signal } from "@angular/core";
import { DatePipe, DecimalPipe, NgClass } from "@angular/common";
import { Router } from "@angular/router";
import { HttpResponse } from "@angular/common/http";

import { TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { ToolbarModule } from "primeng/toolbar";
import { Tag } from "primeng/tag";
import { ProgressBarModule } from "primeng/progressbar";
import { SkeletonModule } from "primeng/skeleton";
import { TooltipModule } from "primeng/tooltip";
import { BadgeModule } from "primeng/badge";

import {
  CommandeService,
  ICommandeDashboard,
  ICommandeResumee,
  IPharmaMlEnvoiResumee
} from "app/entities/commande/commande.service";
import { SemoisService } from "app/entities/semois/semois.service";
import {
  BudgetCommande,
  SemoisFraicheur,
  SuggestionService
} from "app/entities/commande/suggestion/suggestion.service";
import { CommandCommonService } from "app/entities/commande/command-common.service";
import { NotificationService } from "app/shared/services/notification.service";
import { ErrorService } from "app/shared/error.service";
import { IReapproDashboard, ITopUrgentDTO } from "app/shared/model/semois/semois-dashboard.model";

@Component({
  selector: "app-appro-unified-dashboard",
  templateUrl: "./appro-unified-dashboard.component.html",
  styleUrls: ["./appro-unified-dashboard.component.scss"],
  imports: [
    DatePipe,
    DecimalPipe,
    NgClass,
    TableModule,
    ButtonModule,
    ToolbarModule,
    Tag,
    ProgressBarModule,
    SkeletonModule,
    TooltipModule,
    BadgeModule
  ]
})
export class ApproUnifiedDashboardComponent implements OnInit {
  readonly loadingCommandes = signal(true);
  readonly loadingSemois = signal(true);
  readonly recalculEnCours = signal(false);
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
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loadingCommandes.set(true);
    this.loadingSemois.set(true);
    this.lastRefresh.set(null);

    this.commandeService.getDashboard().subscribe({
      next: data => {
        this.commandeDashboard.set(data);
        this.loadingCommandes.set(false);
        this.checkRefreshDone();
      },
      error: () => this.loadingCommandes.set(false)
    });

    this.semoisService.getDashboard().subscribe({
      next: (res: HttpResponse<IReapproDashboard>) => {
        this.semoisDashboard.set(res.body);
        this.loadingSemois.set(false);
        this.checkRefreshDone();
      },
      error: () => this.loadingSemois.set(false)
    });

    this.suggestionService.getSemoisFraicheur().subscribe({
      next: f => this.semoisFraicheur.set(f),
      error: () => this.semoisFraicheur.set(null)
    });

    this.suggestionService.getBudget().subscribe({
      next: b => this.budget.set(b),
      error: () => this.budget.set(null)
    });
  }

  /** Déclenche un recalcul VMM manuel puis recharge le tableau de bord. */
  recalculerSemois(): void {
    this.recalculEnCours.set(true);
    this.suggestionService.recalculerSemois().subscribe({
      next: () => {
        this.notificationService.success("Recalcul VMM déclenché — mise à jour dans quelques instants.", "VMM");
        this.recalculEnCours.set(false);
        this.loadAll();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), "Recalcul VMM");
        this.recalculEnCours.set(false);
      }
    });
  }

  private checkRefreshDone(): void {
    if (!this.loadingCommandes() && !this.loadingSemois()) {
      this.lastRefresh.set(new Date());
    }
  }

  get isLoading(): boolean {
    return this.loadingCommandes() || this.loadingSemois();
  }

  // ─── Calculs budget ───────────────────────────────────────────────────────────

  getBudgetPct(): number {
    const b = this.budget();
    if (!b || b.budgetIllimite || b.budgetMensuel === 0) return 0;
    return Math.min(100, Math.round(((b.montantCommande + b.montantEstime) / b.budgetMensuel) * 100));
  }

  getBudgetBarSeverity(): string {
    const pct = this.getBudgetPct();
    if (pct >= 100) return "danger";
    if (pct >= 80) return "warning";
    return "success";
  }

  // ─── Navigation ──────────────────────────────────────────────────────────────

  openCommande(row: ICommandeResumee): void {
    const commandeId = { id: row.id, orderDate: row.orderDate };
    if (row.orderStatus === 'RECEIVED') {
      this.commandCommonService.pendingOpenDeliveryId.set(commandeId);
      this.commandCommonService.navigateToBonsLivraison();
    } else {
      // REQUESTED (ou autre statut) → onglet Commandes à passer
      this.commandCommonService.pendingOpenCommandeId.set(commandeId);
      this.commandCommonService.navigateToCommandesAPasser();
    }
  }

  openCommandeFromEnvoi(row: IPharmaMlEnvoiResumee): void {
    this.router.navigate(["/commande", row.commandeId, row.commandeOrderDate, "edit"]);
  }

  navigateToSemoisSuggestions(): void {
    this.commandCommonService.navigateToAnalyse();
  }

  navigateToCommandeEnCours(): void {
    this.commandCommonService.navigateToCommandesAPasser();
  }

  navigateToReceptionEnAttente(): void {
    this.commandCommonService.navigateToBonsLivraison();
  }

  navigateToSuggestions(): void {
    this.commandCommonService.navigateToReappro();
  }

  newCommande(): void {
    this.router.navigate(["/commande", "new"]);
  }

  // ─── Calculs VMM ─────────────────────────────────────────────────────────────

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

  // ─── Fraîcheur VMM ───────────────────────────────────────────────────────────

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


