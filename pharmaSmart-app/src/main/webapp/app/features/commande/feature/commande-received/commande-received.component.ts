import { Component, DestroyRef, ElementRef, inject, input, OnInit, output, signal, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { HttpResponse } from "@angular/common/http";
import { ICommande } from "app/shared/model/commande.model";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { IOrderLine } from "../../../../shared/model/order-line.model";
import { ILot } from "../../../../shared/model/lot.model";
import { MenuItem } from "primeng/api";
import { ErrorService } from "../../../../shared/error.service";
import { saveAs } from "file-saver";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ButtonModule } from "primeng/button";
import { RippleModule } from "primeng/ripple";
import { InputTextModule } from "primeng/inputtext";
import { TagModule } from "primeng/tag";
import { SplitButtonModule } from "primeng/splitbutton";
import { ToolbarModule } from "primeng/toolbar";
import { TooltipModule } from "primeng/tooltip";
import { SelectModule } from "primeng/select";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { FloatLabel } from "primeng/floatlabel";
import { Toast } from "primeng/toast";
import { filter, finalize, map, timeout } from "rxjs/operators";
import { TimeoutError } from "rxjs";
import { SORT } from "../../../../shared/util/command-item-sort";
import { CommandeService, IPutawayPreviewItem } from "../../../../entities/commande/commande.service";
import { DeliveryService } from "../../../../entities/commande/delevery/delivery.service";
import { ListLotComponent } from "../../ui/lot/list/list-lot.component";
import { FormLotComponent } from "../../ui/lot/form/form-lot.component";
import { EditProduitComponent } from "../../ui/delivery/edit-produit/edit-produit.component";
import { EtiquetteComponent } from "../../ui/delivery/etiquette/etiquette.component";
import { PutawayModalComponent } from "../../ui/delivery/putaway-modal/putaway-modal.component";
import { FileResponseModalComponent } from "../../ui/file-response-modal/file-response-modal.component";
import { CommandeResponseDialogComponent } from "../../ui/commande-response-dialog/commande-response-dialog.component";
import { IResponseCommande } from "../../../../shared/model/response-commande.model";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import { SpinnerComponent } from "../../../../shared/spinner/spinner.component";
import { ReceptionConcordanceComponent } from "../../ui/reception-concordance/reception-concordance.component";
import { PrixHistoriqueComponent } from "../../ui/prix-historique/prix-historique.component";
import { CommandeId } from "../../../../shared/model/abstract-commande.model";
import { Params } from "../../../../shared/model/enumerations/params.model";
import { ConfigurationService } from "../../../../shared/configuration.service";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";
import { handleBlobForTauri } from "../../../../shared/util/tauri-util";
import { IStockEntryResult } from "../../../../shared/model/stock-entry-result.model";
import { IReceptionScanResult } from "../../../../shared/model/reception-scan-result.model";
import { ScanEvent, ScanOrchestratorService } from "../../../../shared/scanner";
import { ReceptionScannerService } from "./reception-scanner.service";
import { RetourDepuisReceptionComponent } from "../../ui/retour-depuis-reception/retour-depuis-reception.component";
import { ReconciliationFactureComponent } from "../../ui/reconciliation-facture/reconciliation-facture.component";
import { ReceptionHelpComponent } from "../../ui/reception-help/reception-help.component";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ScanAudioFeedbackService } from "../../../../shared/services/scan-audio-feedback.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {
  AllCommunityModule,
  CellClickedEvent,
  CellValueChangedEvent,
  ClientSideRowModelModule,
  ColDef,
  GetRowIdFunc,
  GridApi,
  GridReadyEvent,
  ModuleRegistry,
  RowClassRules,
  themeAlpine
} from "ag-grid-community";
import { AgGridAngular } from "ag-grid-angular";
import { CommandeReceivedActionsComponent } from "./commande-received-actions.component";
import { CommandeReceivedStatutComponent } from "./commande-received-statut.component";
import { LotExpandCellComponent } from "../../ui/lot/inline/lot-expand-cell.component";
import { LotInlineEditorComponent } from "../../ui/lot/inline/lot-inline-editor.component";
import { ReceptionSequentialComponent } from "./sequential/reception-sequential.component";
import { ReceptionFinalizeModalComponent } from "./sequential/reception-finalize-modal.component";
import { IConfiguration } from "../../../../shared/model/configuration.model";
import { IPoste } from "../../../../shared/model/poste.model";

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: "app-commande-received",
  templateUrl: "./commande-received.component.html",
  styleUrls: ["./commande-received.component.scss"],
  providers: [ReceptionScannerService, ScanOrchestratorService],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    TagModule,
    SplitButtonModule,
    ToolbarModule,
    TooltipModule,
    SelectModule,
    IconField,
    InputIcon,
    FloatLabel,
    Toast,
    SpinnerComponent,
    ReceptionConcordanceComponent,
    AgGridAngular,
    ReceptionSequentialComponent
  ]
})
export class CommandeReceivedComponent implements OnInit {
  commande = input.required<ICommande>();
  commandeChange = output<ICommande | null>();
  retour = output<void>();

  protected orderLines: IOrderLine[] = [];
  protected displayRows: any[] = [];
  protected search?: string;
  protected tris = "UPDATE";
  protected selectedFilter = "ALL";
  showLotBtn = input(false);
  protected showLeftPanel = true;
  private conformeSansLot = false;

  // ── Vue séquentielle / grille ─────────────────────────────────────────────
  protected readonly viewMode = signal<"grid" | "sequential">(
    (localStorage.getItem("reception-view-mode") as "grid" | "sequential") ?? "sequential"
  );
  protected putWayMode = signal<string>("MANUAL");

  protected setViewMode(mode: "grid" | "sequential"): void {
    this.viewMode.set(mode);
    localStorage.setItem("reception-view-mode", mode);
  }

  protected currentCommande!: ICommande;
  protected filtres: any[];
  protected exportbuttons: MenuItem[];
  protected readonly sorts = SORT;

  // ── AG Grid ───────────────────────────────────────────────────────────────
  protected readonly theme = themeAlpine;
  private gridApi: GridApi<any> | null = null;

  protected readonly defaultColDef: ColDef<any> = {
    resizable: true,
    sortable: false,
    suppressHeaderMenuButton: true
  };

