import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { FormRayonComponent } from './form-rayon/form-rayon.component';
import { RayonService } from './rayon.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IRayon } from '../../shared/model/rayon.model';
import { IMagasin, IStorage } from '../../shared/model/magasin.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { Panel } from 'primeng/panel';
import { Select } from 'primeng/select';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../shared/error.service';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { FileUploadDialogComponent } from '../groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { finalize, takeUntil } from 'rxjs/operators';
import { CloneFormComponent } from './clone-form/clone-form.component';
import { MagasinService } from '../magasin/magasin.service';
import { StorageService } from '../storage/storage.service';
import { Storage } from '../storage/storage.model';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-rayon',
  templateUrl: './rayon.component.html',
  styleUrl: './rayon.component.scss',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
    ToastAlertComponent,
    Panel,
    Select,
    ReactiveFormsModule,
    SpinnerComponent
  ]
})
export class RayonComponent implements OnInit, OnDestroy {
  protected magasin?: IMagasin;
  protected storage?: IStorage;
  protected magasins: IMagasin[] = [];
  protected storages: IStorage[] = [];
  protected displayDialog?: boolean;
  protected fileDialog?: boolean;
  protected responseDialog?: boolean;
  protected dialogueClone?: boolean;
  protected responsedto?: IResponseDto;
  protected loading?: boolean;
  protected entites: IRayon[] = [];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected ngbPaginationPage = 1;
  protected selectedEl: IRayon[] = [];
  private readonly entityService = inject(RayonService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
   private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly magasinService = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.fetchCurrentUserMagasin();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onChange(): void {
    this.selectedEl = [];
    this.loadPage(0);
  }

  protected loadPage(page?: number, search?: string): void {
    // const pageToLoad: number = page || this.page;
    const pageToLoad: number = page || this.page || 1;
    const query: string = search || '';
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad - 1,
        size: ITEMS_PER_PAGE,
        search: query,
        storageId: this.storage?.id
      })
      .subscribe({
        next: (res: HttpResponse<IRayon[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: err => this.onError(err)
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
          search: '',
          storageId: this.storage?.id
        })
        .subscribe({
          next: (res: HttpResponse<IRayon[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: err => this.onError(err)
        });
    }
  }

  protected onClone(): void {
    showCommonModal(
      this.modalService,
      CloneFormComponent,
      {
        rayons: this.selectedEl
      },
      () => {
        this.loadPage(0);
      },
      'xl'
    );
  }

  protected showFileDialog(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        this.spinner().show();
        this.uploadFileResponse(this.entityService.uploadRayonFile(result));
      },
      'lg'
    );
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormRayonComponent,
      {
        entity: null,
        magasin: this.magasin,
        header: 'Ajouter un nouveau rayon'
      },
      () => {
        this.loadPage(0);
      },
      'xl'
    );
  }

  protected onEdit(entity: IRayon): void {
    showCommonModal(
      this.modalService,
      FormRayonComponent,
      {
        entity: entity,
        magasin: this.magasin,
        header: 'Modification de ' + entity.libelle
      },
      () => {
        this.loadPage(0);
      },
      'xl'
    );
  }

  protected delete(entity: IRayon): void {
    if (entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  protected confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  protected confirmDialog(id: number): void {
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

  protected search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  protected fetchCurrentUserMagasin(): void {
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.magasin = magasin;
      this.findMagsinStorage(magasin.id);
    });
  }

  private findMagsinStorage(magasinId: number): void {
    this.storageService
      .fetchStorages({
        magasinId
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<Storage[]>) => {
        this.storages = res.body;
        this.storage = this.storages.find(s => s.storageType === 'Stockage principal');
        this.loadPage(0);
      });
  }

  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: err => this.onSaveError(err)
    });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
    }
    this.responseDialog = true;
    this.fileDialog = false;
    this.dialogueClone = false;
    this.loadPage(0);
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private onSuccess(data: IRayon[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.ngbPaginationPage = this.page;
    this.entites = data || [];
    this.loading = false;
  }

  private onError(error: HttpErrorResponse): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }
}
