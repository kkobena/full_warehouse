import { Component, inject, OnInit } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { ISales } from '../../../shared/model/sales.model';
import { SalesService } from '../sales.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { Select } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

@Component({
  selector: 'jhi-vente-en-cours',
  templateUrl: './vente-en-cours.component.html',
  providers: [ConfirmationService],
  imports: [
    WarehouseCommonModule,
    RouterModule,
    ConfirmDialogModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    TableModule,
    ToolbarModule,
    Select,
    IconField,
    InputIcon,
  ],
})
export class VenteEnCoursComponent implements OnInit {
  typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  typeVenteSelected = '';
  sales: ISales[] = [];
  search = '';
  protected salesService = inject(SalesService);
  protected confirmationService = inject(ConfirmationService);

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
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.deletePrevente(sale),
      key: 'deletePrevente',
    });
  }
}