  protected readonly rowClassRules: RowClassRules<any> = {
    "pharma-row-danger": p => !p.data?.__type && !!p.data && p.data.costAmount !== p.data.orderCostAmount,
    "pharma-row-warning": p =>
      !p.data?.__type && !!p.data &&
      p.data.costAmount === p.data.orderCostAmount &&
      p.data.regularUnitPrice !== p.data.orderUnitPrice,
    "pharma-row-provisional": p => !p.data?.__type && !!p.data?.provisionalCode,
    "cr-lot-editor-row": p => p.data?.__type === "lot-editor"
  };

  protected readonly getRowId: GetRowIdFunc<any> = p => {
    if (p.data?.__type === "lot-editor") return `lot-editor-${p.data.__line.id}`;
    return String(p.data.id);
  };
  protected columnDefs: ColDef<any>[] = [];
  protected readonly gridContext: { componentParent: CommandeReceivedComponent } = { componentParent: this };

  // ── Lot inline expand ──────────────────────────────────────────────────────
  private expandedLineIds = new Set<number>();
  protected readonly isFullWidthRow = (params: any): boolean => params.rowNode.data?.__type === "lot-editor";
  protected readonly fullWidthCellRenderer = LotInlineEditorComponent;
  protected readonly getRowHeight = (params: any): number | undefined => {
    if (params.data?.__type !== "lot-editor") return undefined;
    const line = params.data.__line as IOrderLine;
    const lots = (line.lots ?? []).length;
    return Math.max(160, 48 + (lots + 1) * 28 + 44);
  };

  // ── Scan réception ────────────────────────────────────────────────────────
  protected scanValue = "";
  protected readonly lastScanResult = signal<IReceptionScanResult | null>(null);
  /** Pré-remplissage lot transmis au composant séquentiel après un scan DataMatrix sans lot auto-créé. */
  protected readonly scanLotPrefill = signal<{ numLot: string; expiry: string } | null>(null);
  private scanFeedbackTimer: ReturnType<typeof setTimeout> | null = null;
  /** Listener clavier dédié aux raccourcis (Ctrl+G, F5, F11, Alt+H) — toujours actif. */
  private shortcutListener: ((e: KeyboardEvent) => void) | null = null;
  /** Listener clavier dédié au buffer scan HID — installé seulement quand l'orchestrateur est en mode HID. */
  private scanKeydownListener: ((e: KeyboardEvent) => void) | null = null;
  /** Dernier code scanné en réception + timestamp — anti-rebond / double trigger. */
  private lastReceptionScan: { code: string; at: number } | null = null;
  private static readonly DEDUP_WINDOW_MS = 400;
  private static readonly SCAN_RECEPTION_TIMEOUT_MS = 3000;
  /** IDs des lignes dont le CIP a été mis à jour pendant cette réception. */
  private readonly updatedCipLineIds = new Set<number>();

  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  readonly scanInputRef = viewChild<ElementRef>("scanInputRef");
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly audioFeedback = inject(ScanAudioFeedbackService);
  private readonly commandeService = inject(CommandeService);
  private readonly deliveryService = inject(DeliveryService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);
  /** Service scanner isolé à ce composant — ne partage PAS le buffer avec le module vente. */
  private readonly receptionScanner = inject(ReceptionScannerService);
  /** Orchestrateur unifié HID + Tauri SERIAL. */
  private readonly scanOrchestrator = inject(ScanOrchestratorService);
  /** Mode scanner actif (badge UI) — délégué à l'orchestrateur. */
  protected readonly scannerMode = this.scanOrchestrator.scannerMode;
  /** Bouton reconnexion CDC visible — délégué à l'orchestrateur. */
  protected readonly canReconnectScanner = this.scanOrchestrator.canReconnect;

  constructor() {
    this.filtres = [
      { label: "Prix d'achat differents", value: "NOT_EQUAL" },
      { label: "Code cip  à mettre à jour", value: "PROVISOL_CIP" },
      { label: "Tous", value: "ALL" }
    ];
    this.exportbuttons = [
      {
        label: "PDF",
        icon: "pi pi-file-pdf",
        command: () => this.exportPdf()
      },
      {
        label: "CSV",
        icon: "pi pi-file-excel",
        command: () => this.exportCSV()
      }
    ];

    // HID source : codes 'complete' du buffer scanner réception.
    const hidSource$ = this.receptionScanner.onScanEvent$.pipe(
      filter((e: ScanEvent) => e.type === "complete" && !!e.code),
      map((e: ScanEvent) => e.code!)
    );
    this.scanOrchestrator.configure({
      eventName: "scan-reception",
      hidSource$,
      hidEnable: () => this.installScanKeydownListener(),
      hidDisable: () => this.removeScanKeydownListener()
    });
  }

  ngOnInit(): void {

    this.initPutAway();
    this.currentCommande = this.commande();
    this.orderLines = (this.currentCommande.orderLines as IOrderLine[]) ?? [];

    this.columnDefs = this.buildColumnDefs();
    this.refreshDisplayRows();
    this.setupBarcodeScanner();
  }

  protected previousState(): void {
    this.retour.emit();
  }

  protected openHelp(): void {
    this.modalService.open(ReceptionHelpComponent, { size: "lg", centered: true, scrollable: true });
  }

  protected toggleLeftPanel(): void {
    this.showLeftPanel = !this.showLeftPanel;
  }

  protected onSequentialLineChanged(updatedLine: IOrderLine): void {
    // effacer le prefill DataMatrix dès que la ligne est validée (consommé)
    this.scanLotPrefill.set(null);
    const idx = this.orderLines.findIndex(l => l.id === updatedLine.id);
    if (idx !== -1) {
      this.orderLines = [
        ...this.orderLines.slice(0, idx),
        updatedLine,
        ...this.orderLines.slice(idx + 1)
      ];
      this.refreshDisplayRows();
    }
  }

  protected onSequentialAllDone(): void {
    const ref = this.modalService.open(ReceptionFinalizeModalComponent, {
      size: "md",
      backdrop: "static",
      centered: true
    });
    const instance = ref.componentInstance as ReceptionFinalizeModalComponent;
    instance.orderLines = this.orderLines;
    instance.commandeRef = this.currentCommande.orderReference ?? this.currentCommande.receiptReference ?? "";
    instance.fournisseurLibelle = this.currentCommande.fournisseur?.libelle ?? "";
    //  Lignes dont le CIP a été mis à jour pendant cette réception
    instance.updatedCipLines = this.orderLines.filter(l => l.id && this.updatedCipLineIds.has(l.id));

    ref.result.then(
      result => {
        if (result === "finalize") this.finalizeSansConfirmModal();
      },
      () => { /* dismissed — continuer la saisie */
      }
    );
  }

