import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FamilleProduitService } from './famille-produit.service';
import { RouterModule } from '@angular/router';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormFamilleComponent } from './form-famille/form-famille.component';
import { IResponseDto } from '../../shared/util/response-dto';
import { IFamilleProduit } from '../../shared/model/famille-produit.model';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { Tooltip } from 'primeng/tooltip';
import { InputIcon } from 'primeng/inputicon';
import { IconField } from 'primeng/iconfield';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { ErrorService } from '../../shared/error.service';
import { FileUploadDialogComponent } from '../groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { finalize } from 'rxjs/operators';
import { Panel } from 'primeng/panel';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-famille-produit',
  templateUrl: './famille-produit.component.html',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    Tooltip,
    InputIcon,
    IconField,
    ToastAlertComponent,
    ConfirmDialogComponent,
    Panel,
    SpinnerComponent
  ]
})
export class FamilleProduitComponent implements OnInit {
  responsedto!: IResponseDto;
  entites?: IFamilleProduit[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  loading!: boolean;
  isSaving = false;
  private readonly entityService = inject(FamilleProduitService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
   private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(page?: number, search?: string): void {
    const pageToLoad: number = page || this.page;
    const query: string = search || '';
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: ITEMS_PER_PAGE,
        search: query
      })
      .subscribe({
        next: (res: HttpResponse<IFamilleProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: err => this.onError(err)
      });
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    this.page = event.first / event.rows;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows,
        search: ''
      })
      .subscribe({
        next: (res: HttpResponse<IFamilleProduit[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: err => this.onError(err)
      });
  }

  confirmDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
      'Suppression',
      'Êtes-vous sûr de vouloir supprimer ?'
    );
  }

  delete(entity: IFamilleProduit): void {
    this.confirmDelete(entity.id);
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  showFileDialog(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        this.spinner().show();
        this.uploadFileResponse(this.entityService.uploadFile(result));
      },
      'xl'
    );
  }

  addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormFamilleComponent,
      {
        familleProduit: null,
        header: 'Ajout d\'une nouvelle famille de produit'
      },
      () => {
        this.loadPage(0);
      },
      'xl'
    );
  }

  onEdit(entity: IFamilleProduit): void {
    showCommonModal(
      this.modalService,
      FormFamilleComponent,
      {
        familleProduit: entity,
        header: 'Modification de ' + entity.libelle
      },
      () => {
        this.loadPage(0);
      },
      'xl'
    );
  }

  protected onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
    this.showFileDialog();
  }

  protected uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: err => this.onSaveError(err)
    });
  }

  protected onSuccess(data: IFamilleProduit[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.entites = data || [];
    this.loading = false;
  }

  protected onError(error: HttpErrorResponse): void {
    this.loading = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
      this.loadPage(0);
    }
  }
}
