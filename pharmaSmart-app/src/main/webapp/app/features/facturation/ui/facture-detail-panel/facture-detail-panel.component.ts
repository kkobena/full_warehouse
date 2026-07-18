import { Component, DestroyRef, effect, inject, input, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { NgbModal, NgbNavModule } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { BadgeModule } from "primeng/badge";
import { TooltipModule } from "primeng/tooltip";
import { ProgressSpinnerModule } from "primeng/progressspinner";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";

import { FactureApiService } from "../../data-access/services/facture-api.service";
import { ReglementApiService } from "../../data-access/services/reglement-api.service";
import { CertificationApiService } from "../../data-access/services/certification-api.service";
import { FacturationStore } from "../../data-access/store/facturation.store";
import {
  IDossierFactureProjection,
  IFacture,
  IFactureItem,
  IFneResponse,
  IReglement,
  IReglementFactureDossier
} from "../../data-access/models";
import { ReglementWorkspaceComponent } from "../reglement-workspace/reglement-workspace.component";
import { AvoirWorkspaceComponent } from "../avoir-workspace/avoir-workspace.component";
import { FneCertificateViewerComponent } from "../fne-certificate-viewer/fne-certificate-viewer.component";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-facture-detail-panel",
  imports: [
    CommonModule,
    NgbNavModule,
    ButtonModule,
    TableModule,
    BadgeModule,
    TooltipModule,
    ProgressSpinnerModule,
    ReglementWorkspaceComponent,
    AvoirWorkspaceComponent
  ],
  templateUrl: "./facture-detail-panel.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./facture-detail-panel.component.scss"
})
export class FactureDetailPanelComponent {
  readonly facture = input<IFacture | null>(null);
  readonly canExecute = input<boolean>(true);
  readonly canExport  = input<boolean>(true);
  readonly activeTabRequest = input<string | null>(null);

  protected loadingItems = false;
  protected certifying = false;
  protected loadingReglements = false;
  protected loadingPdf = false;
  protected certificationLoading = false;
  protected factureItems = signal<IFactureItem[]>([]);
  protected reglements = signal<IReglement[]>([]);
  protected dossierFactureProjection = signal<IDossierFactureProjection | null>(null);
  protected reglementDossiers = signal<IReglementFactureDossier[]>([]);
  protected activeTab = signal<string>("detail");


  private currentFactureId: number | null = null;
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly factureApiService = inject(FactureApiService);
  private readonly reglementApiService = inject(ReglementApiService);
  private readonly certificationApiService = inject(CertificationApiService);
  protected readonly store = inject(FacturationStore);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly modalService = inject(NgbModal);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  get isGroupe(): boolean {
    return this.facture()?.groupeFactureId != null;
  }

  getStatutSeverity(statut: string): string {
    switch (statut) {
      case "PAID":
        return "success";
      case "PARTIALLY_PAID":
        return "warn";
      case "NOT_PAID":
        return "danger";
      default:
        return "secondary";
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case "PAID":
        return "Réglé";
      case "PARTIALLY_PAID":
        return "Partiel";
      case "NOT_PAID":
        return "Impayé";
      default:
        return statut ?? "—";
    }
  }

  constructor() {
    effect(() => {
      const f = this.facture();
      if (!f?.factureItemId) return;

      const isNew = f.factureItemId.id !== this.currentFactureId;
      this.currentFactureId = f.factureItemId.id;

      this.factureItems.set([]);
      this.reglements.set([]);
      this.dossierFactureProjection.set(null);
      this.reglementDossiers.set([]);

      if (isNew) {
        this.activeTab.set(this.activeTabRequest() ?? "detail");
      }

      this.loadItems(f);
      this.loadReglements(f);
    });
  }

  onTabChange(tab: string | number): void {
    const tabId = String(tab);
    this.activeTab.set(tabId);
    const f = this.facture();
    if (!f) return;

    if (tabId === "regler" && !this.dossierFactureProjection()) {
      this.loadReglementContext(f);
    }
    if (tabId === "versements") {

      this.loadReglements(f);
    }
  }