  protected onCipUpdated(lineId: number): void {
    this.updatedCipLineIds.add(lineId);
  }

  protected onFilterCommandeLines(): void {
    const query = {
      commandeId: this.currentCommande.id,
      search: this.search,
      filterCommaneEnCours: this.selectedFilter,
      orderBy: this.tris
    };
    this.commandeService.filterCommandeLines(query).subscribe(res => {
      this.orderLines = res.body!;
      this.expandedLineIds.clear();
      this.refreshDisplayRows();
    });
  }

  // ── AG Grid events ────────────────────────────────────────────────────────

  protected onGridReady(event: GridReadyEvent<any>): void {
    this.gridApi = event.api;
  }

  protected onCellValueChanged(event: CellValueChangedEvent<IOrderLine>): void {
    const line = event.data;
    const field = event.colDef.field;

    if (field === "quantityReceivedTmp") {
      const qty = Number(event.newValue);
      if (qty >= 0) {
        line.quantityReceived = qty;
        line.quantityReceivedTmp = qty;
        this.orderLines = [...this.orderLines];
        this.gridApi?.refreshCells({
          rowNodes: [event.node],
          columns: ["afterStock", "statut", "quantityReceivedTmp"],
          force: true
        });
        this.deliveryService.updateQuantityReceived(line).subscribe({
          error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
        });
        // Auto-ouvrir l'éditeur de lots si le produit le nécessite et que la ligne n'est pas déjà expandée
        if (this.showLotBtn() && line.gestionLot !== false && qty > 0 && !this.expandedLineIds.has(line.id!)) {
          setTimeout(() => this.onToggleLotExpand(line), 50);
        }
      }
    } else if (field === "freeQty") {
      const qty = Number(event.newValue);
      if (qty >= 0) {
        line.freeQty = qty;
        this.orderLines = [...this.orderLines];
        this.gridApi?.refreshCells({ rowNodes: [event.node], columns: ["afterStock"], force: true });
        this.commandeService.updateQuantityUG(line).subscribe({
          next: () => this.refreshCommande(),
          error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
        });
      }
    } else if (field === "produitCip") {
      const newCip = (event.newValue as string)?.trim();
      const oldCip = (event.oldValue as string)?.trim();
      if (newCip && newCip !== "") {
        if (!line.provisionalCode && newCip !== oldCip) {
          // Substitution détectée : CIP reçu ≠ CIP commandé sur un produit non provisoire
          this.confirmDialog.onConfirm(
            () => {
              line.produitCip = newCip;
              this.commandeService.updateCip(line).subscribe(() => {
                if (line.id) this.updatedCipLineIds.add(line.id);
                this.refreshCommande();
              });
            },
            "Substitution détectée",
            `Le CIP reçu (${newCip}) diffère du CIP commandé (${oldCip}).\nAccepter la substitution et mettre à jour le produit ?`,
            "pi pi-exclamation-triangle",
            () => {
              // Annuler : remettre l'ancien CIP dans la cellule
              line.produitCip = oldCip;
              this.gridApi?.refreshCells({ rowNodes: [event.node], columns: ["produitCip"], force: true });
            }
          );
        } else {
          line.produitCip = newCip;
          this.commandeService.updateCip(line).subscribe(() => {
            if (line.id) this.updatedCipLineIds.add(line.id);
            this.refreshCommande();
          });
        }
      }
    }
  }

  protected onCellClicked(_event: CellClickedEvent<IOrderLine>): void {
    // Actions handled by CommandeReceivedActionsComponent renderer
  }


  protected onToutValider(): void {
    const allLines = this.currentCommande.orderLines as IOrderLine[] ?? [];
    const linesToUpdate = allLines.filter(l => !l.updated);
    if (linesToUpdate.length === 0) {
      this.notificationService.info("Aucune ligne en attente de validation", "Tout valider");
      return;
    }
    this.onConfirmToutValider(allLines, linesToUpdate);
  }

  private onConfirmToutValider(allLines: IOrderLine[], linesToUpdate: IOrderLine[]): void {
    this.confirmDialog.onConfirm(
      () => this.doToutValider(allLines, linesToUpdate),
      "Tout valider",
      `Confirmer la réception conforme pour les ${linesToUpdate.length} ligne(s) non saisies ?\n\nChaque ligne sera considérée comme entièrement livrée selon la quantité commandée.\nAucun écart ne sera signalé pour ces lignes.`
    );

  }

  private doToutValider(allLines: IOrderLine[], linesToUpdate: IOrderLine[]): void {
    for (const l of allLines) {
      l.quantityReceived = l.quantityRequested ?? 0;
      l.quantityReceivedTmp = l.quantityRequested ?? 0;
    }
    this.selectedFilter = "ALL";
    this.orderLines = [...allLines];
    this.refreshDisplayRows();
    this.deliveryService.batchUpdateQuantityReceived(linesToUpdate).subscribe({
      next: () => this.refreshCommande(),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
    });

  }

  protected onDeleteOrderLine(orderLine: IOrderLine): void {
    if (this.currentCommande) {
      this.commandeService.deleteOrderLineById(orderLine.orderLineId).subscribe(() => this.refreshCommande());
    }
  }

  protected confirmDeleteItem(item: IOrderLine): void {
    this.confirmDialog.onConfirm(
      () => this.onDeleteOrderLine(item),
      "Suppression",
      "Voullez-vous supprimer de la commande ce produit ?"
    );
  }

  protected onConfirmFinalize(): void {
    this.executeFinalizationFlow(true);
  }

  protected finalizeSansConfirmModal(): void {
    this.executeFinalizationFlow(false);
  }

  private canValidate(): boolean {
    const allLines = this.currentCommande.orderLines as IOrderLine[] ?? [];
    const linesToUpdate = allLines.filter(l => !l.updated);
    return linesToUpdate.length === 0;
  }

  private countLignesNonSaisies(): number {
    const allLines = this.currentCommande.orderLines as IOrderLine[] ?? [];
    return allLines.filter(l => !l.updated).length;
  }

