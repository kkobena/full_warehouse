import { Component, inject, OnInit, viewChild } from '@angular/core';
import { ISales } from '../../../shared/model/sales.model';
import { SalesService } from '../sales.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { Select } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-vente-en-cours',
  templateUrl: './vente-en-cours.component.html',
  styleUrls: ['../sales.component.scss', './vente-en-cours.component.scss'],
  imports: [
    WarehouseCommonModule,
    RouterModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    TableModule,
    ToolbarModule,
    Select,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
  ],
})
export class VenteEnCoursComponent implements OnInit {
  typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  typeVenteSelected = '';
  sales: ISales[] = [];
  search = '';
  private readonly salesService = inject(SalesService);
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
      this.salesService.deletePrevente(sale.saleId).subscribe(() => this.loadPreventes());
    }
  }

  confirmRemove(sale: ISales): void {
    this.confimDialog().onConfirm(() => this.deletePrevente(sale), 'Suppression de pré-vente', 'Voulez-vous supprimer cette pré-vente ?');
  }
}
