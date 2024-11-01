import { Component, inject, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { Facture, FactureItem } from '../facture.model';
import { SalesLineService } from '../../sales-line/sales-line.service';
import { ISalesLine } from '../../../shared/model/sales-line.model';
import { HttpResponse } from '@angular/common/http';
import { CommonModule, DatePipe, DecimalPipe, JsonPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { PanelModule } from 'primeng/panel';
import { FactureService } from '../facture.service';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-facture-detail',
  standalone: true,
  imports: [DecimalPipe, TableModule, PanelModule, DatePipe, JsonPipe, CommonModule, InputTextModule, FormsModule],
  templateUrl: './facture-detail.component.html',
  styles: ``,
})
export class FactureDetailComponent implements OnInit, OnChanges {
  @Input() facture: Facture | null = null;
  salesLineService = inject(SalesLineService);
  factureService = inject(FactureService);
  salesLines: ISalesLine[] = [];
  selectedFactureItem: FactureItem | null = null;
  searchValue: string | undefined;

  // scrollHeight="400px"
  constructor() {
    //   this.facture = this.factureStateService.selectedInvoice();
  }

  onRowSelect(factureItem: FactureItem) {
    this.selectedFactureItem = factureItem;
    this.salesLineService.queryBySale(factureItem.saleId).subscribe((res: HttpResponse<ISalesLine[]>) => {
      this.salesLines = res.body || [];
    });
  }

  ngOnInit(): void {
    if (this.facture && this.facture.factureId) {
      this.factureService.find(this.facture?.factureId).subscribe((res: HttpResponse<Facture>) => {
        this.facture = res.body || null;
      });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['facture'] && !changes['facture'].isFirstChange()) {
      this.selectedFactureItem = null;
      this.salesLines = [];
    }
  }
}
