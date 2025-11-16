import { Component, inject, input, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { Table, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from 'primeng/ripple';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { IRetourBon } from 'app/shared/model/retour-bon.model';
import { IReponseRetourBon } from 'app/shared/model/reponse-retour-bon.model';
import { RetourBonStatut } from 'app/shared/model/enumerations/retour-bon-statut.model';
import { RetourBonService } from './retour-bon.service';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import moment from 'moment';
import { SupplierResponseModalComponent } from './supplier-response-modal.component';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-retour-bon-list',
  imports: [
    CommonModule,
    ButtonModule,
    TableModule,
    TooltipModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule,
    RippleModule,
    WarehouseCommonModule,
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './retour-bon-list.component.html',
  styleUrl: './retour-bon-list.component.scss',
})
export class RetourBonListComponent implements OnInit {
  private readonly retourBonService = inject(RetourBonService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly modalService = inject(NgbModal);

  @ViewChild('dt') table: Table | undefined;

  // Inputs from parent component
  readonly search = input('');
  readonly selectedStatut = input<RetourBonStatut | null>(null);
  readonly dtStart = input<Date | null>(null);
  readonly dtEnd = input<Date | null>(null);

  protected retourBons = signal<IRetourBon[]>([]);
  protected loading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = signal<number>(0);

  readonly rowExpandMode: ExpandMode;
  protected readonly RetourBonStatut = RetourBonStatut;

  ngOnInit(): void {
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    const query: any = {
      page: this.page(),
      size: this.itemsPerPage,
    };

    if (this.dtStart()) {
      query.dtStart = DATE_FORMAT_ISO_DATE(this.dtStart());
    }
    if (this.dtEnd()) {
      query.dtEnd = DATE_FORMAT_ISO_DATE(this.dtEnd());
    }

    if (this.search()) {
      query.search = this.search();
    }

    const observable = this.selectedStatut()
      ? this.retourBonService.queryByStatut(this.selectedStatut(), query)
      : this.retourBonService.query(query);

    observable.subscribe({
      next: (res: HttpResponse<IRetourBon[]>) => {
        this.onSuccess(res.body, res.headers);
      },
      error: () => {
        this.onError();
      },
      complete: () => {
        this.loading.set(false);
      },
    });
  }

  protected onSuccess(data: IRetourBon[] | null, headers: any): void {
    this.totalRecords.set(Number(headers.get('X-Total-Count')));
    this.retourBons.set(data || []);
  }

  protected onError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Erreur lors du chargement des retours',
    });
  }

  protected onPageChange(event: any): void {
    this.page.set(event.page);
    this.loadAll();
  }

  onSearch(): void {
    this.page.set(0);
    this.loadAll();
  }

  protected setSupplierResponse(retourBon: IRetourBon): void {
    showCommonModal(
      this.modalService,
      SupplierResponseModalComponent,
      {
        retourBon: retourBon,
        title: `Saisir la réponse fournisseur - ${retourBon.receiptReference}`,
      },
      (reponseRetourBon: IReponseRetourBon) => {
        if (reponseRetourBon) {
          this.saveSupplierResponse(reponseRetourBon);
        }
      },
      'xl',
    );
  }

  private saveSupplierResponse(reponseRetourBon: IReponseRetourBon): void {
    this.loading.set(true);
    this.retourBonService.createSupplierResponse(reponseRetourBon).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Réponse fournisseur enregistrée avec succès',
        });
        this.loadAll();
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: "Erreur lors de l'enregistrement de la réponse fournisseur",
        });
        this.loading.set(false);
      },
    });
  }

  protected getStatusSeverity(statut: RetourBonStatut): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (statut) {
      case RetourBonStatut.VALIDATED:
        return 'info';
      case RetourBonStatut.PROCESSING:
        return 'secondary';
      case RetourBonStatut.CLOSED:
        return 'success';
      default:
        return 'info';
    }
  }

  protected getStatusLabel(statut: RetourBonStatut): string {
    switch (statut) {
      case RetourBonStatut.VALIDATED:
        return 'En attente de réponse';
      case RetourBonStatut.PROCESSING:
        return 'En cours';
      case RetourBonStatut.CLOSED:
        return 'Clôturé';
      default:
        return statut;
    }
  }

  protected formatDate(date: string | undefined): string {
    return date ? moment(date).format('DD/MM/YYYY HH:mm') : '';
  }

  protected getTotalItems(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.length || 0;
  }

  protected getTotalQuantity(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.reduce((sum, item) => sum + (item.qtyMvt || 0), 0) || 0;
  }

  protected getTotalAccepted(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.reduce((sum, item) => sum + (item.acceptedQty || 0), 0) || 0;
  }
}
