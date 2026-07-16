import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { HttpResponse } from "@angular/common/http";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { TooltipModule } from "primeng/tooltip";
import { TagModule } from "primeng/tag";
import { ToastModule } from "primeng/toast";
import { InputTextModule } from "primeng/inputtext";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { SelectModule } from "primeng/select";
import { DatePicker } from "primeng/datepicker";
import { FloatLabel } from "primeng/floatlabel";
import { SplitButtonModule } from "primeng/splitbutton";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { MenuItem } from "primeng/api";
import { NotificationService } from "app/shared/services/notification.service";
import { NgbConfirmDialogService } from "app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { IRetourBon } from "app/shared/model/retour-bon.model";
import { IRetourBonGroupe } from "app/shared/model/retour-bon-groupe.model";
import { IAvoirFournisseur } from "app/shared/model/avoir-fournisseur.model";
import { RetourBonStatut } from "app/shared/model/enumerations/retour-bon-statut.model";
import { ITEMS_PER_PAGE } from "app/shared/constants/pagination.constants";
import { DATE_FORMAT_ISO_DATE } from "app/shared/util/warehouse-util";
import { RetourBonService } from "../../../../entities/commande/retour_fournisseur/retour-bon.service";
import {
  SupplierResponseModalComponent
} from "../../../../entities/commande/retour_fournisseur/supplier-response-modal.component";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import { AvoirEncoursComponent } from "./avoir-encours/avoir-encours.component";
import { AvoirFournisseurService } from "../../../../entities/commande/retour_fournisseur/avoir-fournisseur.service";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

export type RetourTab = "EN_ATTENTE" | "HISTORIQUE" | "AVOIRS" | "GROUPE";

@Component({
  selector: "app-retour-fournisseur",
  templateUrl: "./retour-fournisseur.component.html",
  styleUrls: ["./retour-fournisseur.scss"],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    IconField,
    InputIcon,
    SelectModule,
    DatePicker,
    FloatLabel,
    TableModule,
    TooltipModule,
    TagModule,
    ToastModule,
    SplitButtonModule,
    AvoirEncoursComponent
  ]
})
export class AppRetourFournisseurComponent implements OnInit {
  protected search = "";
  protected selectedStatut: RetourBonStatut | null = RetourBonStatut.VALIDATED;
  protected dtStart: Date | null = null;
  protected dtEnd: Date | null = null;

  /** Onglet actif : retours en cours (VALIDATED/PROCESSING) ou historique (CLOSED) */
  protected activeTab = signal<RetourTab>("EN_ATTENTE");
  /** Badge : nb de retours VALIDATED en attente d'action */
  protected countEnAttente = signal<number>(0);
  protected countAvoirs = signal<number>(0);

  /** Options de filtre selon l'onglet actif */
  protected readonly enAttenteStatutOptions = [
    { label: "En attente de réponse", value: RetourBonStatut.VALIDATED },
    { label: "En cours de traitement", value: RetourBonStatut.PROCESSING },
    { label: "Partiellement accepté", value: RetourBonStatut.PARTIALLY_ACCEPTED }
  ];

