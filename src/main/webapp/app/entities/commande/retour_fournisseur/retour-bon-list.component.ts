import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule, Table } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from 'primeng/ripple';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { IRetourBon } from 'app/shared/model/retour-bon.model';
import { RetourBonStatut } from 'app/shared/model/enumerations/retour-bon-statut.model';
import { RetourBonService } from './retour-bon.service';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import moment from 'moment';

@Component({
  selector: 'jhi-retour-bon-list',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    ToolbarModule,
    TooltipModule,
    TagModule,
    InputTextModule,
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

  @ViewChild('dt') table: Table | undefined;

  protected retourBons = signal<IRetourBon[]>([]);
  protected loading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = signal<number>(0);
  protected searchValue = '';
  protected selectedStatut: RetourBonStatut | null = null;
  protected expandedRows: { [key: string]: boolean } = {};

  protected readonly RetourBonStatut = RetourBonStatut;

  ngOnInit(): void {
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    const query = {
      page: this.page(),
      size: this.itemsPerPage,
    };

    const observable = this.selectedStatut
      ? this.retourBonService.queryByStatut(this.selectedStatut, query)
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

  protected onSearch(): void {
    if (this.table) {
      this.table.filterGlobal(this.searchValue, 'contains');
    }
  }

  protected clearSearch(): void {
    this.searchValue = '';
    if (this.table) {
      this.table.clear();
    }
  }

  protected viewDetails(retourBon: IRetourBon): void {
    // Navigate to details view or open modal
    this.messageService.add({
      severity: 'info',
      summary: 'Détails',
      detail: `Voir les détails du retour #${retourBon.id}`,
    });
  }

  protected validateRetour(retourBon: IRetourBon): void {
    this.confirmationService.confirm({
      message: `Voulez-vous valider le retour #${retourBon.id} ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => {
        this.retourBonService.validate(retourBon.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Retour validé avec succès',
            });
            this.loadAll();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Erreur lors de la validation du retour',
            });
          },
        });
      },
    });
  }

  protected deleteRetour(retourBon: IRetourBon): void {
    this.confirmationService.confirm({
      message: `Voulez-vous supprimer le retour #${retourBon.id} ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.retourBonService.delete(retourBon.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Retour supprimé avec succès',
            });
            this.loadAll();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Erreur lors de la suppression du retour',
            });
          },
        });
      },
    });
  }

  protected navigateToCreate(): void {
    this.router.navigate(['/commande/retour-fournisseur']);
  }

  protected getStatusSeverity(statut: RetourBonStatut): 'success' | 'info' | 'warn' | 'danger' {
    switch (statut) {
      case RetourBonStatut.VALIDATED:
        return 'success';
      case RetourBonStatut.PROCESSING:
        return 'info';
      case RetourBonStatut.CANCELLED:
        return 'danger';
      default:
        return 'info';
    }
  }

  protected getStatusLabel(statut: RetourBonStatut): string {
    switch (statut) {
      case RetourBonStatut.VALIDATED:
        return 'Validé';
      case RetourBonStatut.PROCESSING:
        return 'En cours';
      case RetourBonStatut.CANCELLED:
        return 'Annulé';
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
    return (
      retourBon.retourBonItems?.reduce((sum, item) => sum + (item.qtyMvt || 0), 0) || 0
    );
  }

}
