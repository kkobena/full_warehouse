import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FactureDetailComponent } from '../facture-detail/facture-detail.component';
import { Facture } from '../facture.model';
import { CommonModule, DecimalPipe } from '@angular/common';
import { PanelModule } from 'primeng/panel';
import { PrimeTemplate } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { HttpResponse } from '@angular/common/http';
import { FactureService } from '../facture.service';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-groupe-facture-detail',
  imports: [
    FactureDetailComponent,
    DecimalPipe,
    PanelModule,
    PrimeTemplate,
    TableModule,
    InputTextModule,
    PaginatorModule,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './groupe-facture-detail.component.html',
  styles: ``,
})
export class GroupeFactureDetailComponent implements OnInit {
  readonly groupeFacture = input<Facture | null>(null);
  groupeFactureSignal = signal(this.groupeFacture());
  selectedFacture: Facture | null = null;
  factureService = inject(FactureService);
  searchValue: string | undefined;

  onRowSelect(factureItem: Facture) {
    this.selectedFacture = factureItem;
  }

  ngOnInit(): void {
    this.groupeFactureSignal.set(this.groupeFacture());
    this.factureService.find(this.groupeFactureSignal().factureId).subscribe((res: HttpResponse<Facture>) => {
      this.groupeFactureSignal.set(res.body);
    });
  }
}
