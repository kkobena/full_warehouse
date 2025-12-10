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
import { IProduit } from '../../../shared/model/produit.model';
import { ProduitAuditingParamService } from './produit-auditing-param.service';
import { BadgeModule } from 'primeng/badge';
import {
  APPEND_TO,
  PRODUIT_COMBO_MIN_LENGTH,
  PRODUIT_COMBO_RESULT_SIZE,
  PRODUIT_NOT_FOUND,
} from '../../../shared/constants/pagination.constants';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { RippleModule } from 'primeng/ripple';
import { StatSalesComponent } from '../stat-sales/stat-sales.component';
import { StatDeliveryComponent } from '../stat-delivery/stat-delivery.component';
import RecapProduitVenduComponent from '../../reports/recap-produit-vendu/recap-produit-vendu.component';

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
    StatSalesComponent,
    StatDeliveryComponent,
    RecapProduitVenduComponent,
  ],
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.scss'],
  providers: [ProduitAuditingParamService],
})
export class TransactionComponent implements OnInit {
  readonly delivery = viewChild(StatDeliveryComponent);
  protected active = 'auditing';
  protected produit: IProduit | null = null;
  protected produits: IProduit[] = [];
  protected event: any;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => {
      if (produit?.id) {
        this.produit = produit;
        const params: ProduitAuditingParam = this.buildQuery();
        this.produitAuditingParamService.setParameter(params);
      }
    });
  }

  protected buildQuery(): ProduitAuditingParam {
    const params: ProduitAuditingParam = {
      produitId: this.produit.id,
    };
    this.produitAuditingParamService.setParameter(params);
    return params;
  }
}
