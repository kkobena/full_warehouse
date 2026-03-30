import { Component, computed, DestroyRef, inject, OnInit, signal, viewChild } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { forkJoin } from "rxjs";
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
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import {
  AllCommunityModule,
  CellClickedEvent,
  ClientSideRowModelModule,
  ColDef,
  GetRowIdFunc,
  ModuleRegistry,
  RowClassRules,
  themeAlpine
} from "ag-grid-community";
import { AgGridAngular } from "ag-grid-angular";
import { SpinnerComponent } from "app/shared/spinner/spinner.component";
import { IDelivery } from "app/shared/model/delevery.model";
import { ICommande } from "app/shared/model/commande.model";
import { IFournisseur } from "app/shared/model/fournisseur.model";
import { IOrderLine } from "app/shared/model/order-line.model";
import { ITEMS_PER_PAGE } from "app/shared/constants/pagination.constants";
import { DeliveryService, IDeliveryTotals } from "../../../../entities/commande/delevery/delivery.service";
import { FournisseurService } from "../../../../entities/fournisseur/fournisseur.service";
import { TauriPrinterService } from "app/shared/services/tauri-printer.service";
import { NotificationService } from "app/shared/services/notification.service";
import { handleBlobForTauri } from "app/shared/util/tauri-util";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import { EtiquetteComponent } from "../delivery/etiquette/etiquette.component";
import { CommandeReceivedComponent } from "../../feature/commande-received/commande-received.component";
import { ReceptionConcordanceComponent } from "../reception-concordance/reception-concordance.component";

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

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
    SpinnerComponent,
    CommandeReceivedComponent,
    ReceptionConcordanceComponent,
    AgGridAngular,
    DatePipe
  ]
})
export class AppListBonsComponent implements OnInit {
  // ── État liste ─────────────────────────────────────────────────────────────
  protected search = "";
  protected selectFournisseurId: number | null = null;
  // Pas de date par défaut : les RECEIVED apparaissent toujours, les CLOSED
  // sont filtrés par date uniquement si l'utilisateur en saisit une.
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
  private readonly datePipe = inject(DatePipe);
  protected readonly statutOptions = [
    { label: "Tous les bons", value: null },
    { label: "En attente de saisie", value: "RECEIVED" },
    { label: "Clôturé", value: "CLOSED" }
  ];

  // ── AG Grid partagé ────────────────────────────────────────────────────────
  protected readonly theme = themeAlpine;

  protected readonly defaultColDef: ColDef = {
    resizable: true,
    sortable: false,
    suppressHeaderMenuButton: true
  };

  // ── AG Grid — liste BL principale ─────────────────────────────────────────
  protected readonly blGetRowId: GetRowIdFunc<IDelivery> = p => String(p.data.id);

  protected readonly blRowClassRules: RowClassRules<IDelivery> = {
    "row-received": p => p.data?.orderStatus === "RECEIVED" || (p.data as any)?.statut === "RECEIVED"
  };

  protected readonly blColDefs: ColDef<IDelivery>[] = [
    {
      field: "receiptDate",
      headerName: "Date BL",
      width: 105,
      sortable: true,
      valueFormatter: p => p.value ? new Date(p.value as string).toLocaleDateString("fr-FR") : "—"
    },
    {
      field: "fournisseurLibelle",
      headerName: "Fournisseur",
      flex: 1,
      minWidth: 140
    },
    {
      colId: "reference",
      headerName: "Référence",
      width: 145,
      valueGetter: p => (p.data as any)?.receiptReference ?? p.data?.orderReference ?? "—",
      cellStyle: { fontFamily: "monospace", fontSize: "12px" }
    },
    {
      field: "itemSize",
      headerName: "Art.",
      width: 60,
      type: "numericColumn"
    },
    {
      field: "grossAmount",
      headerName: "Montant HT",
      width: 125,
      type: "numericColumn",
      valueFormatter: p => p.value != null ? Number(p.value).toLocaleString("fr-FR") : "—"
    },
    {
      field: "receiptAmount",
      headerName: "Montant TTC",
      width: 130,
      type: "numericColumn",
      valueFormatter: p => p.value != null ? Number(p.value).toLocaleString("fr-FR") : "—"
    },
    {
      colId: "statut",
      headerName: "Statut",
      width: 160,
      cellRenderer: (p: any) => {
        const status = p.data?.orderStatus ?? p.data?.statut;
        if (status === "RECEIVED") {
          return `<span style="display:inline-flex;align-items:center;gap:4px;padding:2px 8px;border-radius:10px;font-size:0.72rem;font-weight:700;background:#fef3c7;color:#92400e">
            <i class="pi pi-inbox"></i> En attente de saisie
          </span>`;
        }
        return `<span style="display:inline-flex;align-items:center;gap:4px;padding:2px 8px;border-radius:10px;font-size:0.72rem;font-weight:700;background:#d1fae5;color:#065f46">
          <i class="pi pi-check-circle"></i> Clôturé
        </span>`;
      }
    },
    {
      colId: "actions",
      headerName: "",
      width: 90,
      sortable: false,
      cellRenderer: (p: any) => {
        if (!p.data) return "";
        const status = p.data.orderStatus ?? p.data.statut;
        const receiveBtn = status === "RECEIVED"
          ? `<button data-action="receive" title="Saisir la réception" style="background:rgba(59,130,246,0.1);color:#1d4ed8;border:none;border-radius:4px;padding:3px 6px;cursor:pointer;font-size:12px"><i class="pi pi-inbox"></i></button>`
          : "";
        const etiBtn = status === "CLOSED"
          ? `<button data-action="etiquette" title="Étiquettes" style="background:none;border:none;cursor:pointer;color:#6c757d;font-size:12px;padding:2px 4px"><i class="pi pi-print"></i></button>`
          : "";
        return `<span style="display:flex;align-items:center;gap:2px">
          ${receiveBtn}
          <button data-action="pdf" title="Imprimer BL" style="background:none;border:none;cursor:pointer;color:#6c757d;font-size:12px;padding:2px 4px"><i class="pi pi-file-pdf"></i></button>
          ${etiBtn}
        </span>`;
      }
    }
  ];

