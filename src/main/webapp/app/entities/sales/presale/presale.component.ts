import { Component, inject, OnInit, viewChild } from '@angular/core';
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
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'jhi-presale',
  templateUrl: './presale.component.html',

  imports: [
    WarehouseCommonModule,
    RouterModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    TableModule,
    ToolbarModule,
    IconField,
    InputIcon,
    Select,
    ConfirmDialogComponent
  ]
})
export class PresaleComponent implements OnInit {

  typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  typeVenteSelected = '';
  sales: ISales[] = [];
  search = '';
  private readonly  salesService = inject(SalesService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
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
    this.confimDialog().onConfirm( () => this.deletePrevente(sale), 'Suppression de pré-vente', 'Voulez-vous supprimer cette pré-vente ?');
  }
}
