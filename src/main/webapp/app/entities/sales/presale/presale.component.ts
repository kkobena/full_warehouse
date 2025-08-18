import { Component, inject, OnInit } from '@angular/core';
import { ISales } from '../../../shared/model/sales.model';
import { SalesService } from '../sales.service';
import { ConfirmationService } from 'primeng/api';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { RouterModule } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToolbarModule } from 'primeng/toolbar';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Select } from 'primeng/select';

@Component({
  selector: 'jhi-presale',
  templateUrl: './presale.component.html',
  providers: [ConfirmationService],
  imports: [
    WarehouseCommonModule,
    RouterModule,
    ConfirmDialogModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    TableModule,
    ToolbarModule,
    IconField,
    InputIcon,
    Select
  ]
})
export class PresaleComponent implements OnInit {
  protected salesService = inject(SalesService);
  protected confirmationService = inject(ConfirmationService);

  typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  typeVenteSelected = '';
  sales: ISales[] = [];
  search = '';

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
        type: this.typeVenteSelected
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
      key: 'deletePrevente'
    });
  }
}
