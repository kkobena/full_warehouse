import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {HttpResponse} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {ButtonModule} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {TooltipModule} from 'primeng/tooltip';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';
import {ToolbarModule} from 'primeng/toolbar';
import {InputTextModule} from 'primeng/inputtext';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {SelectModule} from 'primeng/select';
import {DatePicker} from 'primeng/datepicker';
import {FloatLabel} from 'primeng/floatlabel';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {NotificationService} from 'app/shared/services/notification.service';
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
import { handleBlobForTauri } from "../../../../shared/util/tauri-util";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";

@Component({
  selector: 'app-retour-fournisseur',
  templateUrl: './retour-fournisseur.component.html',
  styleUrls: ['./retour-fournisseur.scss'],
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
    {label: 'En cours', value: RetourBonStatut.PROCESSING},
    {label: 'Clôturé', value: RetourBonStatut.CLOSED},
  ];

  protected retourBons = signal<IRetourBon[]>([]);
  protected loading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = signal<number>(0);
  protected readonly RetourBonStatut = RetourBonStatut;
  private readonly destroyRef = inject(DestroyRef);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly retourBonService = inject(RetourBonService);
  private readonly notificationService = inject(NotificationService);
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
    void this.router.navigate(['/commande/retour-fournisseur/new']);
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

    observable.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
    this.notificationService.error('Erreur lors du chargement des retours');
  }

  protected downloadPdf(retourBon: IRetourBon): void {
    this.retourBonService.getPdf(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (blob: Blob) => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'retour_bon');
        } else {
          const blobUrl = URL.createObjectURL(blob);
          window.open(blobUrl);
        }

      },
      error: () => {
        this.notificationService.error('Impossible de générer le PDF');
      },
    });
  }

  protected editRetour(retourBon: IRetourBon): void {
    void this.router.navigate(['/commande/retour-fournisseur', retourBon.id, 'edit']);
  }

  protected deleteRetour(retourBon: IRetourBon): void {
    this.confirmDialog.onConfirm(
      () => {
        this.retourBonService.delete(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => {
            this.notificationService.success('Retour supprimé avec succès');
            this.loadAll();
          },
          error: () => {
            this.notificationService.error('Impossible de supprimer ce retour');
          },
        });
      },
      'Supprimer le retour',
      `Supprimer le retour #${retourBon.id} (${retourBon.fournisseurLibelle}) ? Cette action est irréversible.`,
    );
  }

  protected sendEdi(retourBon: IRetourBon): void {
    this.retourBonService.sendEdi(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success('Retour envoyé via EDI PharmaML avec succès');
        this.loadAll();
      },
      error: () => {
        this.notificationService.error("Erreur lors de l'envoi EDI. Vérifiez la configuration PharmaML du fournisseur.");
      },
    });
  }

  protected markAsProcessing(retourBon: IRetourBon): void {
    this.retourBonService.markAsProcessing(retourBon.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success('Retour marqué en cours de traitement');
        this.loadAll();
      },
      error: () => {
        this.notificationService.error('Impossible de mettre à jour le statut');
      },
    });
  }

  private saveSupplierResponse(reponseRetourBon: IReponseRetourBon): void {
    this.loading.set(true);
    this.retourBonService.createSupplierResponse(reponseRetourBon).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success('Réponse fournisseur enregistrée avec succès');
        this.loadAll();
      },
      error: () => {
        this.notificationService.error("Erreur lors de l'enregistrement de la réponse fournisseur");
        this.loading.set(false);
      },
    });
  }
}
