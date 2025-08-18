import { HttpResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from 'app/shared/constants/pagination.constants';
import moment from 'moment';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ToolbarModule } from 'primeng/toolbar';
import { CalendarModule } from 'primeng/calendar';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { BadgeModule } from 'primeng/badge';
import { ProduitStatService } from './stat/produit-stat.service';
import { ProduitAuditingParam, ProduitAuditingState } from '../../shared/model/produit-record.model';
import { DividerModule } from 'primeng/divider';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from '../../shared/util/warehouse-util';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-produit-detail',
  templateUrl: './produit-detail.component.html',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    PanelModule,
    AutoCompleteModule,
    ToolbarModule,
    CalendarModule,
    TableModule,
    BadgeModule,
    DividerModule,
    NgxSpinnerModule
  ]
})
export class ProduitDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  protected produitStatService = inject(ProduitStatService);
  protected produitService = inject(ProduitService);
  private spinner = inject(NgxSpinnerService);

  produit: IProduit | null = null;
  produits: IProduit[] = [];
  event: any;
  searchValue?: string;
  entites: ProduitAuditingState[] = [];
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
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

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => {
      if (produit?.id) {
        this.produit = produit;
        this.loadPage();
      }
    });

    this.loadProduits();
  }

  previousState(): void {
    window.history.back();
  }

  onSelect(event: any): void {
    this.event = event;
    this.loadPage();
  }

  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 10,
        withdetail: false,
        search: this.searchValue
      })
      .subscribe((res: HttpResponse<IProduit[]>) => this.onProduitSuccess(res.body));
  }

  searchFn(event: any): void {
    this.searchValue = event.query;
    this.loadProduits();
  }

  loadPage(): void {
    this.spinner.show();
    this.produitStatService.fetchTransactions(this.buildQuery()).subscribe({
      next: (res: HttpResponse<ProduitAuditingState[]>) => this.onSuccessPage(res.body),
      error: err => this.onError(err)
    });
  }

  exportPdf(): void {
    this.spinner.show();

    this.produitStatService.exportToPdf(this.buildQuery()).subscribe({
      next: blod => {
        const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
        //   saveAs(blod, 'suivi_mvt_article_' + fileName);
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
        this.spinner.hide();
      },
      error: () => this.spinner.hide()
    });
  }

  protected onSuccessPage(data: ProduitAuditingState[] | null): void {
    this.spinner.hide();
    this.entites = data || [];
    this.computeTotaux();
  }

  protected onError(eror: any): void {
    this.spinner.hide();
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  private buildQuery(): ProduitAuditingParam {
    return {
      produitId: this.produit.id,
      fromDate: this.fromDate ? moment(this.fromDate).format('yyyy-MM-DD') : null,
      toDate: this.toDate ? moment(this.toDate).format('yyyy-MM-DD') : null
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
    this.resetTotaux();
    if (this.entites.length > 0) {
      for (const e of this.entites) {
        console.warn(e);
        if (e.saleQuantity) {
          this.saleQuantity = this.saleQuantity + e.saleQuantity;
          console.error(this.saleQuantity);
        }
        if (e.retourFournisseurQuantity) {
          this.retourFournisseurQuantity = this.retourFournisseurQuantity + e.retourFournisseurQuantity;
        }
        if (e.deleveryQuantity) {
          this.deleveryQuantity = this.deleveryQuantity + e.deleveryQuantity;
        }
        if (e.canceledQuantity) {
          this.canceledQuantity = this.canceledQuantity + e.canceledQuantity;
        }
        if (e.perimeQuantity) {
          this.perimeQuantity = this.perimeQuantity + e.perimeQuantity;
        }
        if (e.ajustementPositifQuantity) {
          this.ajustementPositifQuantity = this.ajustementPositifQuantity + e.ajustementPositifQuantity;
        }
        if (e.ajustementNegatifQuantity) {
          this.ajustementNegatifQuantity = this.ajustementNegatifQuantity + e.ajustementNegatifQuantity;
        }
        if (e.deconPositifQuantity) {
          this.deconPositifQuantity = this.deconPositifQuantity + e.deconPositifQuantity;
        }
        if (e.deconNegatifQuantity) {
          this.deconNegatifQuantity = this.deconNegatifQuantity + e.deconNegatifQuantity;
        }
      }
    }
  }
}
