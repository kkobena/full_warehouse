import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { BadgeModule } from 'primeng/badge';
import { DividerModule } from 'primeng/divider';
import { NgxSpinnerModule } from 'ngx-spinner';
import { ProduitAuditingParam, ProduitAuditingState, ProduitAuditingSum } from '../../../shared/model/produit-record.model';
import { HttpResponse } from '@angular/common/http';
import { ProduitStatService } from '../stat/produit-stat.service';
import { ProduitAuditingParamService } from '../transaction/produit-auditing-param.service';
import { ITEMS_PER_PAGE, PRODUIT_COMBO_RESULT_SIZE } from '../../../shared/constants/pagination.constants';
import { MouvementProduit } from '../../../shared/model/enumerations/mouvement-produit.model';
import { MagasinService } from '../../magasin/magasin.service';
import { DatePickerComponent } from '../../../shared/date-picker/date-picker.component';
import { ProduitAutocompleteComponent } from '../../../shared/produit-autocomplete/produit-autocomplete.component';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { IProduit } from '../../../shared/model/produit.model';

@Component({
  selector: 'jhi-auditing',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    PanelModule,
    TableModule,
    BadgeModule,
    DividerModule,
    NgxSpinnerModule,
    DatePickerComponent,
    ProduitAutocompleteComponent,
    Toolbar,
    Tooltip,
  ],
  templateUrl: './auditing.component.html',
  styleUrl: './auditing.component.scss',
})
export class AuditingComponent implements OnInit {
  protected saleQuantity?: number;
  protected deleveryQuantity?: number;
  protected retourDepot?: number;
  protected retourFournisseurQuantity?: number;
  protected perimeQuantity?: number;
  protected ajustementPositifQuantity?: number;
  protected ajustementNegatifQuantity?: number;
  protected deconPositifQuantity?: number;
  protected deconNegatifQuantity?: number;
  protected canceledQuantity?: number;
  protected storeInventoryQuantity?: number;
  protected entites: ProduitAuditingState[] = [];
  protected summaries: ProduitAuditingSum[] = [];
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected loading!: boolean;
  protected totalItems = 0;
  protected defaultDate: Date = new Date();
  protected produit: IProduit | null = null;
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected produits: IProduit[] = [];
  protected event: any;
  protected hasDepot = signal<boolean>(false);
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  private readonly produitStatService = inject(ProduitStatService);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);
  private readonly magasinService = inject(MagasinService);
  private readonly dateDebut = viewChild<DatePickerComponent>('dateDebut');
  private readonly dateFin = viewChild<DatePickerComponent>('dateFin');
  /*
  protected get fromDate(): Date | null {
    return this.dateDebut().value;
  }

  protected get toDate(): Date | null {
    return this.dateFin().value;
  }
*/
  onSelect(event: any): void {
    this.event = event;
    this.load();
  }

  onClear(event: any): void {
    this.resetData();
  }

  ngOnInit(): void {
    this.magasinService.hasDepot().subscribe(hasDepot => {
      this.hasDepot.set(hasDepot.body || false);
    });
    const param = this.produitAuditingParamService.produitAuditingParam;
    if (param && param.produitId) {
      this.loadPage();
      this.fetchSum();
    } else {
      this.resetData();
    }
  }

  resetData(): void {
    this.entites = [];
    this.summaries = [];
    this.resetTotaux();
  }

  load(): void {
    this.loadPage();
    this.fetchSum();
  }

  fetchSum(): void {
    this.produitStatService.fetchTransactionsSum(this.buildQuery()).subscribe({
      next: (res: HttpResponse<ProduitAuditingSum[]>) => {
        this.summaries = res.body || [];
        this.computeTotaux();
      },
      error: () => {
        this.summaries = [];
      },
    });
  }

  exportPdf(): void {
    this.produitStatService.exportToPdf(this.buildQuery()).subscribe({
      next(blod) {
        // const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
        // saveAs(blod, 'suivi_mvt_article_' + fileName);
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
    });
  }

  protected buildQuery(): ProduitAuditingParam {
    const params: ProduitAuditingParam = {
      produitId: this.produit?.id,
      fromDate: this.dateDebut()?.submitValue,
      toDate: this.dateFin()?.submitValue,
    };
    this.produitAuditingParamService.setParameter(params);
    return params;
  }

  protected updateParam(): void {
    this.produitAuditingParamService.setParameter(this.buildQuery());
  }

  private onError(): void {
    this.entites = [];
  }

  private onSuccessPage(data: ProduitAuditingState[] | null): void {
    this.entites = data || [];
    this.computeTotaux();
  }

  private onSuccess(data: ProduitAuditingState[] | null): void {
    this.entites = data || [];
    this.loading = false;
  }

  private loadPage(): void {
    this.produitStatService
      .fetchTransactions({
        ...this.buildQuery(),
      })
      .subscribe({
        next: (res: HttpResponse<ProduitAuditingState[]>) => this.onSuccess(res.body),
        error: err => this.onError(),
      });
  }

  private buidParamscc(): ProduitAuditingParam {
    const param = this.buildQuery();
    return {
      produitId: param.produitId,
      fromDate: param.fromDate,
      toDate: param.toDate,
      page: this.page,
      size: this.itemsPerPage,
    };
  }

  private resetTotaux(): void {
    this.saleQuantity = null;
    this.deleveryQuantity = null;
    this.retourFournisseurQuantity = null;
    this.perimeQuantity = null;
    this.ajustementPositifQuantity = null;
    this.ajustementNegatifQuantity = null;
    this.deconPositifQuantity = null;
    this.deconNegatifQuantity = null;
    this.canceledQuantity = null;
    this.retourDepot = null;
    this.storeInventoryQuantity = null;
  }

  private computeTotaux(): void {
    const saleSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.SALE);
    this.saleQuantity = saleSum ? saleSum.quantity : null;
    const deliverySum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.ENTREE_STOCK);
    this.deleveryQuantity = deliverySum ? deliverySum.quantity : null;
    const retourFournisseurSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.RETOUR_FOURNISSEUR);
    this.retourFournisseurQuantity = retourFournisseurSum ? retourFournisseurSum.quantity : null;
    const perimeSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.RETRAIT_PERIME);
    this.perimeQuantity = perimeSum ? perimeSum.quantity : null;
    const ajustementPositifSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.AJUSTEMENT_IN);
    this.ajustementPositifQuantity = ajustementPositifSum ? ajustementPositifSum.quantity : null;
    const ajustementNegatifSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.AJUSTEMENT_OUT);
    this.ajustementNegatifQuantity = ajustementNegatifSum ? ajustementNegatifSum.quantity : null;
    const deconPositifSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.DECONDTION_IN);
    this.deconPositifQuantity = deconPositifSum ? deconPositifSum.quantity : null;
    const deconNegatifSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.MOUVEMENT_STOCK_OUT);
    this.deconNegatifQuantity = deconNegatifSum ? deconNegatifSum.quantity : null;
    const canceledSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.CANCEL_SALE);
    this.canceledQuantity = canceledSum ? canceledSum.quantity : null;
    const retourDepotSum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.RETOUR_DEPOT);
    this.retourDepot = retourDepotSum ? retourDepotSum.quantity : null;
    const storeInventorySum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.INVENTAIRE);
    this.storeInventoryQuantity = storeInventorySum ? storeInventorySum.quantity : null;
  }
}
