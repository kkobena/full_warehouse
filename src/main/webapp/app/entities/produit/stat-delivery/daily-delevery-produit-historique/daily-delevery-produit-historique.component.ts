import { Component, inject, OnInit } from '@angular/core';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import {
  HistoriqueProduitAchats,
  HistoriqueProduitAchatsSummary,
  ProduitAuditingParam,
} from '../../../../shared/model/produit-record.model';
import { ProduitStatService } from '../../stat/produit-stat.service';
import { ProduitAuditingParamService } from '../../transaction/produit-auditing-param.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { LazyLoadEvent } from 'primeng/api';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'jhi-daily-delevery-produit-historique',
  imports: [CommonModule, TableModule],
  templateUrl: './daily-delevery-produit-historique.component.html',
})
export class DailyDeleveryProduitHistoriqueComponent implements OnInit {
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected loading!: boolean;
  protected page = 0;
  protected data: HistoriqueProduitAchats[] = [];
  protected summary: HistoriqueProduitAchatsSummary | null = null;
  private readonly produitStatService = inject(ProduitStatService);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);

  ngOnInit(): void {
    this.load(this.produitAuditingParamService.produitAuditingParam);
  }

  load(produitAuditingParam: ProduitAuditingParam): void {
    this.loadPage(produitAuditingParam);
    this.getHistoriqueAchatSummary(produitAuditingParam);
  }

  loadPage(produitAuditingParam: ProduitAuditingParam, page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.produitStatService
      .getProduitHistoriqueAchat({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...produitAuditingParam,
      })
      .subscribe({
        next: (res: HttpResponse<HistoriqueProduitAchats[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.produitStatService
        .getProduitHistoriqueAchat({
          page: this.page,
          size: event.rows,
          ...this.produitAuditingParamService.produitAuditingParam,
        })
        .subscribe({
          next: (res: HttpResponse<HistoriqueProduitAchats[]>) => this.onSuccess(res.body, res.headers, this.page),
        });
    }
  }

  protected getHistoriqueAchatSummary(produitAuditingParam: ProduitAuditingParam): void {
    this.produitStatService.getHistoriqueAchatSummary(produitAuditingParam).subscribe({
      next: res => {
        this.summary = res.body;
      },
    });
  }

  private onSuccess(data: HistoriqueProduitAchats[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data || [];
    this.loading = false;
  }
}
