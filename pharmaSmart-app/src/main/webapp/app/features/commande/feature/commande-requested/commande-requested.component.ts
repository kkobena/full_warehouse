import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
  OnInit,
  output,
  computed,
  signal,
  viewChild
} from "@angular/core";
import {HttpResponse} from "@angular/common/http";
import {IFournisseur} from "../../../../shared/model/fournisseur.model";
import {IRemiseRfaFournisseur} from "../../../finances/data-access/models";
import {Observable} from "rxjs";
import {Commande, IAjustementColisage, ICommande} from "app/shared/model/commande.model";
import {CommandeService} from "../../../../entities/commande/commande.service";
import {NgbModal, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {IOrderLine, OrderLine} from "../../../../shared/model/order-line.model";
import {IClientsEnAttente} from "../../../../shared/model/produit-du.model";
import {ErrorService} from "../../../../shared/error.service";
import {IResponseCommande} from "../../../../shared/model/response-commande.model";
import {
  CommandeResponseDialogComponent
} from "../../ui/commande-response-dialog/commande-response-dialog.component";
import {FormsModule} from "@angular/forms";
import {
  AppSplitButtonItem,
  BadgeComponent,
  ButtonComponent,
  SplitButtonComponent,
} from "../../../../shared/ui";
import {
  FournisseurSelectComponent
} from "../../../partners/ui/fournisseur-select/fournisseur-select.component";
import {DeliveryModalComponent} from "../../ui/delivery/delivery-modal/delivery-modal.component";
import {ListLotComponent} from "../../ui/lot/list/list-lot.component";
import {FormLotComponent} from "../../ui/lot/form/form-lot.component";
import {EditProduitComponent} from "../../ui/delivery/edit-produit/edit-produit.component";
import {
  FileResponseModalComponent
} from "../../ui/file-response-modal/file-response-modal.component";
import {showCommonModal} from "../../../../entities/sales/selling-home/sale-helper";
import {CommandeId} from "../../../../shared/model/abstract-commande.model";
import {CommandCommonService} from "../../../../entities/commande/command-common.service";
import {NotificationService} from "../../../../shared/services/notification.service";
import {
  NgbConfirmDialogService
} from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {ProduitSearch} from "../../../../shared/model";
import {EnvoiPharmamlComponent, PharmamlHomeComponent, ReponsePharmamlComponent} from "../pharmaml";
import {
  DispoComparaisonComponent
} from "../pharmaml/ui/dispo-comparaison/dispo-comparaison.component";
import {IPharmamlCommandeResponse} from "../../../../shared/model/pharmaml.model";
import {
  CommandeProductSearchComponent
} from "../../ui/commande-product-search/commande-product-search.component";
import {
  ImportSuggestionModalComponent
} from "../../ui/import-suggestion/import-suggestion-modal.component";
import {CommonModule} from "@angular/common";
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
import {AgGridAngular} from "ag-grid-angular";
import {CommandeRequestedLineActionsComponent} from "./commande-requested-line-actions.component";

import {formatNumber} from 'app/shared/utils/format-utils';
import {AG_GRID_LOCALE_FR} from '../../../../shared/ui/ag-grid/ag-grid-locale.fr';
import {DevisePipe} from 'app/shared/utils/devise';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: "app-commande-requested",
  templateUrl: "./commande-requested.component.html",
  styleUrls: ["./commande-requested.component.scss"],
  host: {
    "(window:keydown.F9)": "onKeyF9($event)",
    "(window:keydown.escape)": "onKeyEscape($event)"
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    BadgeComponent,
    NgbTooltip,
    CommandeProductSearchComponent,
    FournisseurSelectComponent,
    SplitButtonComponent,
    AgGridAngular
    , DevisePipe,
    PharmamlHomeComponent]
})
export class CommandeRequestedComponent implements OnInit {
  commande = input<ICommande | null | undefined>(null);
  commandeChange = output<ICommande | null>();
  /** Libellés français d'AG Grid — voir shared/ui/ag-grid. */
  protected readonly localeFr = AG_GRID_LOCALE_FR;
  protected readonly orderLines = signal<IOrderLine[]>([]);
  protected readonly selectedEl = signal<IOrderLine[]>([]);
  protected quantiteSaisie = 1;
  protected readonly produitSelected = signal<ProduitSearch | null>(null);
  protected readonly selectedProvider = signal<number | null>(null);
  protected seuilMontantCommande = 0;
  protected readonly currentCommande = signal<ICommande | null>(null);
  protected readonly pharmamlActions = signal<AppSplitButtonItem[]>([]);

