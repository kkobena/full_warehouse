import { Component, DestroyRef, effect, inject, input, output, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { HttpHeaders } from "@angular/common/http";
import { CommonModule } from "@angular/common";
import { NgbDropdown, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import {
  AppTableLazyLoadEvent,
  BadgeComponent,
  DataTableComponent,
  HeaderCheckboxComponent,
  RowCheckboxComponent, AppBadgeSeverity } from "../../../../shared/ui";

import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";

import { FactureApiService } from "../../data-access/services/facture-api.service";
import { CertificationApiService } from "../../data-access/services/certification-api.service";
import { FacturationStore } from "../../data-access/store/facturation.store";
import { IFacture, IFneResponse, IInvoiceSearchParams } from "../../data-access/models";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { FneCertificateViewerComponent } from "../fne-certificate-viewer/fne-certificate-viewer.component";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

type FactureMenuAction = "view" | "certify" | "view-fne" | "delete" | "avoir" | "pdf";

interface MenuEntry {
  label: string;
  icon: string;
  action: FactureMenuAction;
  color: string;
  danger?: boolean;
  separatorBefore?: boolean;
}

@Component({
  selector: "app-facture-list",
  imports: [
    CommonModule,
    NgbTooltip,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    BadgeComponent,
    DataTableComponent,
    HeaderCheckboxComponent,
    RowCheckboxComponent
  ],
  templateUrl: "./facture-list.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./facture-list.component.scss"
})
export class FactureListComponent {

  readonly searchParams = input<IInvoiceSearchParams | null>(null);
  readonly canDelete = input<boolean>(true);
  readonly canExport = input<boolean>(true);

  readonly factureSelected = output<IFacture>();
  readonly createAvoir = output<IFacture>();

  protected readonly loading = signal(false);
  protected readonly certifying = signal(false);
  protected readonly page = signal(0);
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected selectedFactures = signal<IFacture[]>([]);

  protected readonly store = inject(FacturationStore);
  private readonly factureApiService = inject(FactureApiService);
  private readonly certificationApiService = inject(CertificationApiService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly modalService = inject(NgbModal);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  constructor() {
    effect(() => {
      const params = this.searchParams();
      if (params !== null) {
        this.page.set(0);
        this.loadPage(params);
      }
    });
  }

  protected menuItemsFor(facture: IFacture): MenuEntry[] {
    const items: MenuEntry[] = [{ label: "Voir détail", icon: "pi pi-eye", action: "view", color: "text-success" }];
    if (!facture.fneResponse && !facture.factureProvisoire && facture.statut === "NOT_PAID") {
      items.push({ label: "Certifier FNE", icon: "pi pi-shield", action: "certify", color: "text-info" });
    }
    if (facture.fneResponse) {
      items.push({ label: "Voir certificat FNE", icon: "pi pi-verified", action: "view-fne", color: "text-purple" });
    }
    if (facture.statut === "PARTIALLY_PAID") {
      items.push({ label: "Créer un avoir", icon: "pi pi-file-plus", action: "avoir", color: "text-primary" });
    }
    if (this.canExport()) {
      items.push({ label: "Exporter en PDF", icon: "pi pi-file-pdf", action: "pdf", color: "text-warning" });
    }
    if (this.canDelete() && facture.statut === "NOT_PAID" && !facture.factureProvisoire) {
      items.push({ label: "Supprimer", icon: "pi pi-trash", action: "delete", color: "text-danger", danger: true, separatorBefore: true });
    }
    return items;
  }

  protected onMenuAction(facture: IFacture, action: FactureMenuAction): void {
    switch (action) {
      case "view":
        this.onRowSelect(facture);
        break;
      case "certify":
        this.onConfirmCertification(facture);
        break;
      case "view-fne":
        this.onViewFne(facture);
        break;
      case "avoir":
        this.onCreateAvoir(facture);
        break;
      case "pdf":
        this.exportPdf(facture);
        break;
      case "delete":
        this.onDeleteSingle(facture);
        break;
    }
  }

  onCreateAvoir(facture: IFacture): void {
    this.createAvoir.emit(facture);
  }

  onRowSelect(facture: IFacture): void {
    if (!facture.factureItemId) return;
    this.store.selectFacture(facture);
    this.factureSelected.emit(facture);
  }

  onSelectionChange(selection: IFacture[]): void {
    this.selectedFactures.set(selection);
    this.store.setSelectedFactures(selection);
  }

  lazyLoading(event: AppTableLazyLoadEvent): void {
    const params = this.searchParams();
    if (event && params) {
      this.page.set(Math.floor((event.first ?? 0) / (event.rows ?? this.itemsPerPage)));
      this.loadPage(params, event.rows ?? this.itemsPerPage);
    }
  }

  getStatutSeverity(statut: string): AppBadgeSeverity {
    switch (statut) {
      case "PAID":
        return "success";
      case "PARTIALLY_PAID":
        return "warn";
      case "NOT_PAID":
        return "warn";
      default:
        return "secondary";
    }
  }

  exportPdf(f: IFacture): void {
    if (!f?.factureItemId) return;
    this.factureApiService
      .exportToPdf(f.factureItemId)
      .pipe(
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

  onDeleteSingle(facture: IFacture): void {
    if (!facture.factureItemId) return;
    this.confirmDialog.onConfirm(
      () =>
        this.factureApiService
          .delete(facture.factureItemId!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.store.removeFactureFromList(facture.factureItemId!.id);
              this.notificationService.success("Facture supprimée", "Suppression");
            },
            error: err => {
              this.notificationService.error(this.errorService.getErrorMessage(err), "Suppression");
              if (err?.error?.errorKey === "factureDejaAnnulee") {
                // Annulée entre-temps sur un autre poste : elle n'a plus rien à faire dans la liste.
                this.store.removeFactureFromList(facture.factureItemId!.id);
              }
            }
          }),
      "Suppression facture",
      `Supprimer la facture ${facture.numFacture} ?`
    );
  }

  onViewFne(facture: IFacture): void {
    const fneResponse: IFneResponse = facture.fneResponse;
    if (fneResponse) {
      this.openFneCertificateViewer(fneResponse.token, fneResponse.reference);
    }

  }

  private onCertifySingle(facture: IFacture): void {
    this.certifying.set(true);
    this.certificationApiService.certify(facture.factureItemId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        this.certifying.set(false);
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
        this.certifying.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur de certification FNE");

      }
    });
  }

  openFneCertificateViewer(tokenUrl: string, reference: string): void {
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
    this.certifying.set(true);
    this.certificationApiService.certifyGroupe(facture.factureItemId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.certifying.set(false);
        this.notificationService.success("Toutes les factures du groupe ont été certifiées avec succès auprès du FNE.", "Certification groupe");

      },
      error: err => {
        this.certifying.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur de certification FNE");
      }
    });
  }

  onConfirmCertification(facture: IFacture): void {

    this.confirmDialog.onConfirm(() => this.onCertify(facture), "Confirmer la certification", `Certifier la facture ${facture.numFacture} auprès du FNE ?`
    );
  }

  private onCertify(facture: IFacture): void {
    if (!facture.factureItemId) return;
    if (facture.groupeFactureId) {
      this.onCertifyGroupInvoice(facture);
    } else {
      this.onCertifySingle(facture);
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

  private loadPage(params: IInvoiceSearchParams, rows = this.itemsPerPage): void {
    this.loading.set(true);
    this.store.setLoading(true);
    this.factureApiService
      .query({ ...params, page: this.page(), size: rows } as any)
      .pipe(
        finalize(() => (this.loading.set(false))),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => this.onSuccess(res.body, res.headers),
        error: err => {
          this.store.setLoading(false);
          this.notificationService.error(this.errorService.getErrorMessage(err), "Chargement factures");
        }
      });
  }

  private onSuccess(data: IFacture[] | null, headers: HttpHeaders): void {
    this.store.setFactures(data ?? [], Number(headers.get("X-Total-Count")));
  }
}
