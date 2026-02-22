import {Component, inject, OnInit, viewChild} from '@angular/core';
import {ISales, SalesStatut} from '../../../shared/model';
import {SalesService} from '../sales.service';
import {WarehouseCommonModule} from '../../../shared/warehouse-common/warehouse-common.module';
import {FormsModule} from '@angular/forms';
import {TooltipModule} from 'primeng/tooltip';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {TableModule} from 'primeng/table';
import {Router, RouterModule} from '@angular/router';
import {ToolbarModule} from 'primeng/toolbar';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Select} from 'primeng/select';
import {
  ConfirmDialogComponent
} from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {ConfigurationService} from '../../../shared/configuration.service';
import {ButtonGroup} from 'primeng/buttongroup';
import {NgxSpinnerComponent, NgxSpinnerService} from "ngx-spinner";

@Component({
  selector: 'jhi-presale',
  templateUrl: './presale.component.html',
  styleUrls: ['./presale.scss'],

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
    ConfirmDialogComponent,
    ButtonGroup,
    NgxSpinnerComponent,
  ],
})
export class PresaleComponent implements OnInit {
  typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  typeVenteSelected = '';
  sales: ISales[] = [];
  search = '';
  useSimpleSale = false;
  private readonly salesService = inject(SalesService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly configService = inject(ConfigurationService);
  private readonly router = inject(Router);
  private readonly spinner = inject(NgxSpinnerService);

  ngOnInit(): void {
    this.typeVenteSelected = 'TOUT';
    this.loadPreventes();

    // Load simple sale configuration
    this.configService.getSimpleSaleConfig().subscribe(enabled => {
      this.useSimpleSale = enabled;
    });
  }

  onTypeVenteChange(): void {
    this.loadPreventes();
  }

  loadPreventes(): void {
    this.salesService
      .queryPrevente({
        search: this.search,
        type: this.typeVenteSelected,
        statut: [SalesStatut.PROCESSING, SalesStatut.PENDING],
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
      this.spinner.show('prevente-spinner');
      this.salesService.deletePrevente(sale.saleId).subscribe(() => {
        this.spinner.hide('prevente-spinner');
        this.loadPreventes();
      });
    }
  }

  confirmRemove(sale: ISales): void {
    this.confimDialog().onConfirm(() => this.deletePrevente(sale), 'Suppression de pré-vente', 'Voulez-vous supprimer cette pré-vente ?');
  }

  confirmTransform(sale: ISales): void {
    this.confimDialog().onConfirm(
      () => this.transformPrevente(sale),
      'Transformer en vente',
      'Voulez-vous transformer cette pré-vente en vente ? La pré-vente sera supprimée après transformation.'
    );
  }

  navigateToSale(sale: ISales): void {
    this.router.navigate(['/sales-home/prevente'], {
      state: {saleInfo: {saleId: sale.saleId, isPresale: true}},
    });
  }

  navigateToSaleEnCors(sale: ISales): void {
    this.router.navigate(['/sales-home'], {
      state: {saleInfo: {saleId: sale.saleId}},
    });
  }

  transformPrevente(sale: ISales): void {
    if (!sale.saleId) {
      return;
    }
    const isAssurance = sale.natureVente === 'ASSURANCE';
    this.spinner.show('prevente-spinner');
    const transform$ = isAssurance
      ? this.salesService.transformToVenteAssurance(sale.saleId)
      : this.salesService.transformToVenteComptant(sale.saleId);

    transform$.subscribe({
      next: () => {
        this.spinner.hide('prevente-spinner');
        this.navigateToSaleEnCors(sale);
      },
      error: () => {
        this.spinner.hide('prevente-spinner');
      },
    });
  }
}