  protected readonly quantityBox = viewChild.required<ElementRef>("quantityBox");
  protected readonly productSearch = viewChild.required<CommandeProductSearchComponent>("productSearch");

  // ─── AG Grid ─────────────────────────────────────────────────────────────────
  protected readonly theme = themeAlpine;
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
  protected readonly columnDefs: ColDef<IOrderLine>[] = [
    {
      field: "produitCip",
      headerName: "Code",
      width: 130,
      headerTooltip: "Code CIP — italique = code provisoire, cliquez pour saisir le CIP",
      editable: (p: any) => !!p.data?.provisionalCode,
      cellEditor: "agTextCellEditor",
      cellRenderer(p: any) {
        if (!p.data) {
          return "";
        }
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
      minWidth: 150,
      // Le dû client ne figurait nulle part au moment de décider la quantité. Il ne modifie
      // rien tout seul : il se montre, et le pharmacien tranche.
      cellRenderer: (p: any) => {
        const libelle = p.data?.produitLibelle ?? "";
        const attente = this.clientsEnAttente()[p.data?.produitId];
        if (!attente) {
          return libelle;
        }
        const personnes = Math.max(attente.nbClients, attente.nbDemandes);
        const pluriel = personnes > 1 ? "clients attendent" : "client attend";
        return (
          `${libelle} <span class="badge rounded-pill bg-danger-subtle text-danger-emphasis ms-1" ` +
          `title="${personnes} ${pluriel} ce produit — ${attente.quantiteDue} unité(s) due(s)">` +
          `<i class="pi pi-user-plus"></i> ${personnes}</span>`
        );
      }
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
      cellStyle(p: any) {
        if (p.value == null) {
          return {color: "#9ca3af"};
        }
        if (p.value < 7) {
          return {color: "#dc3545", fontWeight: 700};
        }
        if (p.value < 30) {
          return {color: "#fd7e14"};
        }
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
      cellEditorParams: {preventStepping: true},
      cellRenderer(p: any) {
        if (!p.data) {
          return "";
        }
        const val = p.data.orderCostAmount != null
          ? formatNumber(Number(p.data.orderCostAmount)) : "—";
        if (p.data.costAmount != null && p.data.costAmount !== p.data.orderCostAmount) {
          const tarif = formatNumber(Number(p.data.costAmount));
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
      cellEditorParams: {preventStepping: true},
      cellRenderer(p: any) {
        if (!p.data) {
          return "";
        }
        const val = p.data.orderUnitPrice != null
          ? formatNumber(Number(p.data.orderUnitPrice)) : "—";
        if (p.data.regularUnitPrice != null && p.data.regularUnitPrice !== p.data.orderUnitPrice) {
          const tarif = formatNumber(Number(p.data.regularUnitPrice));
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
      width: 115,
      editable: p => !this.isLocked,
      cellEditorParams: {preventStepping: true},
      type: "numericColumn",
      cellEditor: "agNumberCellEditor",
      cellRenderer(p: any) {
        if (!p.data) {
          return "";
        }
        const qty = p.data.quantityRequested;
        const val = qty != null ? formatNumber(Number(qty)) : "—";
        const pcb = p.data.qteColis;
        const mini = p.data.qteMinimaleCommande;
        const contraintes: string[] = [];
        if (pcb != null && pcb > 1) {
          contraintes.push(`colis de ${pcb}`);
        }
        if (mini != null && mini > 0) {
          contraintes.push(`minimum ${mini}`);
        }
        if (!contraintes.length) {
          return `<span>${val}</span>`;
        }
        // Informatif, pas une alerte : la quantité enregistrée est déjà conforme, le badge dit
        // seulement pourquoi elle ne peut pas prendre n'importe quelle valeur.
        const badge = `<span title="Contrainte fournisseur : ${contraintes.join(" — ")}. Une quantité non conforme est portée au multiple supérieur." style="display:inline-flex;align-items:center;gap:2px;margin-left:4px;padding:1px 4px;border-radius:6px;background:rgba(13,110,253,.10);color:#0d6efd;font-size:0.65rem;font-weight:700;cursor:help"><i class="pi pi-th-large"></i>${pcb != null && pcb > 1 ? pcb : mini}</span>`;
        return `<span>${val}</span>${badge}`;
      }
    },

    {
      colId: "actions",
      headerName: "",
      width: 95,
      sortable: false,
      cellRenderer: CommandeRequestedLineActionsComponent
    }
  ];
  protected readonly gridContext: {
    componentParent: CommandeRequestedComponent
  } = {componentParent: this};
  private gridApi: GridApi<IOrderLine> | null = null;
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly commandeService = inject(CommandeService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);

  // ─────────────────────────────────────────────────────────────────────────────
  private readonly commandCommonService = inject(CommandCommonService);

  /**
   * Transmise par EDI. Distinct du verrou : seul ce cas donne au panneau PharmaML une réponse
   * du grossiste à afficher, et seul lui interdit d'importer un fichier de réponse — PharmaML
   * ayant son propre canal de retour.
   */
  protected get estSoumisePharmaml(): boolean {
    return !!this.currentCommande()?.hasBeenSubmittedToPharmaML;
  }

  /**
   * La commande est partie chez le grossiste : elle ne se modifie plus à la main.
   *
   * <p>Le verrou ne portait que sur l'envoi EDI, seul envoi que l'application constatait. Depuis
   * qu'une transmission par mail ou par téléphone se date elle aussi, la même règle s'applique :
   * ce que le grossiste a reçu ne doit plus bouger chez nous, sans quoi le bon imprimé et la
   * commande en base cessent de dire la même chose.
   *
   * <p>Un marquage posé par erreur se défait par « Annuler l'envoi » depuis la liste, ce qui
   * rouvre la commande — le verrou n'enferme donc personne.
   */
  protected get isLocked(): boolean {
    return this.estSoumisePharmaml || !!this.currentCommande()?.envoyee;
  }

  // ── Palier de remise de fin d'année ───────────────────────────────────────

  /**
   * Avancement du grossiste vers son palier RFA, ou null s'il n'y a aucun accord.
   *
   * Le calcul existait déjà, mais dans un écran financier qu'on ouvre après coup. Il n'a de
   * valeur qu'ici, au seul instant où il peut encore changer une décision : orienter ce bon
   * vers ce grossiste plutôt qu'un autre.
   */
  protected readonly palierRfa = signal<IRemiseRfaFournisseur | null>(null);

  /**
   * Ce que ce bon représente face à ce qu'il reste à facturer.
   *
   * On n'ajoute PAS le bon au pourcentage : le chiffre d'affaires compte les commandes closes à
   * leur date de RÉCEPTION, et un bon passé aujourd'hui peut être reçu l'an prochain. Afficher
   * les deux montants côte à côte laisse le pharmacien conclure, sans rien affirmer de faux.
   */
  protected readonly faitBasculerLePalier = computed(() => {
    const rfa = this.palierRfa();
    const montant = this.currentCommande()?.grossAmount ?? 0;
    return !!rfa && rfa.resteAvantPalier > 0 && montant >= rfa.resteAvantPalier;
  });

  // ── Franco de port ────────────────────────────────────────────────────────

  /**
   * Ce qu'il manque pour atteindre le franco de port, ou null s'il est atteint ou non négocié.
   *
   * Un bon à 142 300 F quand le franco est à 150 000 F fait payer des frais de port qu'un
   * article de plus aurait supprimés — et l'officine ne l'apprend qu'à la facture. Le franco
   * retenu est celui de l'agence, à défaut celui de son grossiste : c'est le serveur qui
   * résout la cascade, l'écran ne fait que l'afficher.
   */
  protected readonly manqueAuFranco = computed<number | null>(() => {
    const franco = this.currentCommande()?.fournisseur?.francoEffectif;
    if (!franco || franco <= 0) {
      return null;
    }
    const montant = this.currentCommande()?.grossAmount ?? 0;
    return montant >= franco ? null : franco - montant;
  });

  /** Vrai quand le bon a franchi le franco : la livraison est acquise, on peut valider. */
  protected readonly francoAtteint = computed(() => {
    const franco = this.currentCommande()?.fournisseur?.francoEffectif;
    return !!franco && franco > 0 && (this.currentCommande()?.grossAmount ?? 0) >= franco;
  });

  /**
   * Relit le dû client des produits affichés.
   *
   * <p>Compte TOUS les avoirs ouverts, y compris ceux qu'une commande déjà partie couvre :
   * « 3 clients attendent ce produit » reste vrai pendant que le bon est en route, et c'est ce
   * qui justifie de ne pas diminuer la quantité.
   */
  private chargerClientsEnAttente(): void {
    const produitIds = this.orderLines()
      .map(l => l.produitId)
      .filter((id): id is number => id != null);
    if (produitIds.length === 0) {
      this.clientsEnAttente.set({});
      return;
    }
    this.commandeService.lireClientsEnAttente(produitIds).subscribe({
      next: parProduit => this.clientsEnAttente.set(parProduit),
      error: () => this.clientsEnAttente.set({}),
    });
  }

  /** Clients en attente, par identifiant de produit, pour les lignes affichées. */
  protected readonly clientsEnAttente = signal<Record<number, IClientsEnAttente>>({});

  /** Fournisseur pour lequel le palier affiché a été lu. */
  private fournisseurDuPalier: number | null = null;

  private chargerPalierRfa(fournisseurId: number | null | undefined): void {
    if (!fournisseurId) {
      this.fournisseurDuPalier = null;
      this.palierRfa.set(null);
      return;
    }
    // Appelé après chaque enregistrement, donc à chaque ligne ajoutée : on ne relit que si le
    // grossiste a changé. Le chiffre d'affaires de l'exercice, lui, ne bouge pas pendant une
    // saisie — seules les commandes CLOSES l'alimentent.
    if (fournisseurId === this.fournisseurDuPalier) {
      return;
    }
    this.fournisseurDuPalier = fournisseurId;
    this.commandeService.lireRfaDuGroupe(fournisseurId).subscribe({
      next: rfa => this.palierRfa.set(rfa && rfa.etat !== 'SANS_ACCORD' ? rfa : null),
      // Le palier est une information d'appoint : son absence ne doit pas alarmer au milieu
      // d'une saisie.
      error: () => this.palierRfa.set(null),
    });
  }

  ngOnInit(): void {
    const c = this.commande();
    if (c?.commandeId) {
      // Load full commande with order lines (the list query uses commandes-without-order-lines)
      this.commandeService.find(c.commandeId).subscribe(res => {
        this.currentCommande.set(res.body);
        this.orderLines.set(this.currentCommande()?.orderLines ?? []);
        this.selectedProvider.set(this.currentCommande()?.fournisseurId);
        this.chargerPalierRfa(this.currentCommande()?.fournisseurId);
        this.buildPharmamlActions();
        this.focusProduitBox();
      });
    } else {
      this.selectedProvider.set(null);
    }
    // this.loadSeuilMontant();
  }

  protected readonly getRowId: GetRowIdFunc<IOrderLine> = p => String(p.data.id);

  protected onGridReady(e: GridReadyEvent<IOrderLine>): void {
    this.gridApi = e.api;
  }

  protected onSelectionChanged(): void {
    this.selectedEl.set(this.gridApi?.getSelectedRows() ?? []);
  }


  // ─── PharmaML actions ────────────────────────────────────────────────────────

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

  protected openEnvoi(): void {
    if (!this.currentCommande()?.commandeId) {
      return;
    }
    const ref = this.modalService.open(EnvoiPharmamlComponent, {
      size: "lg",
      backdrop: "static",
      centered: true
    });
    (ref.componentInstance as EnvoiPharmamlComponent).commandeId = this.currentCommande().commandeId;
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
    if (!this.currentCommande()?.commandeId) {
      return;
    }
    const ref = this.modalService.open(ReponsePharmamlComponent, {
      size: "xl",
      backdrop: "static",
      centered: true
    });
    const inst = ref.componentInstance as ReponsePharmamlComponent;
    inst.commandeRef = this.currentCommande().orderReference ?? "";
    inst.orderId = this.currentCommande().commandeId.id.toString();
    ref.result.then(() => {
    }, () => {
    });
  }

  protected ouvrirComparaison(): void {
    if (!this.currentCommande()?.commandeId) {
      return;
    }
    const ref = this.modalService.open(DispoComparaisonComponent, {
      size: "xl",
      backdrop: "static",
      centered: true,
      scrollable: true
    });
    const inst = ref.componentInstance as DispoComparaisonComponent;
    inst.commandeId = this.currentCommande().commandeId;
    inst.header = `Comparaison disponibilité — ${this.currentCommande().orderReference ?? ""}`;
  }

  protected previousState(): void {
    window.history.back();
  }

  protected onQuantityBoxAction(event: any): void {
    const qty = Number(event.target.value);
    if (qty > 0) {
      this.onAddOrderLine(qty);
    }
  }

  protected onQuantity(): void {
    const qty = Number(this.quantityBox().nativeElement.value);
    if (qty > 0) {
      this.onAddOrderLine(qty);
    }
  }

  protected onAddOrderLine(quantityRequested: number): void {
    if (!this.produitSelected()) {
      return;
    }
    if (!this.selectedProvider()) {
      this.notificationService.error("Veuillez selectionner un fournisseur", "Erreur");
      return;
    }
    if (this.currentCommande()?.id !== undefined) {
      this.subscribeToSaveOrderLine(
        this.commandeService.createOrUpdateOrderLine(this.buildOrderLine(this.produitSelected(), quantityRequested))
      );
    } else {
      this.subscribeToSaveOrderLine(this.commandeService.create(this.buildCommande(this.produitSelected(), quantityRequested)));
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
    if (this.currentCommande() && amount > 0) {
      orderLine.orderCostAmount = amount;
      this.subscribeToSaveOrderLine(this.commandeService.updateOrderCostAmount(orderLine));
    }
  }

  protected onUpdateOrderUnitPrice(orderLine: IOrderLine, event: any): void {
    const price = Number(event.target.value);
    if (this.currentCommande() && price > 0) {
      orderLine.orderUnitPrice = price;
      this.subscribeToSaveOrderLine(this.commandeService.updateOrderUnitPrice(orderLine));
    }
  }

  protected onDeleteOrderLine(orderLine: IOrderLine): void {
    if (this.currentCommande()) {
      this.commandeService.deleteOrderLineById(orderLine.orderLineId).subscribe(() => this.refreshCommande());
    }
  }

  protected confirmDeleteItem(item: IOrderLine): void {
    this.confirmDialog.onConfirm(
      () => this.onDeleteOrderLine(item),
      "Suppression",
      "Voulez-vous supprimer de la commande ce produit ?",
      null,
      () => this.resetProductInput()
    );
  }

  protected confirmDeleteAll(): void {
    this.confirmDialog.onConfirm(
      () => this.deleteSelectedLines(),
      "Suppression",
      "Voulez-vous supprimer toutes les lignes ?",
      null,
      () => this.resetProductInput()
    );
  }

  protected onCreateBon(): void {
    const montant = this.currentCommande()?.grossAmount ?? 0;
    if (this.seuilMontantCommande > 0 && montant > this.seuilMontantCommande) {
      const fmt = (n: number): string => formatNumber(n);
      this.confirmDialog.onConfirm(
        () => this.doCreateBon(),
        "Validation titulaire requise",
        `Le montant total de cette commande (${fmt(montant)} F) dépasse le seuil autorisé (${fmt(this.seuilMontantCommande)} F).\n\nLa validation du pharmacien titulaire est requise.\nVoulez-vous continuer ?`
      );
    } else {
      this.doCreateBon();
    }
  }

  protected onImportSuggestion(): void {
    const modalRef = this.modalService.open(ImportSuggestionModalComponent, {
      size: "xl",
      scrollable: true,
      centered: true,
      backdrop: "static",
      modalDialogClass: "facture-modal-dialog"
    });
    modalRef.componentInstance.commandeId = this.currentCommande()!.commandeId;
    modalRef.componentInstance.fournisseurId = this.currentCommande()!.fournisseurId;
    modalRef.componentInstance.commandeFournisseurId = this.currentCommande()!.fournisseurId;
    // Le PRINCIPAL de l'agence servie : c'est lui qui porte les propositions d'achat.
    modalRef.componentInstance.commandeFournisseurPrincipalId =
      this.currentCommande()!.fournisseur?.parentId ?? this.currentCommande()!.fournisseurId;
    modalRef.result.then(
      imported => {
        if (imported) {
          this.refreshCommande();
        }
      },
      () => {
      }
    );
  }

  protected onImporterReponseCommande(): void {
    showCommonModal(
      this.modalService,
      FileResponseModalComponent,
      {commandeSelected: this.currentCommande(), header: "IMPORTER REPONSE"},
      (responseCommande: IResponseCommande) => {
        if (responseCommande) {
          this.refreshCommande();
          this.openImporterReponseCommandeDialog(responseCommande);
        }
      },
      'xl'
    );
  }

  protected onProductSelected(product: ProduitSearch | null): void {
    this.produitSelected.set(product);
    if (product) {
      this.focusAndSelect(this.quantityBox().nativeElement, 50);
    }
  }

  protected onProductScanned(product: ProduitSearch): void {
    this.produitSelected.set(product);
    this.focusAndSelect(this.quantityBox().nativeElement, 50);
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    // Le sélecteur est groupé : un clic sur l'EN-TÊTE d'un groupe rend le grossiste
    // principal, qui ne porte pas d'identifiant de livraison. Le retenir viderait le
    // fournisseur de la commande sans que rien ne le signale — l'écran redemanderait
    // simplement d'en choisir un.
    if (f && f.id == null) {
      return;
    }
    this.selectedProvider.set(f?.id ?? null);
    if (this.currentCommande()?.id) {
      this.changeGrossiste();
    } else {
      this.focusProduitBox();
    }
  }

  protected orderLineTableColor(orderLine: IOrderLine): string {
    if (!orderLine) {
      return "";
    }
    if (orderLine.costAmount !== orderLine.orderCostAmount) {
      return "pharma-row-danger";
    }
    if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) {
      return "pharma-row-warning";
    }
    return "";
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
        {entity: null, deliveryItem: orderLine, header: "Ajout de lot"},
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
        delivery: this.currentCommande(),
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
  // `Event` et non `KeyboardEvent` : c'est le type que fournit un host listener Angular.
  protected onKeyF9(event: Event): void {
    event.preventDefault();
    if (this.currentCommande()?.id) {
      this.onCreateBon();
    }
  }

  /** Echap : remet le focus sur la recherche produit */
  protected onKeyEscape(event: Event): void {
    // Ne pas interférer si une modale est ouverte
    if (document.querySelector(".modal.show")) {
      return;
    }
    event.preventDefault();
    this.productSearch()?.reset();
    this.focusProduitBox();
  }

  private buildPharmamlActions(): void {
    if (this.estSoumisePharmaml || this.currentCommande()?.orderStatus === "RECEIVED") {
      this.pharmamlActions.set([
        {label: "Voir réponse", icon: "pi pi-file", command: () => this.openReponse()},
        ...this.comparaisonAction()
      ]);
    } else {
      this.pharmamlActions.set([
        {label: "Envoyer via PharmaML", icon: "pi pi-send", command: () => this.openEnvoi()},
        ...this.comparaisonAction()
      ]);
    }
  }


  private comparaisonAction(): AppSplitButtonItem[] {

    return [
      {
        label: "Comparer multi-grossistes",
        icon: "pi pi-chart-bar",
        separatorBefore: true,
        command: () => this.ouvrirComparaison()
      }
    ];

  }

  private doCreateBon(): void {
    showCommonModal(
      this.modalService,
      DeliveryModalComponent,
      {commande: this.currentCommande(), header: "CREATION DU BON DE LIVRAISON"},
      commande => {
        if (commande) {
          this.commandCommonService.pendingOpenDeliveryId.set(commande.commandeId);
          this.commandCommonService.navigateToBonsLivraison();
        }
      },
      "xl"
    );
  }

  private openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeResponseDialogComponent, {
      size: "xl",
      scrollable: true,
      backdrop: "static"
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.currentCommande();
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
    if (!this.currentCommande()?.commandeId) {
      return;
    }
    this.commandeService.delete(this.currentCommande().commandeId).subscribe({
      next: () => {
        this.currentCommande.set(null);
        this.orderLines.set([]);
        this.selectedProvider.set(null);
        this.commandeChange.emit(null);
        this.notificationService.success("Commande supprimée", "Suppression");
      },
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
    });
  }

  private reloadCommande(): void {
    if (!this.currentCommande()?.commandeId) {
      return;
    }
    this.commandeService.find(this.currentCommande().commandeId).subscribe(res => {
      this.currentCommande.set(res.body);
      this.orderLines.set(this.currentCommande()?.orderLines ?? []);
      this.buildPharmamlActions();
      this.commandeChange.emit(this.currentCommande());
    });
  }

  private refreshCommande(): void {
    if (!this.currentCommande()?.commandeId) {
      return;
    }
    this.commandeService.find(this.currentCommande().commandeId).subscribe(res => {
      this.currentCommande.set(res.body);
      this.orderLines.set(this.currentCommande()?.orderLines ?? []);
      this.buildPharmamlActions();
      this.focusProduitBox();
      this.commandeChange.emit(this.currentCommande());
    });
  }

  private onSaveSuccess(commandeId: CommandeId): void {
    if (!commandeId) {
      return;
    }
    this.commandeService.find(commandeId).subscribe(res => {
      this.currentCommande.set(res.body);
      this.orderLines.set(this.currentCommande()?.orderLines ?? []);
      // Un changement de grossiste en cours de saisie laissait le palier de l'ancien.
      this.chargerPalierRfa(this.currentCommande()?.fournisseurId);
      this.chargerClientsEnAttente();
      this.buildPharmamlActions();
      this.resetProductInput();
      this.commandeChange.emit(this.currentCommande());
    });
  }

  private resetProductInput(): void {
    if (this.quantityBox()) {
      this.quantityBox().nativeElement.value = 1;
    }
    this.produitSelected.set(null);
    this.productSearch()?.reset();
    this.focusProduitBox();
  }

  private deleteSelectedLines(): void {
    if (!this.currentCommande()?.commandeId) {
      return;
    }
    const ids = this.selectedEl().map(e => e.orderLineId);
    this.commandeService.deleteOrderLinesByIds(this.currentCommande().commandeId, ids).subscribe(() => {
      this.selectedEl.set([]);
      this.refreshCommande();
    });
  }

  private changeGrossiste(): void {
    if (!this.currentCommande()) {
      return;
    }
    this.currentCommande().fournisseurId = this.selectedProvider();
    const cmdId = this.currentCommande().commandeId;
    this.commandeService.changeGrossiste(this.currentCommande()).subscribe({
      next: () => this.onSaveSuccess(cmdId),
      error: error => {
        this.notificationService.error(this.errorService.getErrorMessage(error), "Erreur");
        this.onSaveSuccess(cmdId);
      }
    });
  }

  private subscribeToSaveOrderLine(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe({
      next: res => {
        this.signalerAjustementColisage(res.body?.ajustementColisage);
        this.onSaveSuccess(res.body?.commandeId);
      },
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
    });
  }

  /**
   * Le logiciel vient de changer la quantité saisie pour respecter le colisage ou le minimum
   * fournisseur. Sans ce message, la grille afficherait un autre nombre que celui tapé sans rien
   * expliquer.
   */
  private signalerAjustementColisage(ajustement?: IAjustementColisage): void {
    if (!ajustement) {
      return;
    }
    const produit = ajustement.produitLibelle ? `${ajustement.produitLibelle} : ` : "";
    this.notificationService.info(
      `${produit}quantité portée de ${ajustement.quantiteDemandee} à ${ajustement.quantiteRetenue} (${this.decrireContrainte(ajustement)}).`,
      "Conditionnement fournisseur"
    );
  }

  /** La phrase appartient à l'écran : le backend ne transmet que la contrainte et les nombres. */
  private decrireContrainte(ajustement: IAjustementColisage): string {
    if (ajustement.contrainte === "QUANTITE_MINIMALE") {
      return `minimum fournisseur de ${ajustement.qteMinimaleCommande} unités`;
    }
    const nombreDeColis = ajustement.quantiteRetenue / ajustement.qteColis;
    return nombreDeColis === 1
      ? `colis de ${ajustement.qteColis} unités`
      : `${nombreDeColis} colis de ${ajustement.qteColis} unités`;
  }

  private subscribeUpdateOrderLine(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(this.currentCommande()?.commandeId),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur")
    });
  }

  private buildOrderLine(produit: ProduitSearch, quantityRequested: number): IOrderLine {
    return {
      ...new OrderLine(),
      produitId: produit.id,
      totalQuantity: produit.totalQuantity,
      commande:
        this.currentCommande()?.id !== undefined
          ? this.currentCommande()
          : {...new Commande(), fournisseurId: this.selectedProvider()},
      quantityRequested
    };
  }

  private buildCommande(produit: ProduitSearch, quantityRequested: number): ICommande {
    return {
      ...new Commande(),
      fournisseurId: this.selectedProvider(),
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
