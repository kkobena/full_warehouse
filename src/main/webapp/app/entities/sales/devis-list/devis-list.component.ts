import {Component, inject, OnInit, signal, viewChild} from '@angular/core';
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
import {ButtonGroup} from 'primeng/buttongroup';
import {saveAs} from 'file-saver';
import {NgxSpinnerModule, NgxSpinnerService} from 'ngx-spinner';
import {ToastModule} from 'primeng/toast';
import {MessageService} from 'primeng/api';
import {DatePicker} from "primeng/datepicker";
import {FloatLabel} from "primeng/floatlabel";
import {DatePipe} from "@angular/common";
import {HttpResponse} from "@angular/common/http";
import {SaleId} from "../../../shared/model/sales.model";

@Component({
  selector: 'jhi-devis-list',
  templateUrl: './devis-list.component.html',
  styleUrls: ['./devis-list.component.scss'],
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
    NgxSpinnerModule,
    ToastModule,
    DatePicker,
    FloatLabel,
  ],
  providers: [MessageService, DatePipe],
})
export class DevisListComponent implements OnInit {

  protected loading = signal(false);
  protected typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  protected typeVenteSelected = '';
  protected sales: ISales[] = [];
  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  private readonly salesService = inject(SalesService);
  private readonly confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly router = inject(Router);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly messageService = inject(MessageService);
  private readonly datePipe = inject(DatePipe);

  ngOnInit(): void {
    this.typeVenteSelected = 'TOUT';
    this.loadDevis();

  }

  onTypeVenteChange(): void {
    this.loadDevis();
  }

  loadDevis(): void {
    this.loading.set(true);
    this.salesService
      .queryDevis({
        search: this.search,
        type: this.typeVenteSelected,
        statut: SalesStatut.DEVIS,
        fromDate: this.fromDate ? this.datePipe.transform(this.fromDate, 'yyyy-MM-dd') : null,
        toDate: this.toDate ? this.datePipe.transform(this.toDate, 'yyyy-MM-dd') : null,
      })
      .subscribe({
        next: res => {
          this.sales = res.body ?? [];
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  onSearch(): void {
    this.loadDevis();
  }

  // ============================================
  // ACTIONS
  // ============================================

  confirmDelete(sale: ISales): void {
    this.confirmDialog().onConfirm(
      () => this.deleteDevis(sale),
      'Suppression de devis',
      'Voulez-vous supprimer ce devis ?'
    );
  }

  deleteDevis(sale: ISales): void {
    if (sale.saleId) {
      this.spinner.show('devis-spinner');
      this.salesService.deletePrevente(sale.saleId).subscribe(() => {
        this.spinner.hide('devis-spinner');
        this.loadDevis();
      });
    }
  }

  confirmTransform(sale: ISales): void {
    this.confirmDialog().onConfirm(
      () => this.transformDevis(sale),
      'Transformer en vente',
      'Voulez-vous transformer ce devis en vente ? Le devis sera supprimé après transformation.'
    );
  }

  transformDevis(sale: ISales): void {
    if (!sale.saleId) {
      return;
    }

    this.spinner.show('devis-spinner');
    const isAssurance = sale.natureVente === 'ASSURANCE';

    const transform$ = isAssurance
      ? this.salesService.transformToVenteAssurance(sale.saleId)
      : this.salesService.transformToVenteComptant(sale.saleId);

    transform$.subscribe({
      next: (res: HttpResponse<SaleId>) => {
        this.spinner.hide('devis-spinner');
        this.navigateToSale(res.body);
      },
      error: () => {
        this.spinner.hide('devis-spinner');
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Une erreur est survenue lors de la transformation',
        });
      },
    });
  }

  confirmClone(sale: ISales): void {
    this.confirmDialog().onConfirm(
      () => this.cloneDevis(sale),
      'Cloner le devis',
      'Voulez-vous créer une copie de ce devis ?'
    );
  }

  cloneDevis(sale: ISales): void {
    if (!sale.saleId) {
      return;
    }

    this.spinner.show('devis-spinner');
    const isAssurance = sale.natureVente === 'ASSURANCE';

    const clone$ = isAssurance
      ? this.salesService.cloneDevisAssurance(sale.saleId)
      : this.salesService.cloneDevisComptant(sale.saleId);

    clone$.subscribe({
      next: () => {
        this.spinner.hide('devis-spinner');
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Le devis a été cloné avec succès',
        });
        this.loadDevis();
      },
      error: () => {
        this.spinner.hide('devis-spinner');
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Une erreur est survenue lors du clonage',
        });
      },
    });
  }

  printDevisPdf(sale: ISales): void {
    if (!sale.saleId) {
      return;
    }

    this.spinner.show('devis-spinner');
    this.salesService.printDevisPdf(sale.saleId).subscribe({
      next: (blob: Blob) => {
        this.spinner.hide('devis-spinner');
        const fileName = `devis_${sale.numberTransaction}.pdf`;
        saveAs(blob, fileName);
      },
      error: () => {
        this.spinner.hide('devis-spinner');
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: "Une erreur est survenue lors de l'impression",
        });
      },
    });
  }

  protected editDevis(sale: ISales): void {
    this.router.navigate(['/sales-home/devis'], {
      state: {saleInfo: {saleId: sale.saleId, isDevis: true}},
    });
  }

  private navigateToSale(saleId: SaleId): void {
    this.router.navigate(['/sales-home'], {
      state: {saleInfo: {saleId}},
    });
  }
}
