import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { Facture, FactureItem } from '../facture.model';
import { SalesLineService } from '../../sales-line/sales-line.service';
import { ISalesLine } from '../../../shared/model/sales-line.model';
import { HttpResponse } from '@angular/common/http';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { PanelModule } from 'primeng/panel';
import { FactureService } from '../facture.service';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';
import { InputIcon } from 'primeng/inputicon';
import { IconField } from 'primeng/iconfield';

@Component({
  selector: 'jhi-facture-detail',
  imports: [DecimalPipe, TableModule, PanelModule, DatePipe, CommonModule, InputTextModule, FormsModule, InputIcon, IconField],
  templateUrl: './facture-detail.component.html',
  styles: ``,
})
export class FactureDetailComponent implements OnInit {
  readonly facture = input<Facture | null>(null);
  salesLineService = inject(SalesLineService);
  factureService = inject(FactureService);
  salesLines: ISalesLine[] = [];
  selectedFactureItem: FactureItem | null = null;
  searchValue: string | undefined;
  update = computed(() => {
    if (this.facture()) {
      this.selectedFactureItem = null;
      this.salesLines = [];
    }
  });
  protected factureWritable = signal(this.facture());

  // scrollHeight="400px"
  constructor() {}

  onRowSelect(factureItem: FactureItem) {
    this.selectedFactureItem = factureItem;
    this.salesLineService.queryBySale(factureItem.saleId).subscribe((res: HttpResponse<ISalesLine[]>) => {
      this.salesLines = res.body || [];
    });
  }

  ngOnInit(): void {
    this.factureWritable.set(this.facture());
    const facture = this.factureWritable();
    if (facture && facture.factureId) {
      this.factureService.find(facture.factureId).subscribe((res: HttpResponse<Facture>) => {
        this.factureWritable.set(res.body);
      });
    }
  }
}
