import { Component, inject } from '@angular/core';
import {
  HistoriqueProduitVDonneesMensuelles,
  HistoriqueProduitVenteSummary,
  ProduitAuditingParam,
} from '../../../../shared/model/produit-record.model';
import { ProduitStatService } from '../../stat/produit-stat.service';
import { ProduitAuditingParamService } from '../../transaction/produit-auditing-param.service';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { LazyLoadEvent } from 'primeng/api';

@Component({
  selector: 'jhi-hearly-sale-produit-historique',
  imports: [],
  templateUrl: './yearly-sale-produit-historique.component.html',
  styles: ``,
})
export class YearlySaleProduitHistoriqueComponent {
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected loading!: boolean;
  protected page = 0;
  protected data: HistoriqueProduitVDonneesMensuelles[] = [];
  protected summary: HistoriqueProduitVenteSummary | null = null;
  private readonly produitStatService = inject(ProduitStatService);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);

  constructor() {
    this.load(this.produitAuditingParamService.produitAuditingParam);
  }

  load(produitAuditingParam: ProduitAuditingParam): void {
    this.loadPage(produitAuditingParam);
    this.getSalesSummary(produitAuditingParam);
  }

  loadPage(produitAuditingParam: ProduitAuditingParam, page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.produitStatService
      .getProduitHistoriqueVenteMensuelle({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...produitAuditingParam,
      })
      .subscribe({
        next: (res: HttpResponse<HistoriqueProduitVDonneesMensuelles[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.produitStatService
        .getProduitHistoriqueVenteMensuelle({
          page: this.page,
          size: event.rows,
          ...this.produitAuditingParamService.produitAuditingParam,
        })
        .subscribe({
          next: (res: HttpResponse<HistoriqueProduitVDonneesMensuelles[]>) => this.onSuccess(res.body, res.headers, this.page),
        });
    }
  }

  protected getSalesSummary(produitAuditingParam: ProduitAuditingParam): void {
    this.produitStatService.getHistoriqueVenteSummary(produitAuditingParam).subscribe({
      next: res => {
        this.summary = res.body || null;
      },
    });
  }

  private onSuccess(data: HistoriqueProduitVDonneesMensuelles[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data || [];
    this.loading = false;
  }
}
