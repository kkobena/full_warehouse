import {Component, inject, input, OnInit, output, viewChild} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ICommande} from 'app/shared/model/commande.model';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {IOrderLine} from '../../../../shared/model/order-line.model';
import {MenuItem} from 'primeng/api';
import {ErrorService} from '../../../../shared/error.service';
import {saveAs} from 'file-saver';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {InputTextModule} from 'primeng/inputtext';
import {TagModule} from 'primeng/tag';
import {TableModule} from 'primeng/table';
import {SplitButtonModule} from 'primeng/splitbutton';
import {ToolbarModule} from 'primeng/toolbar';
import {TooltipModule} from 'primeng/tooltip';
import {SelectModule} from 'primeng/select';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {FloatLabel} from 'primeng/floatlabel';
import {ButtonGroup} from 'primeng/buttongroup';
import {Toast} from 'primeng/toast';
import {finalize} from 'rxjs/operators';
import {forkJoin} from 'rxjs';
import {SORT} from '../../../../shared/util/command-item-sort';
import {CommandeService} from '../../../../entities/commande/commande.service';
import {DeliveryService} from '../../../../entities/commande/delevery/delivery.service';
import {ListLotComponent} from '../../ui/lot/list/list-lot.component';
import {FormLotComponent} from '../../ui/lot/form/form-lot.component';
import {EditProduitComponent} from '../../ui/delivery/edit-produit/edit-produit.component';
import {EtiquetteComponent} from '../../ui/delivery/etiquette/etiquette.component';
import {PutawayModalComponent} from '../../ui/delivery/putaway-modal/putaway-modal.component';
import {FileResponseModalComponent} from '../../ui/file-response-modal/file-response-modal.component';
import {CommandeResponseDialogComponent} from '../../ui/commande-response-dialog/commande-response-dialog.component';
import {IResponseCommande} from '../../../../shared/model/response-commande.model';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {SpinnerComponent} from '../../../../shared/spinner/spinner.component';
import {ReceptionConcordanceComponent} from '../../ui/reception-concordance/reception-concordance.component';
import {CommandeStatusBarComponent} from '../../ui/commande-status-bar/commande-status-bar.component';
import {PrixHistoriqueComponent} from '../../ui/prix-historique/prix-historique.component';
import {CommandeId} from '../../../../shared/model/abstract-commande.model';
import {Params} from '../../../../shared/model/enumerations/params.model';
import {IPutawayPreviewItem} from '../../../../entities/commande/commande.service';
import {ConfigurationService} from '../../../../shared/configuration.service';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';
import {handleBlobForTauri} from '../../../../shared/util/tauri-util';
import {IStockEntryResult} from '../../../../shared/model/stock-entry-result.model';
import {NotificationService} from '../../../../shared/services/notification.service';
import {NgbConfirmDialogService} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';

@Component({
  selector: 'app-commande-received',
  templateUrl: './commande-received.component.html',
  styleUrls: ['./commande-received.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    TagModule,
    TableModule,
    SplitButtonModule,
    ToolbarModule,
    TooltipModule,
    SelectModule,
    IconField,
    InputIcon,
    FloatLabel,
    ButtonGroup,
    Toast,
    SpinnerComponent,
    ReceptionConcordanceComponent,
    CommandeStatusBarComponent,

  ],
})
export class CommandeReceivedComponent implements OnInit {
  commande = input.required<ICommande>();
  commandeChange = output<ICommande | null>();
  retour = output<void>();

  protected orderLines: IOrderLine[] = [];
  protected search?: string;
  protected tris = 'UPDATE';
  protected selectedFilter = 'ALL';
  protected showLotBtn = false;
  protected currentCommande!: ICommande;
  protected filtres: any[];
  protected exportbuttons: MenuItem[];
  protected readonly sorts = SORT;

  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly commandeService = inject(CommandeService);
  private readonly deliveryService = inject(DeliveryService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);

  constructor() {
    this.filtres = [
      {label: "Prix d'achat differents", value: 'NOT_EQUAL'},
      {label: 'Code cip  à mettre à jour', value: 'PROVISOL_CIP'},
      {label: 'Tous', value: 'ALL'},
    ];
    this.exportbuttons = [
      {
        label: 'PDF',
        icon: 'pi pi-file-pdf',
        command: () => this.exportPdf(),
      },
      {
        label: 'CSV',
        icon: 'pi pi-file-excel',
        command: () => this.exportCSV(),
      },
    ];
  }

  ngOnInit(): void {
    this.currentCommande = this.commande();
    this.orderLines = this.currentCommande.orderLines ?? [];
    this.isLotActif();
  }

  protected previousState(): void {
    this.retour.emit();
  }

  protected onFilterCommandeLines(): void {
    const query = {
      commandeId: this.currentCommande.id,
      search: this.search,
      filterCommaneEnCours: this.selectedFilter,
      orderBy: this.tris,
    };
    this.commandeService.filterCommandeLines(query).subscribe(res => {
      this.orderLines = res.body!;
    });
  }

