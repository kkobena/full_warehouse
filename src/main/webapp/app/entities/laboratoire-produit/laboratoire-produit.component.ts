import { Component, inject, OnInit, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { LazyLoadEvent } from 'primeng/api';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LaboratoireProduitService } from './laboratoire-produit.service';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { FormLaboratoireComponent } from './form-laboratoire/form-laboratoire.component';
import { IResponseDto } from '../../shared/util/response-dto';
import { ILaboratoire } from '../../shared/model/laboratoire.model';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Panel } from 'primeng/panel';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { SpinerService } from '../../shared/spiner.service';
import { FileUploadDialogComponent } from '../groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { finalize } from 'rxjs/operators';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../shared/error.service';

@Component({
  selector: 'jhi-laboratoire-produit',
  templateUrl: './laboratoire-produit.component.html',
  imports: [
    ButtonModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
    IconField,
    InputIcon,
    Panel,
    ConfirmDialogComponent,
    ToastAlertComponent
  ]
})
export class LaboratoireProduitComponent implements OnInit {
  protected responsedto!: IResponseDto;
  protected entites: ILaboratoire[] = [];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected loading = false;
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly modalService = inject(NgbModal);
  private readonly spinner = inject(SpinerService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly entityService = inject(LaboratoireProduitService);

  ngOnInit(): void {
    this.loadPage();

  }

  protected loadPage(page?: number, search?: string): void {
    const pageToLoad: number = page || this.page;
    const query: string = search || '';
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: query
      })
      .subscribe({
        next: (res: HttpResponse<ILaboratoire[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: (err) => this.onError(err)
      });
  }

  protected lazyLoading(event: LazyLoadEvent): void {
    this.page = event.first / event.rows;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows
      })
      .subscribe({
        next: (res: HttpResponse<ILaboratoire[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: (err) => this.onError(err)
      });
  }

  private confirmDialog(id: number): void {
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


  protected delete(entity: ILaboratoire): void {
    this.confirmDialog(entity.id);
  }



  protected search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  protected showFileDialog(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        this.spinner.show();
        this.uploadFileResponse(this.entityService.uploadFile(result));
      },
      'lg'
    );
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormLaboratoireComponent,
      {
        laboratoire: null,
        header: 'Ajout d\'un nouveau laboratoire'
      },
      () => {
        this.loadPage(0);
      },
      'lg'
    );
  }

  protected onEdit(entity: ILaboratoire): void {
    showCommonModal(
      this.modalService,
      FormLaboratoireComponent,
      {
        laboratoire: entity,
        header: 'Modification de ' + entity.libelle
      },
      () => {
        this.loadPage(0);
      },
      'lg'
    );
  }

  private onSuccess(data: ILaboratoire[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.entites = data || [];
    this.loading = false;
  }

  private onError(error: HttpErrorResponse): void {
    this.loading = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }


  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.pipe(finalize(() => this.spinner.hide())).subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: (err) => this.onSaveError(err)
    });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
    }
    this.loadPage(0);
  }
}
