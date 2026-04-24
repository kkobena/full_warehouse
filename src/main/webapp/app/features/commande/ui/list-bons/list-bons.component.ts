import { Component, computed, DestroyRef, inject, Injector, OnInit, signal, viewChild } from "@angular/core";
import { takeUntilDestroyed, toObservable } from "@angular/core/rxjs-interop";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { forkJoin } from "rxjs";
import { filter } from "rxjs/operators";
import { FormsModule } from "@angular/forms";
import { CommonModule, DatePipe } from "@angular/common";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
import { SelectModule } from "primeng/select";
import { DatePicker } from "primeng/datepicker";
import { FloatLabel } from "primeng/floatlabel";
import { InputTextModule } from "primeng/inputtext";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { Toast } from "primeng/toast";
import { TableModule } from "primeng/table";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { ListBonsStatutComponent } from "./list-bons-statut.component";
import { BonAction, ListBonsActionsComponent } from "./list-bons-actions.component";
import { SpinnerComponent } from "app/shared/spinner/spinner.component";
import { IDelivery } from "app/shared/model/delevery.model";
import { ICommande } from "app/shared/model/commande.model";
import { IFournisseur } from "app/shared/model/fournisseur.model";
import { IOrderLine } from "app/shared/model/order-line.model";
import { ITEMS_PER_PAGE } from "app/shared/constants/pagination.constants";
import { DeliveryService, IDeliveryTotals } from "../../../../entities/commande/delevery/delivery.service";
import { FournisseurService } from "../../../../entities/fournisseur/fournisseur.service";
import { NotificationService } from "app/shared/services/notification.service";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import { EtiquetteComponent } from "../delivery/etiquette/etiquette.component";
import { CommandeReceivedComponent } from "../../feature/commande-received/commande-received.component";
import { ReceptionConcordanceComponent } from "../reception-concordance/reception-concordance.component";
import { CommandCommonService } from "app/entities/commande/command-common.service";
import { RetourBonService } from "app/entities/commande/retour_fournisseur/retour-bon.service";
import { RetourCompletModalComponent } from "./retour-complet-modal.component";
import { RetourWorkspaceComponent } from "../retour-workspace/retour-workspace.component";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

@Component({
  selector: "app-list-bons",
  templateUrl: "./list-bons.component.html",
  styleUrls: ["./list-bons.scss"],
  providers: [DatePipe],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TooltipModule,
    SelectModule,
    DatePicker,
    FloatLabel,
    InputTextModule,
    IconField,
    InputIcon,
    Toast,
    TableModule,
    SpinnerComponent,
    CommandeReceivedComponent,
    ReceptionConcordanceComponent,
    ListBonsActionsComponent,
    ListBonsStatutComponent,
    RetourWorkspaceComponent,
    DatePipe
  ]
})
export class AppListBonsComponent implements OnInit {
  // ── État liste ─────────────────────────────────────────────────────────────
  protected search = "";
  protected selectFournisseurId: number | null = null;
  protected dtStart: Date | null = null;
  protected dtEnd: Date | null = null;
  protected selectedStatut: string | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected deliveries: IDelivery[] = [];
  protected loading = false;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected totalItems = 0;

  // ── Modes master/detail ────────────────────────────────────────────────────
  readonly editingReceived = signal<ICommande | null>(null);
  readonly selectedClosed = signal<IDelivery | null>(null);
  readonly retourWorkspaceBon = signal<IDelivery | null>(null);
  private readonly datePipe = inject(DatePipe);

  protected readonly statutOptions = [
    { label: "Tous les bons", value: null },
    { label: "En attente de saisie", value: "RECEIVED" },
    { label: "Clôturé", value: "CLOSED" }
  ];

  // ── Totaux comptables de la période (backend) ──────────────────────────────
  readonly periodTotals = signal<IDeliveryTotals | null>(null);
  readonly countReceived = signal<number>(0);

  // ── Lignes bon clôturé (panneau consultation) ─────────────────────────────
  readonly closedOrderLines = computed<IOrderLine[]>(
    () => (this.selectedClosed()?.orderLines as IOrderLine[] | undefined) ?? []
  );