  // requireConfirm = true → dialog "Voulez-vous faire l'entrée en stock ?" avant la mise en stock
  // requireConfirm = false → exécution directe (le modal de finalisation a déjà obtenu la confirmation)
  private executeFinalizationFlow(requireConfirm: boolean): void {
    if (this.showLotBtn() && !this.canValidate()) {
      this.notificationService.warning("Des lignes de réception sont incomplètes. Veuillez saisir les quantités reçues avant de valider l'entrée en stock.", "Réception incomplète");
      return;
    }
    // Sans gestion de lot : les lignes non saisies sont considérées conformes (quantityReceived = quantityRequested côté backend)
    this.conformeSansLot = !this.showLotBtn();

    this.commandeService.checkPriceVariation(this.currentCommande.commandeId).subscribe({
      next: res => {
        const lignesAnomalie = res.body ?? [];
        if (lignesAnomalie.length > 0) {
          const detail = lignesAnomalie
            .map(l => `• ${l.produitLibelle} : commandé ${l.orderCostAmount} → actuel ${l.costAmount}`)
            .join("\n");
          this.confirmDialog.onConfirm(
            () => this.confirmFinalizeApresAlertesPrix(),
            "Variation de prix détectée",
            `${lignesAnomalie.length} ligne(s) ont un écart de prix dépassant le seuil configuré :\n${detail}\n\nVoulez-vous continuer malgré ces écarts ?`
          );
        } else {
          this.confirmFinalisation(requireConfirm);
        }
      },
      error: () => this.confirmFinalisation(true)
    });
  }

  private confirmFinalisation(requireConfirm: boolean): void {
    const lignesNonSaisies = this.countLignesNonSaisies();
    if (lignesNonSaisies > 0) {
      this.confirmDialog.onConfirm(
        () => this.checkPutawayAndFinalize(),
        "Entrée en stock sans saisie complète",
        this.buildConformeSansLotMessage(lignesNonSaisies)
      );
      return;
    }
    if (requireConfirm) {
      this.confirmDialog.onConfirm(
        () => this.checkPutawayAndFinalize(),
        "Finalisation de la commande",
        "Voullez-vous faire l'entrée en stock ?"
      );
    } else {
      this.checkPutawayAndFinalize();
    }
  }

  private confirmFinalizeApresAlertesPrix(): void {
    const lignesNonSaisies = this.countLignesNonSaisies();
    if (lignesNonSaisies > 0) {
      this.confirmDialog.onConfirm(
        () => this.checkPutawayAndFinalize(),
        "Entrée en stock sans saisie complète",
        this.buildConformeSansLotMessage(lignesNonSaisies, true)
      );
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.checkPutawayAndFinalize(),
      "Confirmation finale",
      "Confirmer l'entrée en stock malgré les écarts de prix ?"
    );
  }

  private buildConformeSansLotMessage(lignesNonSaisies: number, ecartsPrixAcceptes = false): string {
    const total = (this.currentCommande.orderLines ?? []).length;
    const prefixe = ecartsPrixAcceptes
      ? "Ecarts de prix déjà acceptés."
      : "";
    return (
      `${prefixe}\n\n` +
      `${lignesNonSaisies} ligne(s) sur ${total} n'ont pas été saisies. ` +
      `Elles seront considérées comme intégralement reçues : la quantité reçue sera égale à la quantité commandée et le stock sera mis à jour en conséquence.\n\n` +
      `Confirmer la finalisation et l'entrée en stock ?`
    );
  }

  private checkPutawayAndFinalize(): void {
    if (this.putWayMode() !== "MANUAL") {
      this.onFinalize(false);
      return;
    }
    this.commandeService.getPutawayPreview(this.currentCommande.commandeId).subscribe({
      next: (items: IPutawayPreviewItem[]) => {
        if (!items || items.length === 0) {
          this.onFinalize(false);
          return;
        }
        showCommonModal(
          this.modalService,
          PutawayModalComponent,
          { items, header: `Répartition rayon → réserve (${items.length} produit(s))` },
          (doTransfer: boolean) => this.onFinalize(doTransfer),
          "lg",
          null,
          () => this.onFinalize(false)
        );
      },
      error: () => this.onFinalize(false)
    });
  }

  protected onImporterReponseCommande(): void {
    showCommonModal(
      this.modalService,
      FileResponseModalComponent,
      { commandeSelected: this.currentCommande, header: "IMPORTER RÉPONSE" },
      (responseCommande: IResponseCommande) => {
        if (responseCommande) {
          this.refreshCommande();
          this.openImporterReponseCommandeDialog(responseCommande);
        }
      },
      "lg"
    );
  }

  //Lien vers le module de rapprochement facture fournisseur
  protected onRapprocher(): void {
    const ref = this.modalService.open(ReconciliationFactureComponent, { size: "lg", centered: true });
    (ref.componentInstance as ReconciliationFactureComponent).commande = this.currentCommande;
  }

  protected onRetourFournisseur(): void {
    showCommonModal(
      this.modalService,
      RetourDepuisReceptionComponent,
      {
        commande: this.currentCommande,
        orderLines: this.orderLines,
        header: `Retour fournisseur — ${this.currentCommande.receiptReference ?? this.currentCommande.orderReference ?? ""}`
      },
      () => this.refreshCommande(),
      "xl"
    );
  }

  protected onAddLot(orderLine: IOrderLine): void {
    const qty = orderLine.quantityReceived || orderLine.quantityRequested;
    if (qty > 1 || (orderLine.lots?.length ?? 0) > 0) {
      showCommonModal(
        this.modalService,
        ListLotComponent,
        {
          deliveryItem: orderLine,
          header: `GESTION DE LOTS DE LA LIGNE ${orderLine.produitLibelle} [${orderLine.produitCip}]`
        },
        null,
        "lg"
      );
    } else {
      showCommonModal(
        this.modalService,
        FormLotComponent,
        { entity: null, deliveryItem: orderLine, header: "Ajout de lot" },
        null,
        "lg"
      );
    }
  }

  protected editLigneInfos(orderLine: IOrderLine): void {
    showCommonModal(
      this.modalService,
      EditProduitComponent,
      {
        deliveryItem: orderLine,
        delivery: this.currentCommande,
        header: `EDITION DU PRODUIT ${orderLine.produitLibelle} [${orderLine.produitCip}]`
      },
      null,
      "xl"
    );
  }

  protected onShowPriceHistory(orderLine: IOrderLine): void {
    if (!orderLine.fournisseurProduitId) return;
    showCommonModal(
      this.modalService,
      PrixHistoriqueComponent,
      {
        fournisseurProduitId: orderLine.fournisseurProduitId,
        produitLibelle: orderLine.produitLibelle,
        header: `Historique des prix — ${orderLine.produitLibelle} [${orderLine.produitCip}]`
      },
      null,
      "xl"
    );
  }


