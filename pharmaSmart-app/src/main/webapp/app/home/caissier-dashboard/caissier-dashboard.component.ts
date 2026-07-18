import { Component, computed, inject, OnDestroy, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { HttpResponse } from "@angular/common/http";
import { Router } from "@angular/router";
import { EMPTY, Subject } from "rxjs";
import { catchError, finalize, takeUntil, tap } from "rxjs/operators";

import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { TagModule } from "primeng/tag";
import { ToastModule } from "primeng/toast";
import { BadgeModule } from "primeng/badge";
import { TooltipModule } from "primeng/tooltip";

import { NgbModal } from "@ng-bootstrap/ng-bootstrap";

import { CaissierDashboardService } from "./caissier-dashboard.service";
import {
  ICaisseStatus,
  ICaissierDashboard,
  IDiffereARelancer,
  IEncaissementParMode,
  ILivraisonAttendue,
  IResumeDifferes,
  ISessionEncaissements,
  IVenteRecente,
  PaymentGroup
} from "./caissier-dashboard.model";

import { AccountService } from "app/core/auth/account.service";
import { UserService } from "app/core/user/user.service";
import { IUser } from "app/core/user/user.model";
import { TauriPrinterService } from "app/shared/services/tauri-printer.service";
import { FormTransactionComponent } from "app/entities/mvt-caisse/form-transaction/form-transaction.component";
import { RecapitulatifCaisseService } from "app/entities/ticketZ/recapitulatif-caisse.service";
import { RecapParam } from "app/entities/ticketZ/model/recap-param.model";
import { DATE_FORMAT_ISO_DATE } from "app/shared/util/warehouse-util";
import { NotificationService } from "../../shared/services/notification.service";

@Component({
  selector: "app-caissier-dashboard",
  imports: [
    CommonModule,
    ButtonModule,
    TableModule,
    TagModule,
    ToastModule,
    BadgeModule,
    TooltipModule
  ],
  templateUrl: "./caissier-dashboard.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["./caissier-dashboard.component.scss"]
})
export class CaissierDashboardComponent implements OnInit, OnDestroy {

  protected caisseStatus = signal<ICaisseStatus | null>(null);
  protected sessionEncaissements = signal<ISessionEncaissements | null>(null);
  protected resumeDifferes = signal<IResumeDifferes | null>(null);
  protected livraisonsAttendues = signal<ILivraisonAttendue[]>([]);
  protected ventesRecentes = signal<IVenteRecente[]>([]);
  protected isLoading = signal<boolean>(false);
  protected isPrintingZ = signal<boolean>(false);
  protected lastRefresh = signal<Date | null>(null);
  protected currentUser = signal<IUser | null>(null);

  private readonly destroy$ = new Subject<void>();

  protected isCaisseOuverte = computed(() => this.caisseStatus()?.etat === "OUVERTE");

  protected nombreDifferesUrgents = computed(() =>
    (this.resumeDifferes()?.differes ?? []).filter(
      d => d.urgence === "CRITIQUE" || d.urgence === "AUJOURD_HUI"
    ).length
  );


  private readonly dashboardService = inject(CaissierDashboardService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly accountService = inject(AccountService);
  private readonly userService = inject(UserService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly recapService = inject(RecapitulatifCaisseService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }


  private loadCurrentUser(): void {
    const account = this.accountService.trackCurrentAccount()();
    if (account?.login) {
      this.userService.find(account.login).pipe(
        takeUntil(this.destroy$)
      ).subscribe({
        next: (user: IUser) => this.currentUser.set(user),
        error: () => { /* non bloquant */
        }
      });
    }
  }

  protected loadDashboardData(): void {
    this.isLoading.set(true);
    this.dashboardService.getDashboardData().pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (res: HttpResponse<ICaissierDashboard>) => {
        const data = res.body;
        if (data) {
          this.caisseStatus.set(data.caisseStatus ?? null);
          this.sessionEncaissements.set(data.sessionEncaissements ?? null);
          this.resumeDifferes.set(data.resumeDifferes ?? null);
          this.livraisonsAttendues.set(data.livraisonsAttendues ?? []);
          this.ventesRecentes.set(data.ventesRecentes ?? []);
        }
        this.lastRefresh.set(new Date());
      },
      error: () => {
        this.notificationService.error("Erreur lors du chargement du tableau de bord");
      }
    });
  }

  protected refreshDashboard(): void {
    this.loadDashboardData();
  }

  // ─── Actions Quick Actions ──────────────────────────────────────────────────
  protected nouvelleVente(): void {
    this.router.navigate(["/sales-home"]);
  }

  protected gereCaisse(): void {
    this.router.navigate(["/my-cash-register"]);
  }

  /** Imprime le ticket Z du jour en cours
   *  filtré sur l'utilisateur connecté uniquement (usersId: [currentUserId])
   */
  protected imprimerTicketZ(): void {
    const userId = this.currentUser()?.id;
    const params: RecapParam = {
      fromDate: DATE_FORMAT_ISO_DATE(new Date()),
      toDate: DATE_FORMAT_ISO_DATE(new Date()),
      fromTime: "00:00:00",
      toTime: "23:59:59",
      onlyVente: false,
      usersId: userId ? [userId] : null
    };

    this.isPrintingZ.set(true);

    if (this.tauriPrinterService.isRunningInTauri()) {
      this.recapService
        .getEscPosReceiptForTauri(params)
        .pipe(
          tap(async (escposData: ArrayBuffer) => {
            try {
              await this.tauriPrinterService.printEscPosFromBuffer(escposData);
            } catch {
              this.notificationService.error("Erreur lors de l'impression");
            }
          }),
          catchError(() => {
            this.notificationService.error("Erreur lors de la récupération du reçu");
            return EMPTY;
          }),
          finalize(() => this.isPrintingZ.set(false)),
          takeUntil(this.destroy$)
        )
        .subscribe();
    } else {
      this.recapService
        .print(params)
        .pipe(
          tap(() => {
            this.notificationService.success("Impression lancée avec succès");
          }),
          catchError(() => {
            this.notificationService.error("Erreur lors de l'impression");
            return EMPTY;
          }),
          finalize(() => this.isPrintingZ.set(false)),
          takeUntil(this.destroy$)
        )
        .subscribe();
    }
  }

  /** Ouvre le formulaire de mouvement de caisse via NgbModal */
  protected ouvrirMouvementCaisse(): void {
    const ref = this.modalService.open(FormTransactionComponent, {
      size: "lg",
      backdrop: "static"
    });
    ref.componentInstance.header = "Mouvement de Caisse";
    ref.result.then(
      result => {
        if (result) {
          this.loadDashboardData();
          this.notificationService.success("Mouvement enregistré avec succès");
        }
      },
      () => { /* fermé sans enregistrement */
      }
    );
  }

  /** Naviguer vers le journal des ventes (seule page permettant de consulter le détail d'une vente) */
  protected voirDetailVente(_vente: IVenteRecente): void {
    this.router.navigate(["/sales-home/gestion"]);
  }

  // ─── Helpers formatage ─────────────────────────────────────────────────────
  protected formatCurrency(value: number): string {
    if (value == null) return "—";
    return new Intl.NumberFormat("fr-FR", { style: "currency", currency: "XOF" }).format(value);
  }

  protected getTypeVenteLabel(type: string): string {
    switch (type?.toUpperCase()) {
      case "COMPTANT":
        return "Comptant";
      case "ASSURANCE":
        return "Assurance";
      case "CARNET":
        return "Carnet";
      case "DIFFERE":
        return "Différé";
      default:
        return type ?? "—";
    }
  }

  protected getTypeVenteSeverity(type: string): "success" | "info" | "warn" | "secondary" {
    switch (type?.toUpperCase()) {
      case "COMPTANT":
        return "success";
      case "ASSURANCE":
        return "info";
      case "CARNET":
        return "warn";
      case "DIFFERE":
        return "secondary";
      default:
        return "secondary";
    }
  }

  protected getUrgenceClass(urgence: IDiffereARelancer["urgence"]): string {
    switch (urgence) {
      case "CRITIQUE":
        return "badge bg-danger text-white";
      case "AUJOURD_HUI":
        return "badge bg-warning text-dark";
      case "RETARD":
        return "badge bg-warning text-dark";
      default:
        return "badge bg-secondary";
    }
  }

  protected getUrgenceLabel(d: IDiffereARelancer): string {
    if (d.joursRetard === 0) return "Aujourd'hui";
    if (d.joursRetard > 0) return `Retard ${d.joursRetard}j`;
    return "À venir";
  }


  protected getModeIcon(ligne: IEncaissementParMode): string {
    switch (ligne.paymentGroup as PaymentGroup) {
      case "CASH":
        return "pi pi-money-bill";
      case "MOBILE":
        return "pi pi-mobile";
      case "CB":
        return "pi pi-credit-card";
      case "CHEQUE":
        return "pi pi-file";
      case "VIREMENT":
        return "pi pi-arrows-h";
      case "CREDIT":
        return "pi pi-book";
      case "CAUTION":
        return "pi pi-shield";
      default:
        return "pi pi-wallet";
    }
  }

  protected getModeColorClass(ligne: IEncaissementParMode): string {
    switch (ligne.paymentGroup as PaymentGroup) {
      case "CASH":
        return "text-success";
      case "MOBILE":
        return "text-primary";
      case "CB":
        return "text-info";
      case "CHEQUE":
        return "text-secondary";
      case "VIREMENT":
        return "text-info";
      case "CREDIT":
        return "text-warning";
      case "CAUTION":
        return "text-orange";
      default:
        return "text-muted";
    }
  }

  protected isModeEncaisse(ligne: IEncaissementParMode): boolean {
    return ["CASH", "MOBILE", "CB", "CHEQUE", "VIREMENT"].includes(ligne.paymentGroup);
  }
}
