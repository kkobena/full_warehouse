import { Component, OnInit } from '@angular/core';
import { ISales } from '../../../shared/model/sales.model';
import { SalesService } from '../sales.service';
import { ConfirmationService } from 'primeng/api';

@Component({
  selector: 'jhi-presale',
  templateUrl: './presale.component.html',
  providers: [ConfirmationService],
})
export class PresaleComponent implements OnInit {
  typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  typeVenteSelected = '';
  sales: ISales[] = [];
  search = '';

  constructor(protected salesService: SalesService, protected confirmationService: ConfirmationService) {}

  ngOnInit(): void {
    this.typeVenteSelected = 'TOUT';
    this.loadPreventes();
  }

  onTypeVenteChange(): void {
    this.loadPreventes();
  }

  loadPreventes(): void {
    this.salesService
      .queryPrevente({
        search: this.search,
        type: this.typeVenteSelected,
      })
      .subscribe(res => {
        this.sales = res.body ?? [];
      });
  }

  onSearch(): void {
    this.loadPreventes();
  }

  deletePrevente(sale: ISales): void {
    if (sale.id) {
      this.salesService.deletePrevente(sale.id).subscribe(() => this.loadPreventes());
    }
  }

  confirmRemove(sale: ISales): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer cette pré-vente ?',
      header: 'SUPPRESSION DE PRE-VENTE',
      icon: 'pi pi-info-circle',
      accept: () => this.deletePrevente(sale),
      key: 'deletePrevente',
    });
  }
}