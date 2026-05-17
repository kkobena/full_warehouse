import { Component, ElementRef, inject, input, OnInit, output, viewChild } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { IFournisseur } from "../../../../shared/model/fournisseur.model";
import { Observable } from "rxjs";
import { Commande, ICommande } from "app/shared/model/commande.model";
import { CommandeService } from "../../../../entities/commande/commande.service";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { IOrderLine, OrderLine } from "../../../../shared/model/order-line.model";
import { ErrorService } from "../../../../shared/error.service";
import { IResponseCommande } from "../../../../shared/model/response-commande.model";
import { CommandeResponseDialogComponent } from "../../ui/commande-response-dialog/commande-response-dialog.component";
import { FormsModule } from "@angular/forms";
import { ButtonModule } from "primeng/button";
import { RippleModule } from "primeng/ripple";
import { InputTextModule } from "primeng/inputtext";
import { TagModule } from "primeng/tag";
import { FileUploadModule } from "primeng/fileupload";
import { TooltipModule } from "primeng/tooltip";
import { FournisseurSelectComponent } from "../../../partners/ui/fournisseur-select/fournisseur-select.component";
import { InputGroup } from "primeng/inputgroup";
import { InputGroupAddon } from "primeng/inputgroupaddon";
import { DeliveryModalComponent } from "../../ui/delivery/delivery-modal/delivery-modal.component";
import { ListLotComponent } from "../../ui/lot/list/list-lot.component";
import { FormLotComponent } from "../../ui/lot/form/form-lot.component";
import { EditProduitComponent } from "../../ui/delivery/edit-produit/edit-produit.component";
import { FileResponseModalComponent } from "../../ui/file-response-modal/file-response-modal.component";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import { CommandeId } from "../../../../shared/model/abstract-commande.model";
import { CommandCommonService } from "../../../../entities/commande/command-common.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { ProduitSearch } from "../../../../shared/model";
import { MenuItem } from "primeng/api";
import { SplitButton } from "primeng/splitbutton";
import { EnvoiPharmamlComponent, ReponsePharmamlComponent } from "../pharmaml";
import { DispoComparaisonComponent } from "../pharmaml/ui/dispo-comparaison/dispo-comparaison.component";
import { IPharmamlCommandeResponse } from "../../../../shared/model/pharmaml.model";
import { CommandeProductSearchComponent } from "../../ui/commande-product-search/commande-product-search.component";
import { ImportSuggestionModalComponent } from "../../ui/import-suggestion/import-suggestion-modal.component";
import { CommonModule } from "@angular/common";
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
  RowSelectionOptions,
  themeAlpine
} from "ag-grid-community";
import { AgGridAngular } from "ag-grid-angular";
import { CommandeRequestedLineActionsComponent } from "./commande-requested-line-actions.component";

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: "app-commande-requested",
  templateUrl: "./commande-requested.component.html",
  styleUrls: ["./commande-requested.component.scss"],
  host: {
    "(window:keydown.F9)": "onKeyF9($event)",
    "(window:keydown.escape)": "onKeyEscape($event)"
  },
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    TagModule,
    CommandeProductSearchComponent,
    FournisseurSelectComponent,
    FileUploadModule,
    TooltipModule,
    InputGroup,
    InputGroupAddon,
    SplitButton,
    AgGridAngular
  ]
})
export class CommandeRequestedComponent implements OnInit {
  commande = input<ICommande | null | undefined>(null);
  commandeChange = output<ICommande | null>();

  protected orderLines: IOrderLine[] = [];
  protected selectedEl: IOrderLine[] = [];
  protected quantiteSaisie = 1;
  protected produitSelected: ProduitSearch | null = null;
  protected selectedProvider?: number | null = null;
  protected showLotBtn = false;
  protected seuilMontantCommande = 0;
  protected currentCommande?: ICommande | null = null;
  protected pharmamlActions: MenuItem[] = [];

  protected readonly quantityBox = viewChild.required<ElementRef>("quantityBox");
  protected readonly productSearch = viewChild.required<CommandeProductSearchComponent>("productSearch");

  // ─── AG Grid ─────────────────────────────────────────────────────────────────
  protected readonly theme = themeAlpine;
  private gridApi: GridApi<IOrderLine> | null = null;

  protected readonly defaultColDef: ColDef<IOrderLine> = {
    resizable: true,
    sortable: false,
    suppressHeaderMenuButton: true
  };

  protected readonly rowSelection: RowSelectionOptions = {
    mode: "multiRow",
    checkboxes: true,
    headerCheckbox: true,
    enableClickSelection: false
  };

