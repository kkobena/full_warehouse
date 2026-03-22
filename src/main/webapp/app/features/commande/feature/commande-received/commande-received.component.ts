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
import {SORT} from '../../../../shared/util/command-item-sort';
import {CommandeService} from '../../../../entities/commande/commande.service';
import {DeliveryService} from '../../../../entities/commande/delevery/delivery.service';
import {ListLotComponent} from '../../ui/lot/list/list-lot.component';
import {FormLotComponent} from '../../ui/lot/form/form-lot.component';
import {EditProduitComponent} from '../../ui/delivery/edit-produit/edit-produit.component';
import {EtiquetteComponent} from '../../ui/delivery/etiquette/etiquette.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {SpinnerComponent} from '../../../../shared/spinner/spinner.component';
import {ReceptionConcordanceComponent} from '../../ui/reception-concordance/reception-concordance.component';
import {CommandeStatusBarComponent} from '../../ui/commande-status-bar/commande-status-bar.component';
import {PrixHistoriqueComponent} from '../../ui/prix-historique/prix-historique.component';
import {CommandeId} from '../../../../shared/model/abstract-commande.model';
import {Params} from '../../../../shared/model/enumerations/params.model';
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
    window.history.back();
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
      this.commandeService.updateQuantityUG(orderLine).subscribe({
        next: () => this.refreshCommande(),
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur'),
      });
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
            () => this.onFinalize(),
            'Finalisation de la commande',
            "Voullez-vous faire l'entrée en stock ?",
          );
        }
      },
      error: () => {
        // En cas d'erreur de l'API de vérification, on laisse passer (fail-open)
        this.confirmDialog.onConfirm(
          () => this.onFinalize(),
          'Finalisation de la commande',
          "Voullez-vous faire l'entrée en stock ?",
        );
      },
    });
  }

  private confirmFinalizeApresAlertesPrix(): void {
    this.confirmDialog.onConfirm(
      () => this.onFinalize(),
      'Confirmation finale',
      "Confirmer l'entrée en stock malgré les écarts de prix ?",
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
    return this.showLotBtn || this.orderLines.some(l => l.lots.length > 0);
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

  private refreshCommande(): void {
    this.commandeService.find(this.currentCommande.commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande.orderLines;
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private onFinalize(): void {
    this.spinner().show();
    this.deliveryService
      .finalizeSaisieEntreeStock(this.currentCommande)
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