  onClose(): void {
    this.store.selectFacture(null);
  }

  private onCertifySingle(facture: IFacture): void {
    this.certifying = true;
    this.certificationApiService.certify(facture.factureItemId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        this.certifying = false;
        const fneResponse = response.body;
        if (fneResponse) {
          this.confirmDialog.onConfirm(
            () => this.openFneCertificateViewer(fneResponse.token, fneResponse.reference),
            "Certification Réussie",
            `Facture certifiée avec succès.\nRéférence: ${fneResponse.reference}\n\nVoulez-vous visualiser la facture certifiée FNE ?`
          );
        }

      },
      error: err => {
        this.certifying = false;
        this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur de certification FNE");

      }
    });
  }

  private openFneCertificateViewer(tokenUrl: string, reference: string): void {
    const modalRef = this.modalService.open(FneCertificateViewerComponent, {
      backdrop: "static",
      size: "xl",
      centered: true,
      modalDialogClass: "fne-certificate-modal"
    });
    modalRef.componentInstance.tokenUrl = tokenUrl;
    modalRef.componentInstance.reference = reference;
  }


  private onCertifyGroupInvoice(facture: IFacture): void {
    this.certifying = true;
    this.certificationApiService.certifyGroupe(facture.factureItemId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.certifying = false;
        this.notificationService.success("Toutes les factures du groupe ont été certifiées avec succès auprès du FNE.", "Certification groupe");

      },
      error: err => {
        this.certifying = false;
        this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur de certification FNE");
      }
    });
  }

  onConfirmCertification(): void {
    this.confirmDialog.onConfirm(() => this.onCertify(), "Confirmer la certification", `Certifier la facture ${this.facture().numFacture} auprès du FNE ?`
    );
  }


  private onCertify(): void {
    const fact = this.facture();
    if (!fact.factureItemId) return;
    if (fact.groupeFactureId) {
      this.onCertifyGroupInvoice(this.facture());
    } else {
      this.onCertifySingle(this.facture());
    }
  }


  onViewFne(): void {
    const fneResponse: IFneResponse = this.facture().fneResponse;
    if (fneResponse) {
      this.openFneCertificateViewer(fneResponse.token, fneResponse.reference);
    }

  }

  onExportPdf(): void {
    const f = this.facture();
    if (!f?.factureItemId) return;
    this.loadingPdf = true;
    this.factureApiService
      .exportToPdf(f.factureItemId)
      .pipe(
        finalize(() => (this.loadingPdf = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (blob) => {
          this.downloadDocumentService.downloadPdf(blob, `facture_${f.numFacture}`);
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export PDF")
      });
  }

  private loadItems(f: IFacture): void {
    if (!f.factureItemId) return;
    this.loadingItems = true;
    this.factureApiService
      .find(f.factureItemId)
      .pipe(
        finalize(() => (this.loadingItems = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => this.factureItems.set(res.body?.items ?? []),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Chargement dossiers")
      });
  }

  private loadReglementContext(f: IFacture): void {
    if (!f.factureItemId) return;
    const isGroup = this.isGroupe;
    const typeFacture = isGroup ? "groupes" : "individuelle";

    this.factureApiService
      .findDossierFactureProjection(f.factureItemId, { isGroup })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.dossierFactureProjection.set(res.body),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Chargement projection")
      });

    this.factureApiService
      .findDossierReglement(f.factureItemId, typeFacture, { page: 0, size: 50 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.reglementDossiers.set(res.body ?? []),
        error: () => this.reglementDossiers.set([])
      });
  }

  private loadReglements(f: IFacture): void {
    if (!f.factureItemId) return;
    this.loadingReglements = true;
    this.reglementApiService
      .findByInvoice(f.factureItemId)
      .pipe(
        finalize(() => (this.loadingReglements = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => this.reglements.set(res.body ?? []),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Chargement versements")
      });
  }
}