  protected readonly rowClassRules: RowClassRules<IOrderLine> = {
    "pharma-row-danger": p => !!p.data && p.data.costAmount !== p.data.orderCostAmount,
    "pharma-row-warning": p =>
      !!p.data &&
      p.data.costAmount === p.data.orderCostAmount &&
      p.data.regularUnitPrice !== p.data.orderUnitPrice,
    "pharma-row-provisional": p => !!p.data?.provisionalCode
  };

  protected readonly getRowId: GetRowIdFunc<IOrderLine> = p => String(p.data.id);

  protected readonly columnDefs: ColDef<IOrderLine>[] = [
    {
      field: "produitCip",
      headerName: "Code",
      width: 130,
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
      minWidth: 150
    },
    {
      field: "initStock",
      headerName: "Stock",
      width: 80,
      type: "numericColumn"
    },
    {
      field: "couvertureStockJours",
      headerName: "Couv.",
      width: 80,
      type: "numericColumn",
      headerTooltip: "Couverture stock en jours",
      valueFormatter: (p: any) => p.value != null ? `${p.value}j` : "—",
      cellStyle: (p: any) => {
        if (p.value == null) return { color: "#9ca3af" };
        if (p.value < 7) return { color: "#dc3545", fontWeight: 700 };
        if (p.value < 30) return { color: "#fd7e14" };
        return null;
      }
    },
    {
      field: "orderCostAmount",
      headerName: "P.A",
      width: 130,
      editable: p => !this.isLocked,
      type: "numericColumn",
      cellEditor: "agNumberCellEditor",
      headerTooltip: "Prix d'achat commandé — ⚠ indique un écart avec le tarif catalogue",
      cellEditorParams: { preventStepping: true },
      cellRenderer: (p: any) => {
        if (!p.data) return "";
        const val = p.data.orderCostAmount != null
          ? Number(p.data.orderCostAmount).toLocaleString("fr-FR") : "—";
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
      width: 130,
      editable: p => !this.isLocked,
      type: "numericColumn",
      cellEditor: "agNumberCellEditor",
      headerTooltip: "Prix unitaire commandé — ⚠ indique un écart avec le tarif catalogue",
      cellEditorParams: { preventStepping: true },
      cellRenderer: (p: any) => {
        if (!p.data) return "";
        const val = p.data.orderUnitPrice != null
          ? Number(p.data.orderUnitPrice).toLocaleString("fr-FR") : "—";
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
      headerName: "Qté",
      width: 90,
      editable: p => !this.isLocked,
      cellEditorParams: { preventStepping: true },
      type: "numericColumn",
      cellEditor: "agNumberCellEditor"
    },

    {
      colId: "actions",
      headerName: "",
      width: 95,
      sortable: false,
      cellRenderer: CommandeRequestedLineActionsComponent
    }
  ];

  protected readonly gridContext: { componentParent: CommandeRequestedComponent } = { componentParent: this };

  protected onGridReady(e: GridReadyEvent<IOrderLine>): void {
    this.gridApi = e.api;
  }

  protected onSelectionChanged(): void {
    this.selectedEl = this.gridApi?.getSelectedRows() ?? [];
  }

  protected onCellValueChanged(e: CellValueChangedEvent<IOrderLine>): void {
    const line = e.data;
    const val = Number(e.newValue);
    switch (e.colDef.field) {
      case "produitCip": {
        const cip = (e.newValue as string)?.trim();
        if (cip) {
          line.produitCip = cip;
          this.subscribeUpdateOrderLine(this.commandeService.updateCip(line));
        }
        break;
      }
      case "quantityRequested":
        if (val > 0) {
          line.quantityRequested = val;
          this.subscribeToSaveOrderLine(this.commandeService.updateQuantityRequested(line));
        }
        break;

      case "orderCostAmount":
        if (val > 0) {
          line.orderCostAmount = val;
          this.subscribeToSaveOrderLine(this.commandeService.updateOrderCostAmount(line));
        }
        break;
      case "orderUnitPrice":
        if (val > 0) {
          line.orderUnitPrice = val;
          this.subscribeToSaveOrderLine(this.commandeService.updateOrderUnitPrice(line));
        }
        break;
    }
  }

  protected onCellClicked(e: CellClickedEvent<IOrderLine>): void {
    // Actions handled by CommandeRequestedLineActionsComponent renderer
  }

  // ─────────────────────────────────────────────────────────────────────────────

  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly commandeService = inject(CommandeService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly commandCommonService = inject(CommandCommonService);


  // ─── PharmaML actions ────────────────────────────────────────────────────────

  private buildPharmamlActions(): void {
    if (this.isLocked || this.currentCommande?.orderStatus === "RECEIVED") {
      this.pharmamlActions = [
        { label: "Voir réponse", icon: "pi pi-file", disabled: false, command: () => this.openReponse() },
        { separator: true },
        { label: "Comparer multi-grossistes", icon: "pi pi-chart-bar", disabled: false, command: () => this.ouvrirComparaison() }
      ];
    } else {
      this.pharmamlActions = [
        { label: "Envoyer via PharmaML", icon: "pi pi-send", disabled: false, command: () => this.openEnvoi() },
        { label: "Voir réponse", icon: "pi pi-file", disabled: false, command: () => this.openReponse() },
        { separator: true },
        { label: "Comparer multi-grossistes", icon: "pi pi-chart-bar", disabled: false, command: () => this.ouvrirComparaison() }
      ];
    }
  }

  protected openEnvoi(): void {
    if (!this.currentCommande?.commandeId) return;
    const ref = this.modalService.open(EnvoiPharmamlComponent, { size: "lg", backdrop: "static", centered: true });
    (ref.componentInstance as EnvoiPharmamlComponent).commandeId = this.currentCommande.commandeId;
    ref.result.then(
      (result: IPharmamlCommandeResponse) => {
        if (result.reliquatCommandeId != null) {
          this.notificationService.info(`Commande reliquat créée automatiquement (#${result.reliquatCommandeId})`, "Reliquat créé");
        } else {
          result.success
            ? this.notificationService.success(`${result.successCount} / ${result.totalProduit} produits acceptés`, "Envoi réussi")
            : this.notificationService.warning(`${result.successCount} / ${result.totalProduit} produits acceptés`, "Envoi partiel");
        }
        this.reloadCommande();
      },
      () => {
      }
    );
  }

  protected openReponse(): void {
    if (!this.currentCommande?.commandeId) return;
    const ref = this.modalService.open(ReponsePharmamlComponent, { size: "xl", backdrop: "static", centered: true });
    const inst = ref.componentInstance as ReponsePharmamlComponent;
    inst.commandeRef = this.currentCommande.orderReference ?? "";
    inst.orderId = this.currentCommande.commandeId.id.toString();
    ref.result.then(() => {
    }, () => {
    });
  }

  protected ouvrirComparaison(): void {
    if (!this.currentCommande?.commandeId) return;
    const ref = this.modalService.open(DispoComparaisonComponent, {
      size: "lg",
      backdrop: "static",
      centered: true,
      scrollable: true
    });
    const inst = ref.componentInstance as DispoComparaisonComponent;
    inst.commandeId = this.currentCommande.commandeId;
    inst.header = `Comparaison disponibilité — ${this.currentCommande.orderReference ?? ""}`;
  }

  ngOnInit(): void {
    const c = this.commande();
    if (c?.commandeId) {
      // Load full commande with order lines (the list query uses commandes-without-order-lines)
      this.commandeService.find(c.commandeId).subscribe(res => {
        this.currentCommande = res.body;
        this.orderLines = this.currentCommande?.orderLines ?? [];
        this.selectedProvider = this.currentCommande?.fournisseurId;
        this.buildPharmamlActions();
        this.focusProduitBox();
      });
    } else {
      this.selectedProvider = null;
    }
    //this.loadSeuilMontant();
  }

  protected previousState(): void {
    window.history.back();
  }

  protected onQuantityBoxAction(event: any): void {
    const qty = Number(event.target.value);
    if (qty > 0) this.onAddOrderLine(qty);
  }

  protected onQuantity(): void {
    const qty = Number(this.quantityBox().nativeElement.value);
    if (qty > 0) this.onAddOrderLine(qty);
  }

  protected onAddOrderLine(quantityRequested: number): void {
    if (!this.produitSelected) return;
    if (!this.selectedProvider) {
      this.notificationService.error("Veuillez selectionner un fournisseur", "Erreur");
      return;
    }
    if (this.currentCommande?.id !== undefined) {
      this.subscribeToSaveOrderLine(
        this.commandeService.createOrUpdateOrderLine(this.buildOrderLine(this.produitSelected, quantityRequested))
      );
    } else {
      this.subscribeToSaveOrderLine(this.commandeService.create(this.buildCommande(this.produitSelected, quantityRequested)));
    }
  }

  protected onUpdateQuantityRequested(orderLine: IOrderLine, event: any): void {
    const qty = Number(event.target.value);
    if (qty > 0) {
      orderLine.quantityRequested = qty;
      this.subscribeToSaveOrderLine(this.commandeService.updateQuantityRequested(orderLine));
    }
  }

  protected onUpdateFreeQtyRequested(orderLine: IOrderLine, event: any): void {
    const qty = Number(event.target.value);
    if (qty >= 0) {
      orderLine.freeQty = qty;
      this.subscribeUpdateOrderLine(this.commandeService.updateQuantityUG(orderLine));
    }
  }

  protected onUpdateOrderCostAmount(orderLine: IOrderLine, event: any): void {
    const amount = Number(event.target.value);
    if (this.currentCommande && amount > 0) {
      orderLine.orderCostAmount = amount;
      this.subscribeToSaveOrderLine(this.commandeService.updateOrderCostAmount(orderLine));
    }
  }

  protected onUpdateOrderUnitPrice(orderLine: IOrderLine, event: any): void {
    const price = Number(event.target.value);
    if (this.currentCommande && price > 0) {
      orderLine.orderUnitPrice = price;
      this.subscribeToSaveOrderLine(this.commandeService.updateOrderUnitPrice(orderLine));
    }
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
      "Voullez-vous supprimer de la commande ce produit ?",
      null,
      () => this.resetProductInput()
    );
  }

  protected confirmDeleteAll(): void {
    this.confirmDialog.onConfirm(
      () => this.deleteSelectedLines(),
      "Suppression",
      "Voullez-vous supprimer toutes les lignes ?",
      null,
      () => this.resetProductInput()
    );
  }

  protected onCreateBon(): void {
    const montant = this.currentCommande?.grossAmount ?? 0;
    if (this.seuilMontantCommande > 0 && montant > this.seuilMontantCommande) {
      const fmt = (n: number): string => n.toLocaleString("fr-FR");
      this.confirmDialog.onConfirm(
        () => this.doCreateBon(),
        "Validation titulaire requise",
        `Le montant total de cette commande (${fmt(montant)} F) dépasse le seuil autorisé (${fmt(this.seuilMontantCommande)} F).\n\nLa validation du pharmacien titulaire est requise.\nVoulez-vous continuer ?`
      );
    } else {
      this.doCreateBon();
    }
  }

  private doCreateBon(): void {
    showCommonModal(
      this.modalService,
      DeliveryModalComponent,
      { commande: this.currentCommande, header: "CREATION DU BON DE LIVRAISON" },
      commande => {
        if (commande) {
          this.commandCommonService.pendingOpenDeliveryId.set(commande.commandeId);
          this.commandCommonService.navigateToBonsLivraison();
        }
      },
      "lg"
    );
  }

  protected onImportSuggestion(): void {
    const modalRef = this.modalService.open(ImportSuggestionModalComponent, {
      size: "xl",
      scrollable: true,
      centered:true,
      backdrop: "static"
    });
    modalRef.componentInstance.commandeId = this.currentCommande!.commandeId;
    modalRef.componentInstance.fournisseurId = this.currentCommande!.fournisseurId;
    modalRef.componentInstance.commandeFournisseurId = this.currentCommande!.fournisseurId;
    modalRef.result.then(
      imported => {
        if (imported) this.refreshCommande();
      },
      () => {
      }
    );
  }

  protected onImporterReponseCommande(): void {
    showCommonModal(
      this.modalService,
      FileResponseModalComponent,
      { commandeSelected: this.currentCommande, header: "IMPORTER REPONSE" },
      (responseCommande: IResponseCommande) => {
        if (responseCommande) {
          this.refreshCommande();
          this.openImporterReponseCommandeDialog(responseCommande);
        }
      },
      "lg"
    );
  }

  protected onProductSelected(product: ProduitSearch | null): void {
    this.produitSelected = product;
    if (product) {
      this.focusAndSelect(this.quantityBox().nativeElement, 50);
    }
  }

  protected onProductScanned(product: ProduitSearch): void {
    this.produitSelected = product;
    this.focusAndSelect(this.quantityBox().nativeElement, 50);
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.selectedProvider = f?.id ?? null;
    if (this.currentCommande?.id) {
      this.changeGrossiste();
    } else {
      this.focusProduitBox();
    }
  }

  protected orderLineTableColor(orderLine: IOrderLine): string {
    if (!orderLine) return "";
    if (orderLine.costAmount !== orderLine.orderCostAmount) return "pharma-row-danger";
    if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) return "pharma-row-warning";
    return "";
  }

  protected get isLocked(): boolean {
    return !!this.currentCommande?.hasBeenSubmittedToPharmaML;
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

  protected focusProduitBox(): void {
    this.productSearch()?.getFocus();
  }

  /** F9 : déclenche "Créer le bon de livraison" si une commande est en cours */
  protected onKeyF9(event: KeyboardEvent): void {
    event.preventDefault();
    if (this.currentCommande?.id) {
      this.onCreateBon();
    }
  }

  /** Echap : remet le focus sur la recherche produit */
  protected onKeyEscape(event: KeyboardEvent): void {
    // Ne pas interférer si une modale est ouverte
    if (document.querySelector(".modal.show")) return;
    event.preventDefault();
    this.productSearch()?.reset();
    this.focusProduitBox();
  }

  private openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeResponseDialogComponent, {
      size: "xl",
      scrollable: true,
      backdrop: "static"
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.currentCommande;
    modalRef.result.then(
      result => {
        if (result === "DELETE") {
          this.confirmDialog.onConfirm(
            () => this.deleteCommandeApresRuptureTotale(),
            "Suppression de la commande",
            "Tous les produits sont en rupture. Voulez-vous supprimer définitivement cette commande ?"
          );
        }
      },
      () => {
      }
    );
  }

  private deleteCommandeApresRuptureTotale(): void {
    if (!this.currentCommande?.commandeId) return;
    this.commandeService.delete(this.currentCommande.commandeId).subscribe({
      next: () => {
        this.currentCommande = null;
        this.orderLines = [];
        this.selectedProvider = null;
        this.commandeChange.emit(null);
        this.notificationService.success("Commande supprimée", "Suppression");
      },
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
    });
  }

  private reloadCommande(): void {
    if (!this.currentCommande?.commandeId) return;
    this.commandeService.find(this.currentCommande.commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande?.orderLines ?? [];
      this.buildPharmamlActions();
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private refreshCommande(): void {
    if (!this.currentCommande?.commandeId) return;
    this.commandeService.find(this.currentCommande.commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande?.orderLines ?? [];
      this.buildPharmamlActions();
      this.focusProduitBox();
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private onSaveSuccess(commandeId: CommandeId): void {
    if (!commandeId) return;
    this.commandeService.find(commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande?.orderLines ?? [];
      this.buildPharmamlActions();
      this.resetProductInput();
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private resetProductInput(): void {
    if (this.quantityBox()) this.quantityBox().nativeElement.value = 1;
    this.produitSelected = null;
    this.productSearch()?.reset();
    this.focusProduitBox();
  }

  private deleteSelectedLines(): void {
    if (!this.currentCommande?.commandeId) return;
    const ids = this.selectedEl.map(e => e.orderLineId);
    this.commandeService.deleteOrderLinesByIds(this.currentCommande.commandeId, ids).subscribe(() => {
      this.selectedEl = [];
      this.refreshCommande();
    });
  }

  private changeGrossiste(): void {
    if (!this.currentCommande) return;
    this.currentCommande.fournisseurId = this.selectedProvider;
    const cmdId = this.currentCommande.commandeId;
    this.commandeService.changeGrossiste(this.currentCommande).subscribe({
      next: () => this.onSaveSuccess(cmdId),
      error: error => {
        this.notificationService.error(this.errorService.getErrorMessage(error), "Erreur");
        this.onSaveSuccess(cmdId);
      }
    });
  }

  private subscribeToSaveOrderLine(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body?.commandeId),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
    });
  }

  private subscribeUpdateOrderLine(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(this.currentCommande?.commandeId),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
    });
  }

  private buildOrderLine(produit: ProduitSearch, quantityRequested: number): IOrderLine {
    return {
      ...new OrderLine(),
      produitId: produit.id,
      totalQuantity: produit.totalQuantity,
      commande:
        this.currentCommande?.id !== undefined
          ? this.currentCommande
          : { ...new Commande(), fournisseurId: this.selectedProvider },
      quantityRequested
    };
  }

  private buildCommande(produit: ProduitSearch, quantityRequested: number): ICommande {
    return {
      ...new Commande(),
      fournisseurId: this.selectedProvider,
      orderLines: [this.buildOrderLine(produit, quantityRequested)]
    };
  }

  private focusAndSelect(element: any, delay: number): void {
    setTimeout(() => {
      if (element) {
        element.focus();
        element.select();
      }
    }, delay);
  }
}