  protected retourBons = signal<IRetourBon[]>([]);
  protected retourBonsGroupes = signal<IRetourBonGroupe[]>([]);
  protected loadingGroupes = signal<boolean>(false);
  protected loading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = signal<number>(0);
  protected readonly RetourBonStatut = RetourBonStatut;
  private readonly destroyRef = inject(DestroyRef);
  private readonly downloadDocumentService = inject(BlobDownloadService);
  private readonly retourBonService = inject(RetourBonService);
  private readonly avoirFournisseurService = inject(AvoirFournisseurService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly modalService = inject(NgbModal);
  private readonly router = inject(Router);

  protected exportMenus: MenuItem[] = [
    {
      label: "Excel",
      icon: "pi pi-file-excel",
      command: () => this.exportRetourBons("excel")
    },
    {
      label: "CSV",
      icon: "pi pi-file-export",
      command: () => this.exportRetourBons("csv")
    }
  ];

  ngOnInit(): void {
    this.loadBadges();
    this.loadAll();
  }

  onSearch(): void {
    this.page.set(0);
    this.loadAll();
  }

  onNewRetour(): void {
    void this.router.navigate(["/commande/retour-fournisseur/new"]);
  }

  /** Bascule vers l'onglet cible et recharge les données */
  protected setTab(tab: RetourTab): void {
    if (this.activeTab() === tab) return;
    this.activeTab.set(tab);
    this.search = "";
    this.dtStart = null;
    this.dtEnd = null;
    this.loadBadges();
    if (tab === "AVOIRS") return;
    if (tab === "GROUPE") {
      this.loadGroupes();
      return;
    }
    this.selectedStatut = tab === "EN_ATTENTE" ? null : RetourBonStatut.CLOSED;
    this.page.set(0);
    this.loadAll();
  }

  protected loadGroupes(): void {
    this.loadingGroupes.set(true);
    this.retourBonService.getGroupedByFournisseur().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.retourBonsGroupes.set(res.body || []);
        this.loadingGroupes.set(false);
      },
      error: () => {
        this.notificationService.error("Erreur lors du chargement des retours groupés");
        this.loadingGroupes.set(false);
      }
    });
  }

  protected exportGroupePdf(groupe: IRetourBonGroupe): void {
    const ids = (groupe.retourBons || []).map(r => r.id!).filter(id => !!id);
    this.retourBonService.exportGroupe(ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (blob: Blob) => {
        this.downloadDocumentService.downloadPdf(blob, `bordereau-groupe-${groupe.fournisseurId}`);

      },
      error: () => {
        this.notificationService.error("Impossible de générer le bordereau groupé");
      }
    });
  }

  protected loadAll(): void {
    this.loading.set(true);
    const query: any = {
      page: this.page(),
      size: this.itemsPerPage
    };
    if (this.dtStart) {
      query.dtStart = DATE_FORMAT_ISO_DATE(this.dtStart);
    }
    if (this.dtEnd) {
      query.dtEnd = DATE_FORMAT_ISO_DATE(this.dtEnd);
    }
    if (this.search) {
      query.search = this.search;
    }

    if (this.activeTab() === "HISTORIQUE") {
      query.statut = RetourBonStatut.CLOSED;
    } else if (this.selectedStatut) {
      query.statut = this.selectedStatut;
    } else {
      // EN_ATTENTE sans filtre : exclure CLOSED
      query.excludeStatut = RetourBonStatut.CLOSED;
    }

    const observable = this.retourBonService.query(query);

    observable.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: HttpResponse<IRetourBon[]>) => {
        this.onSuccess(res.body, res.headers);
        this.loading.set(false);
      },
      error: () => {
        this.onError();
        this.loading.set(false);
      }
    });
  }

  protected onPageChange(event: any): void {
    this.page.set(event.page);
    this.loadAll();
  }

  protected setSupplierResponse(retourBon: IRetourBon): void {
    showCommonModal(
      this.modalService,
      SupplierResponseModalComponent,
      {
        retourBon,
        title: `Saisir la réponse fournisseur - ${retourBon.receiptReference}`
      },
      (avoir: IAvoirFournisseur) => {
        if (avoir) {
          this.notificationService.success("Avoir fournisseur créé avec succès — réf. " + (avoir.reference ?? ""));
          this.loadAll();
        }
      },
      "xl"
    );
  }

  protected getStatusSeverity(statut: RetourBonStatut): "success" | "info" | "warn" | "danger" | "secondary" {
    switch (statut) {
      case RetourBonStatut.VALIDATED:
        return "info";
      case RetourBonStatut.PROCESSING:
        return "secondary";
      case RetourBonStatut.CLOSED:
        return "success";
      case RetourBonStatut.PARTIALLY_ACCEPTED:
        return "warn";
      default:
        return "info";
    }
  }

  protected getStatusLabel(statut: RetourBonStatut): string {
    switch (statut) {
      case RetourBonStatut.VALIDATED:
        return "En attente de réponse";
      case RetourBonStatut.PROCESSING:
        return "En cours";
      case RetourBonStatut.CLOSED:
        return "Clôturé";
      case RetourBonStatut.PARTIALLY_ACCEPTED:
        return "Partiellement accepté";
      default:
        return statut;
    }
  }

  protected getTotalItems(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.length || 0;
  }

  protected getTotalQuantity(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.reduce((sum, item) => sum + (item.qtyMvt || 0), 0) || 0;
  }

  protected getTotalAccepted(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.reduce((sum, item) => sum + (item.acceptedQty || 0), 0) || 0;
  }

  private onSuccess(data: IRetourBon[] | null, headers: any): void {
    this.totalRecords.set(Number(headers.get("X-Total-Count")));
    this.retourBons.set(data || []);
  }

  private onError(): void {
    this.notificationService.error("Erreur lors du chargement des retours");
  }

  protected loadBadges(): void {
    this.retourBonService.countEnAttente()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: count => this.countEnAttente.set(count),
        error: () => {}
      });
    this.avoirFournisseurService.countEnAttente()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: count => this.countAvoirs.set(count),
        error: () => {}
      });
  }

  protected downloadPdf(retourBon: IRetourBon): void {
    this.retourBonService.getPdf(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (blob: Blob) => {
        this.downloadDocumentService.downloadPdf(blob, `retour_bon-${retourBon.id}`);

      },
      error: () => {
        this.notificationService.error("Impossible de générer le PDF");
      }
    });
  }

  protected editRetour(retourBon: IRetourBon): void {
    void this.router.navigate(["/commande/retour-fournisseur", retourBon.id, "edit"]);
  }

  protected deleteRetour(retourBon: IRetourBon): void {
    this.confirmDialog.onConfirm(
      () => {
        this.retourBonService.delete(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => {
            this.notificationService.success("Retour supprimé avec succès");
            this.loadAll();
          },
          error: () => {
            this.notificationService.error("Impossible de supprimer ce retour");
          }
        });
      },
      "Supprimer le retour",
      `Supprimer le retour #${retourBon.id} (${retourBon.fournisseurLibelle}) ? Cette action est irréversible.`
    );
  }

  protected sendEdi(retourBon: IRetourBon): void {
    this.retourBonService.sendEdi(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success("Retour envoyé via EDI PharmaML avec succès");
        this.loadAll();
      },
      error: () => {
        this.notificationService.error("Erreur lors de l'envoi EDI. Vérifiez la configuration PharmaML du fournisseur.");
      }
    });
  }

  protected markAsProcessing(retourBon: IRetourBon): void {
    this.retourBonService.markAsProcessing(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success("Retour marqué en cours de traitement");
        this.loadAll();
      },
      error: () => {
        this.notificationService.error("Impossible de mettre à jour le statut");
      }
    });
  }

  protected closeManually(retourBon: IRetourBon): void {
    this.confirmDialog.onConfirm(
      () => {
        this.retourBonService.closeManually(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => {
            this.notificationService.success("Retour clôturé manuellement");
            this.loadAll();
          },
          error: () => {
            this.notificationService.error("Impossible de clôturer ce retour");
          }
        });
      },
      "Clôturer manuellement",
      `Clôturer manuellement le retour ${retourBon.reference ?? "#" + retourBon.id} (acceptation partielle) ?`
    );
  }


  protected exportRetourBons(format: "excel" | "csv"): void {
    const statutToUse =
      this.activeTab() === "HISTORIQUE"
        ? RetourBonStatut.CLOSED
        : this.selectedStatut;

    const params: Record<string, string> = {};
    if (statutToUse) params["statut"] = statutToUse;
    if (this.dtStart) params["dtStart"] = DATE_FORMAT_ISO_DATE(this.dtStart)!;
    if (this.dtEnd) params["dtEnd"] = DATE_FORMAT_ISO_DATE(this.dtEnd)!;
    if (this.search) params["search"] = this.search;

    this.retourBonService.export(format, params).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        this.downloadDocumentService.download(blob, "retours-fournisseur", format);
      },
      error: () => {
        this.notificationService.error("Erreur lors de l'export des retours fournisseur.", "Erreur");
      }
    });
  }
}
