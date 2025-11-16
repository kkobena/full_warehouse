import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
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
import { DatePickerComponent } from '../../../shared/date-picker/date-picker.component';
import { ProduitAutocompleteComponent } from '../../../shared/produit-autocomplete/produit-autocomplete.component';

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
    DatePickerComponent,
    ProduitAutocompleteComponent,
  ],
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.scss'],
  providers: [ProduitAuditingParamService],
})
export class TransactionComponent implements OnInit, AfterViewInit {
  ngAfterViewInit(): void {
    this.dateDebut().value = this.defaultDate;
    this.dateFin().value = this.defaultDate;
  }

  readonly auditingComponent = viewChild(AuditingComponent);
  readonly statSalesComponent = viewChild(StatSalesComponent);
  readonly delivery = viewChild(StatDeliveryComponent);
  protected active = 'auditing';
  protected defaultDate: Date = new Date();
  protected produit: IProduit | null = null;
  protected produits: IProduit[] = [];
  protected event: any;
  protected searchValue?: string;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);
  private readonly dateDebut = viewChild<DatePickerComponent>('dateDebut');
  private readonly dateFin = viewChild<DatePickerComponent>('dateFin');

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => {
      if (produit?.id) {
        this.produit = produit;
        const params: ProduitAuditingParam = this.buildQuery();
        this.produitAuditingParamService.setParameter(params);
      }
    });
  }

  load(): void {
    this.loadData();
  }

  resetData(): void {
    switch (this.active) {
      case 'auditing':
        this.auditingComponent().resetData();
        break;
    }
  }

  protected get fromDate(): Date | null {
    return this.dateDebut().value;
  }

  protected get toDate(): Date | null {
    return this.dateFin().value;
  }

  exportPdf(): void {
    this.updateParam();
    switch (this.active) {
      case 'auditing':
        this.auditingComponent().exportPdf();
        break;
      case 'sales':
        this.statSalesComponent().exportPdf(this.buildQuery());
        break;
      case 'commande':
        this.delivery().exportPdf(this.buildQuery());
        break;
    }
  }

  onSelect(event: any): void {
    this.event = event;
    this.loadData();
  }

  onClear(event: any): void {
    this.resetData();
  }

  protected buildQuery(): ProduitAuditingParam {
    const params: ProduitAuditingParam = {
      produitId: this.produit.id,
      fromDate: this.dateDebut()?.submitValue,
      toDate: this.dateFin()?.submitValue,
    };
    this.produitAuditingParamService.setParameter(params);
    return params;
  }

  protected updateParam(): void {
    this.produitAuditingParamService.setParameter(this.buildQuery());
  }

  private loadData(): void {
    const params: ProduitAuditingParam = this.buildQuery();
    this.produitAuditingParamService.setParameter(params);
    switch (this.active) {
      case 'auditing':
        this.auditingComponent().load();
        break;
      case 'sales':
        this.statSalesComponent().load(params);
        break;
      case 'commande':
        this.delivery().load(params);
        break;
    }
  }

  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
}
