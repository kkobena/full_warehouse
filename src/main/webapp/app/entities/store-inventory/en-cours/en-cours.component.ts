import { Component, OnInit, input, inject } from '@angular/core';
import {
  GROUPING_BY,
  InventoryCategory,
  InventoryStatut,
  IStoreInventory,
  ItemsCountRecord,
  StoreInventoryExportRecord,
} from '../../../shared/model/store-inventory.model';
import { IUser } from '../../../core/user/user.model';
import { StoreInventoryService } from '../store-inventory.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ITEMS_PER_PAGE } from '../../../config/pagination.constants';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ErrorService } from '../../../shared/error.service';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DialogService } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';

@Component({
  selector: 'jhi-en-cours',
  templateUrl: './en-cours.component.html',
  imports: [
    WarehouseCommonModule,
    ConfirmDialogModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    ToastModule,
    NgxSpinnerModule,
    TableModule,
    RouterModule,
  ],
  providers: [ConfirmationService, DialogService, MessageService],
})
export class EnCoursComponent implements OnInit {
  private spinner = inject(NgxSpinnerService);
  private storeInventoryService = inject(StoreInventoryService);
  protected router = inject(Router);
  protected modalService = inject(NgbModal);
  private errorService = inject(ErrorService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  readonly inventoryCategories = input<InventoryCategory[]>();
  readonly user = input<IUser | null>();
  protected statuts: InventoryStatut[] = ['CREATE', 'PROCESSING'];
  protected page!: number;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected rowData: IStoreInventory[] = [];
  protected totalItems = 0;

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loadPage();
  }

  exportPdf(storeInventory: IStoreInventory): void {
    this.spinner.show();
    this.storeInventoryService.exportToPdf(this.buildPdfQuery(storeInventory.id)).subscribe(blod => {
      //  const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
      //  saveAs(blod, 'inventaire_' + fileName);
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
      this.spinner.hide();
    });
  }

  confirmDelete(storeInventory: IStoreInventory): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette ligne  ?',
      header: ' SUPPRESSION',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      icon: 'pi pi-info-circle',
      accept: () => this.delete(storeInventory.id),
      key: 'delete',
    });
  }

  confirmSave(storeInventory: IStoreInventory): void {
    this.confirmationService.confirm({
      message: "Voullez-vous clôturer l'inventaire ?",
      header: ' CLOTURE',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.close(storeInventory.id),
      key: 'saveAll',
    });
  }

  delete(id: number): void {
    this.spinner.show();
    this.storeInventoryService.delete(id).subscribe({
      next: () => {
        this.loadPage();
        this.spinner.hide();
      },
      error: () => {
        this.onError();
        this.spinner.hide();
      },
    });
  }

  close(id: number): void {
    this.spinner.show();
    this.storeInventoryService.close(id).subscribe({
      next: (res: HttpResponse<ItemsCountRecord>) => {
        this.loadPage();
        this.spinner.hide();
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `Nombre de produits de l'inventaire ${res.body.count}`,
        });
      },
      error: error => {
        this.onCloseError(error);
        this.spinner.hide();
      },
    });
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  protected onSuccess(data: IStoreInventory[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/store-inventory'], {
        queryParams: this.buildQuery(page),
      });
    }
    this.rowData = data || [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  protected onCloseError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        error: () => this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger'),
      });
    }
  }

  private loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;

    this.storeInventoryService.query(this.buildQuery(page)).subscribe({
      next: (res: HttpResponse<IStoreInventory[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
      error: () => this.onError(),
    });
  }

  private buildQuery(page?: number): any {
    const pageToLoad: number = page || this.page || 1;
    return {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      userId: this.user()?.id,
      inventoryCategories: this.inventoryCategories()?.map(e => e.name),
      statuts: this.statuts,
    };
  }

  private buildPdfQuery(storeInventoryId: number): StoreInventoryExportRecord {
    return {
      exportGroupBy: GROUPING_BY[0].name,
      filterRecord: {
        storeInventoryId,
        selectedFilter: 'NONE',
      },
    };
  }
}