  // ── Lot inline editor ─────────────────────────────────────────────────────

  onToggleLotExpand(line: IOrderLine): void {
    if (!line?.id) return;
    if (this.expandedLineIds.has(line.id)) {
      this.expandedLineIds.delete(line.id);
      (line as any).__expanded = false;
    } else {
      this.expandedLineIds.add(line.id);
      (line as any).__expanded = true;
    }
    this.refreshDisplayRows();
  }

  onCollapseRow(line: IOrderLine | null): void {
    if (!line?.id) return;
    this.expandedLineIds.delete(line.id);
    const found = this.orderLines.find(l => l.id === line.id);
    if (found) (found as any).__expanded = false;
    this.refreshDisplayRows();
  }

  onLotSaved(line: IOrderLine, lots: ILot[]): void {
    const found = this.orderLines.find(l => l.id === line.id);
    if (found) found.lots = lots;
    const rowNode = this.gridApi?.getRowNode(String(line.id));
    if (rowNode) {
      this.gridApi?.refreshCells({ rowNodes: [rowNode], columns: ["lots"], force: true });
    }
  }

  protected get lignesPcbAlert(): number {
    return this.orderLines.filter(l => {
      const pcb = l.qteColis;
      if (!pcb || pcb <= 1) return false;
      const qty = l.quantityReceivedTmp ?? l.quantityRequested ?? 0;
      return qty > 0 && qty % pcb !== 0;
    }).length;
  }

  protected get lignesSansLot(): number {
    if (!this.showLotBtn()) return 0;
    return this.orderLines.filter(l => (l.lots?.length ?? 0) === 0).length;
  }

