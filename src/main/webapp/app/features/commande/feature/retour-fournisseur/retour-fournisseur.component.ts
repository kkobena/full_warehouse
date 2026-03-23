import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {HttpResponse} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {ButtonModule} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {TooltipModule} from 'primeng/tooltip';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';
import {RippleModule} from 'primeng/ripple';
import {ToolbarModule} from 'primeng/toolbar';
import {InputTextModule} from 'primeng/inputtext';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {SelectModule} from 'primeng/select';
import {DatePicker} from 'primeng/datepicker';
import {FloatLabel} from 'primeng/floatlabel';
import {MessageService} from 'primeng/api';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {NgbConfirmDialogService} from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {WarehouseCommonModule} from 'app/shared/warehouse-common/warehouse-common.module';
import {IRetourBon} from 'app/shared/model/retour-bon.model';
import {IReponseRetourBon} from 'app/shared/model/reponse-retour-bon.model';
import {RetourBonStatut} from 'app/shared/model/enumerations/retour-bon-statut.model';
import {ITEMS_PER_PAGE} from 'app/shared/constants/pagination.constants';
import {DATE_FORMAT_ISO_DATE} from 'app/shared/util/warehouse-util';
import {RetourBonService} from '../../../../entities/commande/retour_fournisseur/retour-bon.service';
import {
  SupplierResponseModalComponent
} from '../../../../entities/commande/retour_fournisseur/supplier-response-modal.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'app-retour-fournisseur',
  templateUrl: './retour-fournisseur.component.html',
  styleUrls: ['./retour-fournisseur.scss'],
  providers: [MessageService],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    ToolbarModule,
    InputTextModule,
    IconField,
    InputIcon,
    SelectModule,
    DatePicker,
    FloatLabel,
    TableModule,
    TooltipModule,
    TagModule,
    ToastModule,
    RippleModule,
    WarehouseCommonModule,
  ],
})
export class AppRetourFournisseurComponent implements OnInit {
  protected search = '';
  protected selectedStatut: RetourBonStatut | null = null;
  protected dtStart: Date | null = new Date();
  protected dtEnd: Date | null = new Date();
  protected readonly statutOptions = [
    {label: 'En attente de réponse', value: RetourBonStatut.VALIDATED},
    {label: 'Clôturé', value: RetourBonStatut.CLOSED},
  ];

  protected retourBons = signal<IRetourBon[]>([]);
  protected loading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = signal<number>(0);
  protected readonly RetourBonStatut = RetourBonStatut;

  private readonly retourBonService = inject(RetourBonService);
  private readonly messageService = inject(MessageService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly modalService = inject(NgbModal);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.loadAll();
  }

  onSearch(): void {
    this.page.set(0);
    this.loadAll();
  }

  onNewRetour(): void {
    this.router.navigate(['/commande/retour-fournisseur/new']);
  }

  protected loadAll(): void {
    this.loading.set(true);
    const query: any = {
      page: this.page(),
      size: this.itemsPerPage,
    };
    if (this.dtStart) {
      query.dtStart = DATE_FORMAT_ISO_DATE(this.dtStart);
    }
    if (this.dtEnd) {
      query.dtEnd = DATE_FORMAT_ISO_DATE(this.dtEnd);
    }
    if (this.search) {
      query.search = this.search;
    }

    const observable = this.selectedStatut
      ? this.retourBonService.queryByStatut(this.selectedStatut, query)
      : this.retourBonService.query(query);

    observable.subscribe({
      next: (res: HttpResponse<IRetourBon[]>) => {
        this.onSuccess(res.body, res.headers);
        this.loading.set(false);
      },
      error: () => {
        this.onError();
        this.loading.set(false);
      },
    });
  }

  protected onPageChange(event: any): void {
    this.page.set(event.page);
    this.loadAll();
  }

  protected setSupplierResponse(retourBon: IRetourBon): void {
    showCommonModal(
      this.modalService,
      SupplierResponseModalComponent,
      {
        retourBon,
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

  protected getTotalItems(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.length || 0;
  }

  protected getTotalQuantity(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.reduce((sum, item) => sum + (item.qtyMvt || 0), 0) || 0;
  }

  protected getTotalAccepted(retourBon: IRetourBon): number {
    return retourBon.retourBonItems?.reduce((sum, item) => sum + (item.acceptedQty || 0), 0) || 0;
  }

  private onSuccess(data: IRetourBon[] | null, headers: any): void {
    this.totalRecords.set(Number(headers.get('X-Total-Count')));
    this.retourBons.set(data || []);
  }

  private onError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Erreur lors du chargement des retours',
    });
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
}