  protected onUpdateCip(orderLine: IOrderLine, event: any): void {
    const cip = event.target.value;
    if (this.currentCommande && cip !== '') {
      orderLine.produitCip = cip;
      this.commandeService.updateCip(orderLine).subscribe(() => this.refreshCommande());
    }
  }

  protected onUpdateFreeQtyRequested(orderLine: IOrderLine, event: any): void {
    const qty = Number(event.target.value);
    if (qty >= 0) {
      orderLine.freeQty = qty;
      this.orderLines = [...this.orderLines]; // force signal update in concordance panel
      this.commandeService.updateQuantityUG(orderLine).subscribe({
        next: () => this.refreshCommande(),
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur'),
      });
    }
  }

  protected onUpdateQuantityReceived(orderLine: IOrderLine, event: any): void {
    const qty = Number(event.target.value);
    if (qty >= 0) {
      orderLine.quantityReceived = qty;
      orderLine.quantityReceivedTmp = qty;
      this.orderLines = [...this.orderLines]; // force signal update in concordance panel
      this.deliveryService.updateQuantityReceived(orderLine).subscribe({
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur'),
      });
    }
  }

  protected computeAfterStock(ol: IOrderLine): number {
    return (ol.initStock ?? 0) + (ol.quantityReceivedTmp ?? ol.quantityRequested ?? 0) + (ol.freeQty ?? 0);
  }

  protected lineStatutSeverity(ol: IOrderLine): string {
    return this.lineStatut(ol).severity;
  }

  protected lineStatutLabel(ol: IOrderLine): string {
    return this.lineStatut(ol).label;
  }

  private lineStatut(ol: IOrderLine): {label: string; severity: string} {
    const rec = ol.quantityReceivedTmp ?? ol.quantityRequested ?? 0;
    const cmd = ol.quantityRequested ?? 0;
    if (rec === cmd) return {label: 'Servi',    severity: 'success'};
    if (rec === 0)   return {label: 'Rupture',  severity: 'danger'};
    if (rec > cmd)   return {label: 'Excédent', severity: 'info'};
    return              {label: 'Partiel',  severity: 'warn'};
  }

  protected onToutValider(): void {
    const allLines = this.currentCommande.orderLines ?? [];
    const linesToUpdate = allLines.filter(
      l => (l.quantityReceivedTmp ?? l.quantityRequested ?? 0) !== (l.quantityRequested ?? 0),
    );
    if (linesToUpdate.length === 0) return;
    this.confirmDialog.onConfirm(
      () => this.doToutValider(allLines, linesToUpdate),
      'Tout valider',
      `Marquer les ${linesToUpdate.length} ligne(s) en attente comme entièrement reçues ?\n\nLes quantités reçues seront égalisées aux quantités commandées.`,
    );
  }