  protected exportPdf(): void {
    this.commandeService.exportToPdf(this.currentCommande.commandeId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, "commande_en_cours");
      } else {
        window.open(URL.createObjectURL(blob));
      }
    });
  }

  protected exportCSV(): void {
    this.commandeService.exportToCsv(this.currentCommande.commandeId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, "commande_en_cours", "csv");
      } else {
        saveAs(blob);
      }
    });
  }

  // ── Scan réception ────────────────────────────────────────────────────────

  protected onScanReception(): void {
    const raw = this.scanValue?.trim();
    if (!raw) return;
    this.scanValue = "";

    // Anti-rebond : ignorer un même code reçu trop vite (double trigger / rebond mécanique)
    const now = Date.now();
    if (this.lastReceptionScan
        && this.lastReceptionScan.code === raw
        && now - this.lastReceptionScan.at < CommandeReceivedComponent.DEDUP_WINDOW_MS) {
      return;
    }
    this.lastReceptionScan = { code: raw, at: now };

    this.deliveryService.scanReception(this.currentCommande.id, raw)
      .pipe(timeout(CommandeReceivedComponent.SCAN_RECEPTION_TIMEOUT_MS))
      .subscribe({
      next: res => {
        const result = res.body!;
        if (!result.found) {
          this.audioFeedback.beepError();
          //  Enrichir le signal avec les lignes provisoires + le code scanné
          const provisionalLines = this.orderLines
            .filter(l => l.provisionalCode)
            .map(l => ({ id: l.id!, libelle: l.produitLibelle! }));
          this.lastScanResult.set({ ...result, provisionalLines, scannedCode: raw });
          this.scheduleClearScanResult();
          return;
        }

        this.audioFeedback.beepSuccess();
        const idx = this.orderLines.findIndex(l => l.id === result.orderLineId);
        if (idx === -1) {
          this.lastScanResult.set(result);
          this.scheduleClearScanResult();
          return;
        }

        const currentLine = this.orderLines[idx];
        const increment = result.scannedQty ?? 1;
        const oldQty = currentLine.quantityReceived ?? 0;
        const newQty = oldQty + increment;
        const orderedQty = currentLine.quantityRequested ?? 0;

        if (newQty > orderedQty) {
          // Confirmation obligatoire avant d'accepter un excédent au scan
          this.confirmDialog.onConfirm(
            () => {
              this.applyScanResult(idx, result, newQty);
              this.lastScanResult.set(result);
              this.scheduleClearScanResult();
            },
            "Excédent détecté",
            `La quantité scannée (${newQty}) dépasse la quantité commandée (${orderedQty})\npour ${currentLine.produitLibelle}.\n\nConfirmer la réception en excédent ?`,
            "pi pi-exclamation-triangle",
            () => {
              // Refusé — révoquer la mise à jour déjà enregistrée côté serveur
              const reverted = { ...currentLine, quantityReceived: oldQty, quantityReceivedTmp: oldQty };
              this.deliveryService.updateQuantityReceived(reverted).subscribe({
                error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Annulation scan")
              });
              this.audioFeedback.beepError();
            }
          );
        } else {
          this.applyScanResult(idx, result, newQty);
          this.lastScanResult.set(result);
          this.scheduleClearScanResult();
        }
      },
      error: err => {
        this.lastScanResult.set(null);
        this.audioFeedback.beepError();
        if (err instanceof TimeoutError) {
          this.notificationService.error("Backend trop lent — scan abandonné", "Scan");
        } else {
          this.notificationService.error(this.errorService.getErrorMessage(err), "Scan");
        }
      }
    });
  }

  private applyScanResult(idx: number, result: IReceptionScanResult, newQty: number): void {
    const updated = { ...this.orderLines[idx] };
    updated.quantityReceived = newQty;
    updated.quantityReceivedTmp = newQty;
    updated.updated = true; // ← marquer comme saisi pour que statut / tauxService / concordance soient corrects
    this.orderLines = [...this.orderLines];
    this.orderLines[idx] = updated;
    this.refreshDisplayRows();

    const rowNode = this.gridApi?.getRowNode(String(updated.id));
    if (rowNode) {
      this.gridApi?.refreshCells({
        rowNodes: [rowNode],
        columns: ["quantityReceivedTmp", "afterStock", "statut"],
        force: true
      });
      // Auto-scroll + flash vert 800 ms
      this.gridApi?.ensureNodeVisible(rowNode, "middle");
      this.gridApi?.flashCells({ rowNodes: [rowNode], flashDuration: 800, fadeDuration: 400 });
    }

    if (result.lotAutoCreated) {
      this.refreshCommande();
    } else if (result.lot?.numLot && result.lot?.expiryDate) {
      // DataMatrix avec info lot non auto-créé → pré-remplir le formulaire lot séquentiel
      const isoDate = result.lot.expiryDate as unknown as string;
      const m = isoDate.match(/^(\d{4})-(\d{2})/);
      const expiry = m ? `${m[2]}/${m[1]}` : "";
      if (expiry) {
        this.scanLotPrefill.set({ numLot: result.lot.numLot, expiry });
        //  le prefill est effacé à la validation de la ligne (voir onSequentialLineChanged)
      }
    }
  }

  //  Associer le code scanné à la première ligne provisoire et ouvrir la cellule CIP
  protected onAssocierScanToProvisional(): void {
    const scan = this.lastScanResult();
    if (!scan?.provisionalLines?.length || !scan.scannedCode) return;
    const firstLine = scan.provisionalLines[0];
    const rowNode = this.gridApi?.getRowNode(String(firstLine.id));
    if (!rowNode) return;
    // Basculer en mode grille pour que la cellule CIP soit visible
    this.setViewMode("grid");
    setTimeout(() => {
      this.gridApi?.ensureNodeVisible(rowNode, "middle");
      setTimeout(() => {
        this.gridApi?.startEditingCell({ rowIndex: rowNode.rowIndex!, colKey: "produitCip" });
        setTimeout(() => {
          const eInput = document.querySelector<HTMLInputElement>(".ag-cell-editor input");
          if (eInput) {
            eInput.value = scan.scannedCode!;
            eInput.dispatchEvent(new Event("input"));
          }
        }, 50);
      }, 100);
    }, 50);
    this.lastScanResult.set(null);
  }

  private scheduleClearScanResult(): void {
    if (this.scanFeedbackTimer) clearTimeout(this.scanFeedbackTimer);
    this.scanFeedbackTimer = setTimeout(() => this.lastScanResult.set(null), 4000);
  }

  /**
   * Démarre le scanner — délègue à {@link ScanOrchestratorService}.
   * Détection automatique USB (CDC/HID) sans requête DB — plug-and-play.
   */
  private setupBarcodeScanner(): void {
    this.installShortcutListener();

    this.scanOrchestrator.onScan$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(code => {
        this.scanValue = code;
        this.onScanReception();
      });

    this.destroyRef.onDestroy(() => {
      this.removeShortcutListener();
      this.removeScanKeydownListener();
      this.scanOrchestrator.teardown();
    });

    // L'orchestrateur détecte lui-même si Tauri est disponible et le mode USB (CDC/HID)
    void this.scanOrchestrator.setup();
  }

  /** Listener clavier raccourcis — actif en permanence quel que soit le mode scanner. */
  private installShortcutListener(): void {
    if (this.shortcutListener) return;
    this.shortcutListener = (event: KeyboardEvent): void => this.handleKeyboardShortcut(event);
    document.addEventListener("keydown", this.shortcutListener, true);
  }

  private removeShortcutListener(): void {
    if (this.shortcutListener) {
      document.removeEventListener("keydown", this.shortcutListener, true);
      this.shortcutListener = null;
    }
  }

  /**
   * Listener clavier scan HID — installé/retiré par les callbacks hidEnable/hidDisable
   * de l'orchestrateur. Alimente le buffer du {@link ReceptionScannerService} qui émettra
   * via `onScanEvent$` (consommé par l'orchestrateur).
   */
  private installScanKeydownListener(): void {
    if (this.scanKeydownListener) return;
    this.scanKeydownListener = (event: KeyboardEvent): void => {
      const result = this.receptionScanner.processKey(event.key);
      if (result.isScanInProgress && this.isInputElementActive()) {
        event.preventDefault();
      }
    };
    document.addEventListener("keydown", this.scanKeydownListener, true);

    if (this.viewMode() !== "sequential") {
      setTimeout(() => {
        (this.scanInputRef()?.nativeElement as HTMLInputElement | undefined)?.focus();
      }, 150);
    }
  }

  private removeScanKeydownListener(): void {
    if (this.scanKeydownListener) {
      document.removeEventListener("keydown", this.scanKeydownListener, true);
      this.scanKeydownListener = null;
    }
  }

  /** Reconnexion manuelle du scanner CDC. */
  protected reconnectScanner(): Promise<void> {
    return this.scanOrchestrator.reconnect();
  }

  /** Raccourcis identiques quel que soit le mode scanner. */
  private handleKeyboardShortcut(event: KeyboardEvent): void {
    if (event.ctrlKey && event.key.toLowerCase() === "g") {
      event.preventDefault();
      this.setViewMode(this.viewMode() === "grid" ? "sequential" : "grid");
    } else if (event.key === "F5" && !event.ctrlKey && !event.shiftKey) {
      event.preventDefault();
      this.onFilterCommandeLines();
    } else if (event.key === "F11") {
      event.preventDefault();
      this.onToutValider();
    } else if (event.altKey && event.key.toLowerCase() === "h") {
      event.preventDefault();
      this.openHelp();
    }
  }

  /**
   * Vérifie si le focus est actuellement sur un champ de saisie texte ou numérique.
   * Utilisé pour décider d'appeler event.preventDefault() lors d'un scan en cours.
   */
  private isInputElementActive(): boolean {
    const el = document.activeElement;
    if (el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement) {
      // Ne pas bloquer si c'est le champ dédié au scan (mode grille)
      return el !== (this.scanInputRef()?.nativeElement);

    }
    return false;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private computeAfterStock(ol: IOrderLine): number {
    return (ol.initStock ?? 0) + (ol.quantityReceivedTmp ?? ol.quantityRequested ?? 0) + (ol.freeQty ?? 0);
  }

  protected lineStatut(ol: IOrderLine): { label: string; severity: string } {
    // Distinguer jamais touché (null) de saisi à 0
    if (ol.quantityReceivedTmp == null || !ol.updated) return { label: "À saisir", severity: "secondary" };
    const rec = ol.quantityReceivedTmp;
    const cmd = ol.quantityRequested ?? 0;
    if (rec === cmd) return { label: "Servi", severity: "success" };
    if (rec === 0) return { label: "Rupture", severity: "danger" };
    if (rec > cmd) return { label: "Excédent", severity: "info" };
    return { label: "Partiel", severity: "warn" };
  }

  //Taux de service : lignes où quantityReceivedTmp >= quantityRequested / total
  protected get tauxService(): number {
    if (!this.orderLines.length) return 0;
    const served = this.orderLines.filter(
      l => l.updated && l.quantityRequested != null &&
        (l.quantityReceivedTmp ?? 0) >= l.quantityRequested &&
        l.quantityRequested > 0
    ).length;
    return Math.round((served / this.orderLines.length) * 100);
  }


  private initPutAway(): void {
    this.configurationService.getParamByKey(Params.APP_PUTAWAY_MODE)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: HttpResponse<IConfiguration>) => {
        if (res) {
          this.putWayMode.set(res.body?.value);
        }
      }
    });
  }


  private buildColumnDefs(): ColDef<IOrderLine>[] {
    const cols: ColDef<IOrderLine>[] = [
      {
        headerName: "#",
        width: 50,
        hide: true,
        valueGetter: p => (p.node?.rowIndex ?? 0) + 1,
        cellStyle: { color: "#9ca3af", fontSize: "0.72rem", textAlign: "center" }
      },
      {
        field: "produitCip",
        headerName: "Code",
        width: 95,
        headerTooltip: "Code CIP — italique = code provisoire, cliquez pour saisir le CIP",
        editable: (p: any) => !!p.data?.provisionalCode,
        cellEditor: "agTextCellEditor",
        cellRenderer: (p: any) => {
          if (!p.data) return "";
          const cip = p.data.produitCip ?? "";
          if (p.data.provisionalCode) {
            return `<span
              style="display:inline-flex;align-items:center;gap:4px;font-family:monospace;font-size:12px;font-style:italic;color:#856404;cursor:pointer;"
              title="Code CIP provisoire — cliquez pour saisir le CIP définitif."
            ><i class="pi pi-exclamation-circle" style="color:#e6a817;font-size:11px;flex-shrink:0;"></i>${cip || "Cliquer pour saisir"}</span>`;
          }
          return `<span style="font-family:monospace;font-size:12px;">${cip}</span>`;
        }
      },
      {
        field: "produitLibelle",
        headerName: "Description",
        flex: 2,
        minWidth: 178
      },
      {
        field: "initStock",
        headerName: "Stock",
        width: 70,
        type: "numericColumn"
      },
      {
        field: "orderCostAmount",
        headerName: "P.A",
        width: 95,
        type: "numericColumn",
        headerTooltip: "Prix d'achat commandé — ⚠ indique un écart avec le tarif catalogue",
        cellRenderer: (p: any) => {
          if (!p.data) return "";
          const val = p.data.orderCostAmount != null ? Number(p.data.orderCostAmount).toLocaleString("fr-FR") : "—";
          if (p.data.costAmount != null && p.data.costAmount !== p.data.orderCostAmount) {
            const tarif = Number(p.data.costAmount).toLocaleString("fr-FR");
            return `<span style="display:flex;align-items:center;gap:4px;white-space:nowrap">
              <span>${val}</span>
              <span title="Tarif catalogue : ${tarif} F" style="display:inline-flex;align-items:center;gap:2px;color:#dc3545;font-size:0.7rem;font-weight:600;cursor:help">
                <i class="pi pi-exclamation-triangle"></i>${tarif}
              </span>
            </span>`;
          }
          return val;
        }
      },
      {
        field: "orderUnitPrice",
        headerName: "P.U",
        width: 95,
        type: "numericColumn",
        headerTooltip: "Prix de vente commandé — ⚠ indique un écart avec le tarif catalogue",
        cellRenderer: (p: any) => {
          if (!p.data) return "";
          const val = p.data.orderUnitPrice != null ? Number(p.data.orderUnitPrice).toLocaleString("fr-FR") : "—";
          if (p.data.regularUnitPrice != null && p.data.regularUnitPrice !== p.data.orderUnitPrice) {
            const tarif = Number(p.data.regularUnitPrice).toLocaleString("fr-FR");
            return `<span style="display:flex;align-items:center;gap:4px;white-space:nowrap">
              <span>${val}</span>
              <span title="Tarif catalogue : ${tarif} F" style="display:inline-flex;align-items:center;gap:2px;color:#dc3545;font-size:0.7rem;font-weight:600;cursor:help">
                <i class="pi pi-exclamation-triangle"></i>${tarif}
              </span>
            </span>`;
          }
          return val;
        }
      },
      {
        field: "quantityRequested",
        headerName: "Qté.cmdée",
        width: 100,
        type: "numericColumn",
        cellRenderer: (p: any) => {
          if (!p.data) return "";
          const qty = p.data.quantityRequested;
          const val = qty != null ? Number(qty).toLocaleString("fr-FR") : "—";
          const pcb = p.data.qteColis;
          const pcbAlert = pcb != null && pcb > 1 && qty != null && qty % pcb !== 0;
          const pcbBadge = pcbAlert
            ? `<span title="Conditionnement : ${pcb} unités/colis — qté commandée non multiple du colisage" style="display:inline-flex;align-items:center;gap:2px;margin-left:4px;padding:1px 4px;border-radius:6px;background:rgba(255,193,7,.15);color:#b45309;font-size:0.65rem;font-weight:700;cursor:help"><i class="pi pi-box"></i>${pcb}</span>`
            : "";
          return `<span>${val}</span>${pcbBadge}`;
        }
      },
      {
        field: "quantityReceivedTmp",
        headerName: "Qté.reçue",
        width: 95,
        type: "numericColumn",
        editable: true,
        cellEditor: "agNumberCellEditor",
        cellEditorParams: { preventStepping: true },
        cellRenderer: (p: any) => {
          if (!p.data) return "";
          const qty = p.data.quantityReceivedTmp;
          const val = qty != null ? Number(qty).toLocaleString("fr-FR") : "—";
          const partial = qty !== p.data.quantityRequested;
          const pcb = p.data.qteColis;
          const pcbAlert = pcb != null && pcb > 1 && qty != null && qty > 0 && qty % pcb !== 0;
          const pcbBadge = pcbAlert
            ? `<span title="Conditionnement : ${pcb} unités/colis — quantité non multiple" style="display:inline-flex;align-items:center;gap:2px;margin-left:4px;padding:1px 4px;border-radius:6px;background:rgba(220,53,69,.12);color:#dc3545;font-size:0.65rem;font-weight:700;cursor:help"><i class="pi pi-box"></i>${pcb}</span>`
            : "";
          return `<span style="${partial ? "color:#f59e0b;font-weight:700" : ""}">${val}</span>${pcbBadge}`;
        }
      },
      {
        field: "freeQty",
        headerName: "Qté.UG",
        width: 80,
        type: "numericColumn",
        editable: true,
        cellEditor: "agNumberCellEditor",
        cellEditorParams: { preventStepping: true }
      },
      {
        headerName: "Stock.après",
        colId: "afterStock",
        width: 105,
        type: "numericColumn",
        valueGetter: p => p.data ? this.computeAfterStock(p.data) : null,
        cellStyle: (p: any) => {
          if (p.value == null) return {};
          if (p.value === 0) return { color: "#f59e0b", fontWeight: 700 };
          if (p.value > 0) return { color: "#16a34a", fontWeight: 600 };
          return {};
        }
      },
      {
        headerName: "Statut",
        colId: "statut",
        width: 83,
        cellRenderer: CommandeReceivedStatutComponent
      }
    ];

    if (this.showLotBtn()) {
      cols.push({
        headerName: "Lots",
        colId: "lots",
        width: 90,
        suppressMovable: true,
        pinned: "right",
        cellRenderer: LotExpandCellComponent,
        suppressKeyboardEvent: (params) => {
          if (params.event.key === " " && !params.editing) {
            params.event.preventDefault();
            const line = params.data as IOrderLine;
            if (line?.id && line.gestionLot !== false) {
              this.onToggleLotExpand(line);
            }
            return true;
          }
          return false;
        }
      });
    }
    // Couverture stock en jours (masquée par défaut)
    cols.push({
      field: "couvertureStockJours",
      headerName: "Couverture",
      width: 100,
      type: "numericColumn",
      headerTooltip: "Jours de couverture stock estimés",
      cellRenderer: (p: any) => {
        const v = p.data?.couvertureStockJours;
        if (v == null) return "—";
        const color = v < 7 ? "#dc3545" : v < 30 ? "#f59e0b" : "#16a34a";
        return `<span style="color:${color};font-weight:700">${v} j</span>`;
      }
    });
    //TVA (masquée par défaut, activable via menu colonnes)
    cols.push({
      field: "tva",
      headerName: "TVA (%)",
      width: 75,
      type: "numericColumn",
      hide: true,
      cellRenderer: (p: any) => p.data?.tva != null ? `${p.data.tva} %` : "—"
    });

    //  Remise + Montant net (masqués par défaut)
    cols.push({
      field: "discountAmount",
      headerName: "Remise",
      width: 95,
      type: "numericColumn",
      hide: true,
      valueFormatter: (p: any) => p.value != null ? `${Number(p.value).toLocaleString("fr-FR")} F` : "—"
    });
    cols.push({
      field: "netAmount",
      headerName: "Net",
      width: 100,
      type: "numericColumn",
      hide: true,
      valueFormatter: (p: any) => p.value != null ? `${Number(p.value).toLocaleString("fr-FR")} F` : "—"
    });


    cols.push({
      colId: "actions",
      headerName: "",
      width: 75,
      sortable: false,
      pinned: "right",
      cellRenderer: CommandeReceivedActionsComponent
    });

    return cols;
  }

  private openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeResponseDialogComponent, {
      size: "xl",
      scrollable: true,
      backdrop: "static"
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.currentCommande;
  }

  private refreshDisplayRows(): void {
    const rows: any[] = [];
    for (const line of this.orderLines) {
      rows.push(line);
      if (this.expandedLineIds.has(line.id!)) {
        rows.push({ __type: "lot-editor", __line: line, __id: `lot-editor-${line.id}` });
      }
    }
    this.displayRows = rows;
    this.gridApi?.setGridOption("rowData", this.displayRows);
  }

  private refreshCommande(): void {
    this.commandeService.find(this.currentCommande.commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande.orderLines as IOrderLine[];
      this.commandeChange.emit(this.currentCommande);
      this.expandedLineIds.clear();
      this.refreshDisplayRows();
    });
  }

  private onFinalize(doTransfer = false): void {
    this.spinner().show();
    this.deliveryService
      .finalizeSaisieEntreeStock({ ...this.currentCommande, doTransfer, conformeSansLot: this.conformeSansLot })
      .pipe(finalize(() => this.spinner().hide()))
      .subscribe({
        next: (res: HttpResponse<IStockEntryResult>) => {
          const finalizedCommandeId = res.body!.commandeId;
          this.checkReliquat(() => this.confirmPrintTicket(finalizedCommandeId));
        },
        error: error => {
          this.notificationService.error(this.errorService.getErrorMessage(error), "Erreur");
        }
      });
  }

  private checkReliquat(afterCallback: () => void): void {
    const lignesPartielles = this.orderLines.filter(
      l => (l.quantityReceivedTmp ?? 0) < (l.quantityRequested ?? 0)
    );
    if (lignesPartielles.length === 0) {
      afterCallback();
      return;
    }
    const totalManquant = lignesPartielles.reduce(
      (sum, l) => sum + ((l.quantityRequested ?? 0) - (l.quantityReceivedTmp ?? 0)),
      0
    );
    this.confirmDialog.onConfirm(
      () => {
        this.commandeService.createReliquat(this.currentCommande.commandeId).subscribe({
          next: res => {
            this.notificationService.success(
              `Reliquat #${res.body!.orderReference ?? res.body!.id} créé (${lignesPartielles.length} article(s))`,
              "Reliquat"
            );
            afterCallback();
          },
          error: err => {
            this.notificationService.error(this.errorService.getErrorMessage(err), "Reliquat");
            afterCallback();
          }
        });
      },
      "Articles manquants",
      `${lignesPartielles.length} article(s) non servis (${totalManquant} unité(s) manquante(s)).\nCréer un reliquat automatique ?`,
      "pi pi-inbox",
      () => afterCallback()
    );
  }

  private confirmPrintTicket(commandeId: CommandeId): void {
    this.confirmDialog.onConfirm(
      () => this.printEtiquette({ ...this.currentCommande, commandeId }),
      "Impression",
      "Voullez-vous imprimer les étiquettes ?",
      null,
      () => {
        this.currentCommande = null;
        this.commandeChange.emit(null);
        this.previousState();
      }
    );
  }

  private printEtiquette(commande: ICommande): void {
    showCommonModal(
      this.modalService,
      EtiquetteComponent,
      { entity: commande, header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${commande.receiptReference} ] ` },
      () => {
        this.currentCommande = null;
        this.commandeChange.emit(null);
        this.previousState();
      },
      "lg",
      null,
      () => {
        this.currentCommande = null;
        this.commandeChange.emit(null);
        this.previousState();
      }
    );
  }
}
