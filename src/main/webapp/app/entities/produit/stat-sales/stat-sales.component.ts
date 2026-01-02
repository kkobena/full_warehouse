import { AfterViewInit, Component, inject, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { DailySaleProduitHistoriqueComponent } from './daily-sale-produit-historique/daily-sale-produit-historique.component';
import { YearlySaleProduitHistoriqueComponent } from './yearly-sale-produit-historique/yearly-sale-produit-historique.component';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';
import { Button } from 'primeng/button';
import { DatePickerComponent } from '../../../shared/date-picker/date-picker.component';
import { ProduitAutocompleteComponent } from '../../../shared/produit-autocomplete/produit-autocomplete.component';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { PRODUIT_COMBO_RESULT_SIZE } from '../../../shared/constants/pagination.constants';
import { IProduit } from '../../../shared/model/produit.model';
import { ProduitAuditingParamService } from '../transaction/produit-auditing-param.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-stat-sales',
  imports: [
    WarehouseCommonModule,
    DailySaleProduitHistoriqueComponent,
    YearlySaleProduitHistoriqueComponent,
    Button,
    DatePickerComponent,
    ProduitAutocompleteComponent,
    Toolbar,
    Tooltip,
    FormsModule,
  ],
  templateUrl: './stat-sales.component.html',
  styleUrl: './stat-sales.component.scss',
})
export class StatSalesComponent implements AfterViewInit {
  readonly dailySaleProduitHistoriqueComponent = viewChild(DailySaleProduitHistoriqueComponent);
  readonly yearlySaleProduitHistoriqueComponent = viewChild(YearlySaleProduitHistoriqueComponent);
  protected active = 'daily';
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  protected defaultDate: Date = new Date();
  protected produit: IProduit | null = null;
  protected produits: IProduit[] = [];
  protected event: any;
  private readonly dateDebut = viewChild<DatePickerComponent>('dateDebut');
  private readonly dateFin = viewChild<DatePickerComponent>('dateFin');
  private readonly produitAuditingParamService = inject(ProduitAuditingParamService);

  protected get fromDate(): Date | null {
    return this.dateDebut().value;
  }

  protected get toDate(): Date | null {
    return this.dateFin().value;
  }

  onSelect(event: any): void {
    this.event = event;
    this.load();
  }

  ngAfterViewInit(): void {
    this.dateDebut().value = this.defaultDate;
    this.dateFin().value = this.defaultDate;
  }

  onClear(event: any): void {
    // this.resetData();
  }

  load(): void {
    if (this.active === 'daily') {
      this.dailySaleProduitHistoriqueComponent()?.load(this.buildQuery());
    } else {
      this.yearlySaleProduitHistoriqueComponent()?.load(this.buildQuery());
    }
  }

  exportPdf(): void {
    if (this.active === 'daily') {
      this.dailySaleProduitHistoriqueComponent()?.exportPdf(this.buildQuery());
    } else {
      this.yearlySaleProduitHistoriqueComponent()?.exportPdf(this.buildQuery());
    }
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
}
