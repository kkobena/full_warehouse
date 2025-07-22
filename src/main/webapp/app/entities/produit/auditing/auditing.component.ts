import {Component, inject, OnInit} from '@angular/core';
import {WarehouseCommonModule} from '../../../shared/warehouse-common/warehouse-common.module';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {FormsModule} from '@angular/forms';
import {PanelModule} from 'primeng/panel';
import {TableModule} from 'primeng/table';
import {BadgeModule} from 'primeng/badge';
import {DividerModule} from 'primeng/divider';
import {NgxSpinnerModule} from 'ngx-spinner';
import {
  ProduitAuditingParam,
  ProduitAuditingState,
  ProduitAuditingSum
} from '../../../shared/model/produit-record.model';
import {HttpHeaders, HttpResponse} from '@angular/common/http';
import {DATE_FORMAT_DD_MM_YYYY_HH_MM_SS} from '../../../shared/util/warehouse-util';
import {ProduitStatService} from '../stat/produit-stat.service';
import {ProduitAuditingParamService} from '../transaction/produit-auditing-param.service';
import {LotPerimes} from "../../gestion-peremption/model/lot-perimes";
import {ITEMS_PER_PAGE} from "../../../shared/constants/pagination.constants";
import {LazyLoadEvent} from "primeng/api";
import {MouvementProduit} from "../../../shared/model/enumerations/mouvement-produit.model";

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
  ],
  templateUrl: './auditing.component.html',
})
export class AuditingComponent implements OnInit {
  protected saleQuantity?: number;
  protected deleveryQuantity?: number;
  protected retourFournisseurQuantity?: number;
  protected perimeQuantity?: number;
  protected ajustementPositifQuantity?: number;
  protected ajustementNegatifQuantity?: number;
  protected deconPositifQuantity?: number;
  protected deconNegatifQuantity?: number;
  protected canceledQuantity?: number;
  protected retourDepot?: number;
  protected storeInventoryQuantity?: number;
  protected entites: ProduitAuditingState[] = [];
  protected summaries: ProduitAuditingSum[] = [];
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected loading!: boolean;
  protected totalItems = 0;
  private readonly produitStatService = inject(ProduitStatService);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);
  ngOnInit(): void {
    const param = this.produitAuditingParamService.produitAuditingParam;
    if (param && param.produitId) {
      this.loadPage();
      this.fetchSum();
    }
  }

  load(): void {
    this.loadPage();
   this.fetchSum();
  }

  fetchSum(): void {
    this.produitStatService.fetchTransactionsSum(this.buidParams()).subscribe({
      next: (res: HttpResponse<ProduitAuditingSum[]>) => {
        this.summaries = res.body || [];
        this.computeTotaux();
      },
      error: () => {
        this.summaries= [];
      }
    });
  }

  exportPdf(): void {
    this.produitStatService.exportToPdf(this.buidParams()).subscribe({
      next: blod => {
        const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
        // saveAs(blod, 'suivi_mvt_article_' + fileName);
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);

      }

    });
  }
  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.produitStatService
        .fetchTransactions({
          page: this.page,
          size: event.rows,
          ...this.buidParams()
        })
        .subscribe({
          next: (res: HttpResponse<ProduitAuditingState[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: (err) => this.onError(err)
        });
    }
  }
  private onError(eror: any): void {

  }

  private onSuccessPage(data: ProduitAuditingState[] | null): void {

    this.entites = data || [];
    this.computeTotaux();
  }

  private onSuccess(data: ProduitAuditingState[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.entites = data || [];
    this.loading = false;
  }

  private loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.produitStatService
      .fetchTransactions({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        ...this.buidParams()
      })
      .subscribe({
        next: (res: HttpResponse<ProduitAuditingState[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: (err) => this.onError(err),
      });
  }
  private buidParams(): ProduitAuditingParam {
    const param = this.produitAuditingParamService.produitAuditingParam;
    return {
      produitId: param.produitId,
      fromDate: param.fromDate,
      toDate: param.toDate,
      page: this.page,
      size: this.itemsPerPage
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
    const deliverySum = this.summaries.find(sum => sum.mouvementProduitType === MouvementProduit.COMMANDE);
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