  // ── Totaux comptables de la période (backend) ──────────────────────────────
  readonly periodTotals = signal<IDeliveryTotals | null>(null);
  readonly countReceived = signal<number>(0);

  // ── AG Grid (panneau consultation bon clôturé) ─────────────────────────────
  protected readonly rowClassRules: RowClassRules<IOrderLine> = {
    "pharma-row-danger": p => !!p.data && p.data.costAmount !== p.data.orderCostAmount,
    "pharma-row-warning": p =>
      !!p.data &&
      p.data.costAmount === p.data.orderCostAmount &&
      p.data.regularUnitPrice !== p.data.orderUnitPrice
  };

  protected readonly getRowId: GetRowIdFunc<IOrderLine> = p => String(p.data.id);

  protected readonly closedColDefs: ColDef<IOrderLine>[] = [
    {
      field: "produitCip",
      headerName: "Code",
      width: 110,
      cellStyle: { fontFamily: "monospace", fontSize: "12px" }
    },
    {
      field: "produitLibelle",
      headerName: "Libellé",
      flex: 2,
      minWidth: 140
    },
    {
      field: "initStock",
      headerName: "Stk.Init",
      width: 90,
      type: "numericColumn"
    },
    {
      field: "afterStock",
      headerName: "Stk.Final",
      width: 90,
      type: "numericColumn"
    },
    {
      field: "quantityReceived",
      headerName: "Qté reçue",
      width: 100,
      type: "numericColumn"
    },
    {
      field: "orderCostAmount",
      headerName: "P.Achat",
      width: 110,
      type: "numericColumn",
      valueFormatter: p => (p.value != null ? Number(p.value).toLocaleString("fr-FR") : "—")
    },
    {
      field: "orderUnitPrice",
      headerName: "P.Vente",
      width: 110,
      type: "numericColumn",
      valueFormatter: p => (p.value != null ? Number(p.value).toLocaleString("fr-FR") : "—")
    }
  ];

  readonly closedOrderLines = computed<IOrderLine[]>(
    () => (this.selectedClosed()?.orderLines as IOrderLine[] | undefined) ?? []
  );

  private readonly entityService = inject(DeliveryService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly modalService = inject(NgbModal);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");

  ngOnInit(): void {
    this.fournisseurService
      .query({ page: 0, size: 999 })
      .subscribe((res: HttpResponse<IFournisseur[]>) => (this.fournisseurs = res.body ?? []));
    this.onSearch();
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

  onBLCellClicked(event: CellClickedEvent<IDelivery>): void {
    if (!event.data) return;
    const action = (event.event?.target as HTMLElement)?.closest("[data-action]")?.getAttribute("data-action");
    if (action === "receive") {
      this.onEditerReceivedDelivery(event.data);
    } else if (action === "pdf") {
      this.exportPdf(event.data, event.event as MouseEvent);
    } else if (action === "etiquette") {
      this.printEtiquette(event.data, event.event as MouseEvent);
    } else if (!action) {
      // clic sur la ligne hors bouton → navigation
      if (this.isReceived(event.data)) {
        this.onEditerReceivedDelivery(event.data);
      } else {
        this.onOuvrirClosed(event.data);
      }
    }
  }

  private onEditerReceivedDelivery(delivery: IDelivery): void {
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

  printEtiquette(delivery: IDelivery, event: Event): void {
    event.stopPropagation();
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

  exportPdf(delivery: IDelivery, event: Event): void {
    event.stopPropagation();
    this.spinner().show();
    this.entityService.exportToPdf(delivery.commandeId).subscribe({
      next: blob => {
        this.spinner().hide();
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, "bon-livraison");
        } else {
          window.open(URL.createObjectURL(blob));
        }
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
    // Le filtre de date s'applique uniquement aux bons CLOSED (historique).
    // Les bons RECEIVED (en attente) sont toujours visibles sans contrainte de date.
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
