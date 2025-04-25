import { Component, inject, OnInit, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { NgbNav } from '@ng-bootstrap/ng-bootstrap';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';
import { AuditingComponent } from '../auditing/auditing.component';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';
import moment from 'moment/moment';
import { IProduit } from '../../../shared/model/produit.model';
import { ProduitService } from '../produit.service';

import { HttpResponse } from '@angular/common/http';
import { ProduitAuditingParamService } from './produit-auditing-param.service';
import { BadgeModule } from 'primeng/badge';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../../shared/constants/pagination.constants';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { RippleModule } from 'primeng/ripple';
import { DatePicker } from 'primeng/datepicker';
import { StatSalesComponent } from '../stat-sales/stat-sales.component';
import { StatDeliveryComponent } from '../stat-delivery/stat-delivery.component';

@Component({
  selector: 'jhi-transaction',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    CardModule,
    DividerModule,
    InputTextModule,
    NgbNav,
    ToolbarModule,
    FormsModule,
    RouterModule,
    PanelModule,
    AuditingComponent,
    BadgeModule,
    AutoCompleteModule,
    RippleModule,
    DatePicker,
    StatSalesComponent,
    StatDeliveryComponent,
  ],
  templateUrl: './transaction.component.html',
  providers: [ProduitAuditingParamService],
})
export class TransactionComponent implements OnInit {
  readonly auditingComponent = viewChild(AuditingComponent);
  readonly statSalesComponent = viewChild(StatSalesComponent);
  readonly delivery = viewChild(StatDeliveryComponent);
  protected active = 'auditing';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected produit: IProduit | null = null;
  protected produits: IProduit[] = [];
  protected event: any;
  protected searchValue?: string;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly produitService = inject(ProduitService);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => {
      if (produit?.id) {
        this.produit = produit;
        const params: ProduitAuditingParam = this.buildQuery();
        this.produitAuditingParamService.setParameter(params);
      }
    });

    this.loadProduits();
  }

  load(): void {
    this.loadData();
  }

  exportPdf(): void {
    switch (this.active) {
      case 'auditing':
        this.auditingComponent().exportPdf(this.buildQuery());
        break;
      case 'sales':
        this.statSalesComponent().exportPdf(this.buildQuery());
        break;
      case 'commande':
        this.delivery().exportPdf(this.buildQuery());
        break;
    }
  }

  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 10,
        withdetail: true,
        search: this.searchValue,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => this.onProduitSuccess(res.body));
  }

  onSelect(event: any): void {
    this.event = event;
    this.loadData();
  }

  searchFn(event: any): void {
    this.searchValue = event.query;
    this.loadProduits();
  }

  previousState(): void {
    window.history.back();
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  protected buildQuery(): ProduitAuditingParam {
    return {
      produitId: this.produit.id,
      fromDate: this.fromDate ? moment(this.fromDate).format('yyyy-MM-DD') : null,
      toDate: this.toDate ? moment(this.toDate).format('yyyy-MM-DD') : null,
    };
  }

  protected updateParam(): void {
    this.produitAuditingParamService.setParameter(this.buildQuery());
  }

  private loadData(): void {
    const params: ProduitAuditingParam = this.buildQuery();
    this.produitAuditingParamService.setParameter(params);
    switch (this.active) {
      case 'auditing':
        this.auditingComponent().load(params);
        break;
      case 'sales':
        this.statSalesComponent().load(params);
        break;
      case 'commande':
        this.delivery().load(params);
        break;
    }
  }
}