  // ── Row styling ────────────────────────────────────────────────────────────
  getBlRowClass(d: IDelivery): Record<string, boolean> {
    return {
      "row-received": d.orderStatus === "RECEIVED" || (d as any).statut === "RECEIVED"
    };
  }

  getClosedRowClass(line: IOrderLine): Record<string, boolean> {
    const costDiff = line.costAmount !== line.orderCostAmount;
    const priceDiff = !costDiff && line.regularUnitPrice !== line.orderUnitPrice;
    return {
      "pharma-row-danger": costDiff,
      "pharma-row-warning": priceDiff
    };
  }

  onRowClick(d: IDelivery): void {
    if (this.isReceived(d)) {
      this.onEditerReceivedDelivery(d);
    } else {
      this.onOuvrirClosed(d);
    }
  }

  private readonly entityService = inject(DeliveryService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly modalService = inject(NgbModal);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly retourBonService = inject(RetourBonService);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly downloadDocumentService = inject(BlobDownloadService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  ngOnInit(): void {
    this.fournisseurService
      .query({ page: 0, size: 999 })
      .subscribe((res: HttpResponse<IFournisseur[]>) => (this.fournisseurs = res.body ?? []));
    this.onSearch();

    toObservable(this.commandCommonService.pendingOpenDeliveryId, { injector: this.injector })
      .pipe(
        filter(id => id != null),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(pending => {
        this.commandCommonService.pendingOpenDeliveryId.set(null);
        this.spinner().show();
        this.entityService.find(pending!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: res => {
            this.spinner().hide();
            if (res.body) {
              this.editingReceived.set(res.body as unknown as ICommande);
              this.loadPage(0);
            }
          },
          error: () => {
            this.spinner().hide();
            this.notificationService.error("Erreur lors du chargement du bon de livraison", "Erreur");
          }
        });
      });
  }

  // ── Recherche / pagination ─────────────────────────────────────────────────

  onSearch(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.fetchDeliveries(page, this.itemsPerPage);
  }

  onFournisseurChange(event: any): void {
    this.selectFournisseurId = event.value ?? null;
    setTimeout(() => this.onSearch(), 50);
  }

  protected isReceived(delivery: IDelivery): boolean {
    return delivery.orderStatus === "RECEIVED" || (delivery as any).statut === "RECEIVED";
  }

  protected get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.itemsPerPage));
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.loadPage(p);
  }

  // ── Navigation master/detail ──────────────────────────────────────────────

  protected onEditerReceivedDelivery(delivery: IDelivery): void {
    this.spinner().show();
    this.entityService
      .find(delivery.commandeId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.spinner().hide();
          if (res.body) this.editingReceived.set(res.body as unknown as ICommande);
        },
        error: () => {
          this.spinner().hide();
          this.notificationService.error("Erreur lors du chargement du bon", "Erreur");
        }
      });
  }

  onRetourSaisie(): void {
    this.editingReceived.set(null);
    this.loadPage(this.page);
  }

  onCommandeChange(c: ICommande | null): void {
    if (c) {
      this.editingReceived.set(c);
    } else {
      this.onRetourSaisie();
    }
  }

  onOuvrirClosed(delivery: IDelivery): void {
    this.spinner().show();
    this.entityService
      .find(delivery.commandeId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.spinner().hide();
          if (res.body) this.selectedClosed.set(res.body);
        },
        error: () => {
          this.spinner().hide();
          this.notificationService.error("Erreur lors du chargement du bon", "Erreur");
        }
      });
  }

  onRetourConsultation(): void {
    this.selectedClosed.set(null);
  }

  // ── Actions ────────────────────────────────────────────────────────────────

  onBonMenuAction(action: BonAction, delivery: IDelivery): void {
    switch (action) {
      case "voirDetail":
        this.onRowClick(delivery);
        break;
      case "receive":
        this.onEditerReceivedDelivery(delivery);
        break;
      case "cancel":
        this.onAnnulerBon(delivery);
        break;
      case "exportPdf":
        this.exportPdf(delivery);
        break;
      case "printEtiquette":
        this.printEtiquette(delivery);
        break;
      case "retourComplet":
        this.onRetourComplet(delivery);
        break;
      case "retourParLigne":
        this.onRetourParLigne(delivery);
        break;
    }
  }

  protected onAnnulerBon(delivery: IDelivery): void {
    this.confirmDialog.onConfirm(
      () => {
        this.spinner().show();
        this.entityService.cancelReceived(delivery.commandeId!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.spinner().hide();
              this.notificationService.success("Bon annulé avec succès");
              this.loadPage(this.page);
            },
            error: () => {
              this.spinner().hide();
              this.notificationService.error("Erreur lors de l'annulation du bon");
            }
          });
      },
      "Annulation du bon",
      `Confirmer l'annulation du bon ${(delivery as any).receiptReference ?? (delivery as any).orderReference ?? ""} ?`
    );


  }

  printEtiquette(delivery: IDelivery): void {
    showCommonModal(
      this.modalService,
      EtiquetteComponent,
      {
        entity: delivery,
        header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${(delivery as any).receiptReference} ] `
      },
      () => {
      },
      "lg"
    );
  }

  onRetourParLigne(delivery: IDelivery): void {
    this.retourWorkspaceBon.set(delivery);
  }

  onRetourWorkspaceDone(): void {
    this.retourWorkspaceBon.set(null);
    this.loadPage(this.page);
  }

  onRetourWorkspaceCancelled(): void {
    this.retourWorkspaceBon.set(null);
  }

  protected onRetourComplet(delivery: IDelivery): void {
    const ref = this.modalService.open(RetourCompletModalComponent, {
      size: "lg",
      centered: true,
      backdrop: "static"
    });
    ref.componentInstance.delivery = delivery;
    ref.result.then(
      (result: { motifRetourId: number; commentaire: string }) => {
        const cmdId = delivery.commandeId?.id ?? delivery.id!;
        const cmdDate = delivery.commandeId?.orderDate ?? delivery.orderDate!;
        this.retourBonService.retourCompletCommande({
          commandeId: cmdId,
          commandeOrderDate: cmdDate,
          motifRetourId: result.motifRetourId,
          commentaire: result.commentaire
        }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => {
            this.notificationService.success("Retour complet créé avec succès");
            this.loadPage(this.page);
          },
          error: () => {
            this.notificationService.error("Erreur lors de la création du retour complet");
          }
        });
      },
      () => {
      }
    );
  }

  exportPdf(delivery: IDelivery): void {
    this.spinner().show();
    this.entityService.exportToPdf(delivery.commandeId).subscribe({
      next: blob => {
        this.spinner().hide();
        this.downloadDocumentService.downloadPdf(blob, `Bon_Livraison_${(delivery as any).receiptReference}`);

      },
      error: () => this.spinner().hide()
    });
  }

  // ── Chargement des données ────────────────────────────────────────────────

  private fetchDeliveries(page: number, size: number): void {
    this.loading = true;
    const query: any = { page, size, search: this.search };
    if (this.selectedStatut) {
      query.statuts = [this.selectedStatut];
    } else {
      query.statuts = ["RECEIVED", "CLOSED"];
    }
    if (this.selectFournisseurId) query.fournisseurId = this.selectFournisseurId;
    const applyDates = this.selectedStatut === "CLOSED";
    if (applyDates) {
      if (this.dtStart) query.fromDate = this.datePipe.transform(this.dtStart, "yyyy-MM-dd");
      if (this.dtEnd) query.toDate = this.datePipe.transform(this.dtEnd, "yyyy-MM-dd");
    }
    forkJoin({
      list: this.entityService.query(query),
      totals: this.entityService.fetchTotals(query)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ list, totals }) => {
          this.onSuccess(list.body, list.headers, page);
          this.periodTotals.set(totals.body);
          this.countReceived.set(
            (list.body ?? []).filter(d => d.orderStatus === "RECEIVED" || (d as any).statut === "RECEIVED").length
          );
        },
        error: () => (this.loading = false)
      });
  }

  private onSuccess(data: IDelivery[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.deliveries = data ?? [];
    this.loading = false;
  }
}
