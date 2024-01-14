import { Component, OnInit } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { BadgeModule } from 'primeng/badge';
import { DividerModule } from 'primeng/divider';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ProduitAuditingParam, ProduitAuditingState } from '../../../shared/model/produit-record.model';
import { HttpResponse } from '@angular/common/http';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from '../../../shared/util/warehouse-util';
import { saveAs } from 'file-saver';
import { ProduitStatService } from '../stat/produit-stat.service';
import { ProduitAuditingParamService } from '../transaction/produit-auditing-param.service';

@Component({
  selector: 'jhi-auditing',
  standalone: true,
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
  styleUrl: './auditing.component.scss',
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

  constructor(
    protected produitStatService: ProduitStatService,
    private spinner: NgxSpinnerService,
    private produitAuditingParamService: ProduitAuditingParamService,
  ) {
    console.warn(this.produitAuditingParamService.produitAuditingParam);
  }

  ngOnInit(): void {
    this.load(this.produitAuditingParamService.produitAuditingParam);
  }

  load(produitAuditingParam: ProduitAuditingParam): void {
    this.spinner.show();
    this.produitStatService.fetchTransactions(produitAuditingParam).subscribe({
      next: (res: HttpResponse<ProduitAuditingState[]>) => this.onSuccessPage(res.body),
      error: err => this.onError(err),
    });
  }

  exportPdf(produitAuditingParam: ProduitAuditingParam): void {
    this.spinner.show();

    this.produitStatService.exportToPdf(produitAuditingParam).subscribe({
      next: blod => {
        const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
        saveAs(blod, 'suivi_mvt_article_' + fileName);
        this.spinner.hide();
      },
      error: () => this.spinner.hide(),
    });
  }

  protected onError(eror: any): void {
    this.spinner.hide();
  }

  protected onSuccessPage(data: ProduitAuditingState[] | null): void {
    this.spinner.hide();
    this.entites = data || [];
    this.computeTotaux();
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
    if (this.entites?.length > 0) {
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
