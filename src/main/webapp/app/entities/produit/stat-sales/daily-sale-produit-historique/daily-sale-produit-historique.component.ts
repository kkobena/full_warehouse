import { Component, inject, OnInit } from '@angular/core';
import { ProduitStatService } from '../../stat/produit-stat.service';
import {
  HistoriqueProduitVente,
  HistoriqueProduitVenteSummary,
  ProduitAuditingParam
} from '../../../../shared/model/produit-record.model';
import { ProduitAuditingParamService } from '../../transaction/produit-auditing-param.service';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { CommonModule, DatePipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';

@Component({
  selector: 'jhi-daily-sale-produit-historique',
  imports: [CommonModule, DatePipe, TableModule],
  templateUrl: './daily-sale-produit-historique.component.html'
})
export class DailySaleProduitHistoriqueComponent implements OnInit {
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected loading!: boolean;
  protected page = 0;
  protected data: HistoriqueProduitVente[] = [];
  protected summary: HistoriqueProduitVenteSummary | null = null;
  private readonly produitStatService = inject(ProduitStatService);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);

  ngOnInit(): void {
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
      .getProduitHistoriqueVente({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...produitAuditingParam
      })
      .subscribe({
        next: (res: HttpResponse<HistoriqueProduitVente[]>) => this.onSuccess(res.body, res.headers, pageToLoad)
      });
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.produitStatService
        .getProduitHistoriqueVente({
          page: this.page,
          size: event.rows,
          ...this.produitAuditingParamService.produitAuditingParam
        })
        .subscribe({
          next: (res: HttpResponse<HistoriqueProduitVente[]>) => this.onSuccess(res.body, res.headers, this.page)
        });
    }
  }

  exportPdf(produitAuditingParam: ProduitAuditingParam): void {
    this.produitStatService.exportHistoriqueVenteToPdf(produitAuditingParam).subscribe({
      next: blod => {
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      }
    });
  }

  protected getSalesSummary(produitAuditingParam: ProduitAuditingParam): void {
    this.produitStatService.getHistoriqueVenteSummary(produitAuditingParam).subscribe({
      next: res => {
        this.summary = res.body;
      }
    });
  }

  private onSuccess(data: HistoriqueProduitVente[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data || [];
    this.loading = false;
  }
}
