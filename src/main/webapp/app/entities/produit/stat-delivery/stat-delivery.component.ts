import { AfterViewInit, Component, inject, viewChild } from '@angular/core';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';
import { YearlyDeleveryProduitHistoriqueComponent } from './yearly-delevery-produit-historique/yearly-delevery-produit-historique.component';
import { DailyDeleveryProduitHistoriqueComponent } from './daily-delevery-produit-historique/daily-delevery-produit-historique.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { Button } from 'primeng/button';
import { DatePickerComponent } from '../../../shared/date-picker/date-picker.component';
import { ProduitAutocompleteComponent } from '../../../shared/produit-autocomplete/produit-autocomplete.component';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { PRODUIT_COMBO_RESULT_SIZE } from '../../../shared/constants/pagination.constants';
import { IProduit } from '../../../shared/model/produit.model';
import { FormsModule } from '@angular/forms';
import { ProduitAuditingParamService } from '../transaction/produit-auditing-param.service';

@Component({
  selector: 'jhi-stat-delivery',
  imports: [
    FormsModule,
    WarehouseCommonModule,
    DailyDeleveryProduitHistoriqueComponent,
    YearlyDeleveryProduitHistoriqueComponent,
    Button,
    DatePickerComponent,
    ProduitAutocompleteComponent,
    Toolbar,
    Tooltip,
  ],
  templateUrl: './stat-delivery.component.html',
  styleUrl: './stat-delivery.component.scss',
})
export class StatDeliveryComponent implements AfterViewInit {
  protected readonly daily = viewChild.required(DailyDeleveryProduitHistoriqueComponent);
  protected readonly yearly = viewChild.required(YearlyDeleveryProduitHistoriqueComponent);
  protected active = 'daily';
  protected defaultDate: Date = new Date();
  protected produit: IProduit | null = null;
  protected produits: IProduit[] = [];
  protected event: any;
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
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
      this.daily()?.load(this.buildQuery());
    } else {
      this.yearly()?.load(this.buildQuery());
    }
  }

  exportPdf(): void {
    if (this.active === 'daily') {
      this.daily()?.exportPdf(this.buildQuery());
    } else {
      this.yearly()?.exportPdf(this.buildQuery());
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
