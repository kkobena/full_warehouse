import { Component, inject, OnInit } from '@angular/core';
import { MonthEnum } from '../../../../shared/model/enumerations/month-enum';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import {
  HistoriqueProduitAchatsSummary,
  HistoriqueProduitDonneesMensuelles,
  ProduitAuditingParam
} from '../../../../shared/model/produit-record.model';
import { ProduitStatService } from '../../stat/produit-stat.service';
import { ProduitAuditingParamService } from '../../transaction/produit-auditing-param.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { LazyLoadEvent, PrimeTemplate } from 'primeng/api';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'jhi-yearly-delevery-produit-historique',
  imports: [CommonModule, PrimeTemplate, TableModule],
  templateUrl: './yearly-delevery-produit-historique.component.html',
  styles: ``
})
export class YearlyDeleveryProduitHistoriqueComponent implements OnInit {
  protected readonly JANUARY = MonthEnum.JANUARY;
  protected readonly FEBRUARY = MonthEnum.FEBRUARY;
  protected readonly MARCH = MonthEnum.MARCH;
  protected readonly APRIL = MonthEnum.APRIL;
  protected readonly MAY = MonthEnum.MAY;
  protected readonly JUNE = MonthEnum.JUNE;
  protected readonly JULY = MonthEnum.JULY;
  protected readonly AUGUST = MonthEnum.AUGUST;
  protected readonly SEPTEMBER = MonthEnum.SEPTEMBER;
  protected readonly OCTOBER = MonthEnum.OCTOBER;
  protected readonly NOVEMBER = MonthEnum.NOVEMBER;
  protected readonly DECEMBER = MonthEnum.DECEMBER;
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected loading!: boolean;
  protected page = 0;
  protected data: HistoriqueProduitDonneesMensuelles[] = [];
  protected summary: HistoriqueProduitAchatsSummary | null = null;
  private readonly produitStatService = inject(ProduitStatService);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);

  ngOnInit() {
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
      .getProduitHistoriqueAchatMensuelle({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...produitAuditingParam
      })
      .subscribe({
        next: (res: HttpResponse<HistoriqueProduitDonneesMensuelles[]>) => this.onSuccess(res.body, res.headers, pageToLoad)
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.produitStatService
        .getProduitHistoriqueAchatMensuelle({
          page: this.page,
          size: event.rows,
          ...this.produitAuditingParamService.produitAuditingParam
        })
        .subscribe({
          next: (res: HttpResponse<HistoriqueProduitDonneesMensuelles[]>) => this.onSuccess(res.body, res.headers, this.page)
        });
    }
  }

  exportPdf(produitAuditingParam: ProduitAuditingParam): void {
    this.produitStatService.exportHistoriqueAchatMensuelToPdf(produitAuditingParam).subscribe({
      next: blod => {
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      }
    });
  }

  protected getHistoriqueAchatSummary(produitAuditingParam: ProduitAuditingParam): void {
    this.produitStatService.getHistoriqueAchatSummary(produitAuditingParam).subscribe({
      next: res => {
        this.summary = res.body;
      }
    });
  }

  protected getMonthData(record: HistoriqueProduitDonneesMensuelles, month: MonthEnum): number {
    const monthValue = record.quantites[month];
    return monthValue ? Number(monthValue) : null;
  }

  private onSuccess(data: HistoriqueProduitDonneesMensuelles[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data;
    this.loading = false;
  }
}