  private doToutValider(allLines: IOrderLine[], linesToUpdate: IOrderLine[]): void {
    for (const l of allLines) {
      l.quantityReceived = l.quantityRequested ?? 0;
      l.quantityReceivedTmp = l.quantityRequested ?? 0;
    }
    this.selectedFilter = 'ALL';
    this.orderLines = [...allLines];
    forkJoin(linesToUpdate.map(l => this.deliveryService.updateQuantityReceived(l))).subscribe({
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur'),
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
      'Suppression',
      'Voullez-vous supprimer de la commande ce produit ?',
    );
  }

  protected onConfirmFinalize(): void {
    this.commandeService.checkPriceVariation(this.currentCommande.commandeId).subscribe({
      next: res => {
        const lignesAnomalie = res.body ?? [];
        if (lignesAnomalie.length > 0) {
          const detail = lignesAnomalie
            .map(l => `• ${l.produitLibelle} : commandé ${l.orderCostAmount} → actuel ${l.costAmount}`)
            .join('\n');
          this.confirmDialog.onConfirm(
            () => this.confirmFinalizeApresAlertesPrix(),
            'Variation de prix détectée',
            `${lignesAnomalie.length} ligne(s) ont un écart de prix dépassant le seuil configuré :\n${detail}\n\nVoulez-vous continuer malgré ces écarts ?`,
          );
        } else {
          this.confirmDialog.onConfirm(
            () => this.checkPutawayAndFinalize(),
            'Finalisation de la commande',
            "Voullez-vous faire l'entrée en stock ?",
          );
        }
      },
      error: () => {
        // En cas d'erreur de l'API de vérification, on laisse passer (fail-open)
        this.confirmDialog.onConfirm(
          () => this.checkPutawayAndFinalize(),
          'Finalisation de la commande',
          "Voullez-vous faire l'entrée en stock ?",
        );
      },
    });
  }

  private confirmFinalizeApresAlertesPrix(): void {
    this.confirmDialog.onConfirm(
      () => this.checkPutawayAndFinalize(),
      'Confirmation finale',
      "Confirmer l'entrée en stock malgré les écarts de prix ?",
    );
  }

  private checkPutawayAndFinalize(): void {
    const mode = this.configurationService.getParamByKey(Params.APP_PUTAWAY_MODE)?.value ?? 'MANUAL';
    if (mode !== 'MANUAL') {
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
          {
            items,
            header: `Répartition rayon → réserve (${items.length} produit(s))`,
          },
          (doTransfer: boolean) => this.onFinalize(doTransfer),
          'lg',
          null,
          () => this.onFinalize(false),
        );
      },
      error: () => this.onFinalize(false),
    });
  }

  protected onImporterReponseCommande(): void {
    showCommonModal(
      this.modalService,
      FileResponseModalComponent,
      {commandeSelected: this.currentCommande, header: 'IMPORTER RÉPONSE'},
      (responseCommande: IResponseCommande) => {
        if (responseCommande) {
          this.refreshCommande();
          this.openImporterReponseCommandeDialog(responseCommande);
        }
      },
      'lg',
    );
  }

  protected onAddLot(orderLine: IOrderLine): void {
    const qty = orderLine.quantityReceived || orderLine.quantityRequested;
    if (qty > 1 || orderLine.lots.length > 0) {
      showCommonModal(
        this.modalService,
        ListLotComponent,
        {
          deliveryItem: orderLine,
          header: `GESTION DE LOTS DE LA LIGNE ${orderLine.produitLibelle} [${orderLine.produitCip}]`
        },
        null,
        'lg',
      );
    } else {
      showCommonModal(
        this.modalService,
        FormLotComponent,
        {entity: null, deliveryItem: orderLine, header: 'Ajout de lot'},
        null,
        'lg',
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
        header: `EDITION DU PRODUIT ${orderLine.produitLibelle} [${orderLine.produitCip}]`,
      },
      null,
      'xl',
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
        header: `Historique des prix — ${orderLine.produitLibelle} [${orderLine.produitCip}]`,
      },
      null,
      'xl',
    );
  }

  protected orderLineTableColor(orderLine: IOrderLine): string {
    if (!orderLine) return '';
    if (orderLine.costAmount !== orderLine.orderCostAmount) return 'pharma-row-danger';
    if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) return 'pharma-row-warning';
    return '';
  }

  protected showLotColumn(): boolean {
    return this.showLotBtn ;
  }

  /** Nombre de lignes sans lot assigné (pertinent uniquement si APP_GESTION_LOT actif) */
  protected get lignesSansLot(): number {
    if (!this.showLotBtn) return 0;
    return this.orderLines.filter(l => l.lots.length === 0).length;
  }

  protected exportPdf(): void {
    this.commandeService.exportToPdf(this.currentCommande.commandeId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, 'commande_en_cours');
      } else {
        window.open(URL.createObjectURL(blob));
      }
    });
  }

  protected exportCSV(): void {
    this.commandeService.exportToCsv(this.currentCommande.commandeId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, 'commande_en_cours', 'csv');
      } else {
        saveAs(blob);
      }
    });
  }

  private isLotActif(): void {
    const param = this.configurationService.getParamByKey(Params.APP_GESTION_LOT);
    if (param) this.showLotBtn = Number(param.value) === 0;
  }

  private openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.currentCommande;
  }

  private refreshCommande(): void {
    this.commandeService.find(this.currentCommande.commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande.orderLines;
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private onFinalize(doTransfer = false): void {
    this.spinner().show();
    this.deliveryService
      .finalizeSaisieEntreeStock({...this.currentCommande, doTransfer})
      .pipe(finalize(() => this.spinner().hide()))
      .subscribe({
        next: (res: HttpResponse<IStockEntryResult>) => {
          this.confirmPrintTicket(res.body.commandeId);
        },
        error: error => {
          this.notificationService.error(this.errorService.getErrorMessage(error), 'Erreur');
        },
      });
  }

  private confirmPrintTicket(commandeId: CommandeId): void {
    this.confirmDialog.onConfirm(
      () => this.printEtiquette({...this.currentCommande, commandeId}),
      'Impression',
      'Voullez-vous imprimer les étiquettes ?',
      null,
      () => {
        this.currentCommande = null;
        this.commandeChange.emit(null);
        this.previousState();
      },
    );
  }

  private printEtiquette(commande: ICommande): void {
    showCommonModal(
      this.modalService,
      EtiquetteComponent,
      {entity: commande, header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${commande.receiptReference} ] `},
      () => {
        this.currentCommande = null;
        this.commandeChange.emit(null);
        this.previousState();
      },
      'lg',
      null,
      () => {
        this.currentCommande = null;
        this.commandeChange.emit(null);
        this.previousState();
      },
    );
  }
}
