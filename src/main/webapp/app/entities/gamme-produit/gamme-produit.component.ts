import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GammeProduitService } from './gamme-produit.service';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { FormGammeComponent } from './form-gamme/form-gamme.component';
import { IGammeProduit } from '../../shared/model/gamme-produit.model';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IResponseDto } from '../../shared/util/response-dto';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';

@Component({
  selector: 'jhi-gamme-produit',
  templateUrl: './gamme-produit.component.html',
  styles: [
    `
      body .ui-inputtext {
        width: 100% !important;
      }
    `,
  ],
  providers: [MessageService, DialogService, ConfirmationService],

  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    ConfirmDialogModule,
    ToastModule,
    DialogModule,
    FileUploadModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    IconField,
    InputIcon,
  ],
})
export class GammeProduitComponent implements OnInit {
  protected entityService = inject(GammeProduitService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  private dialogService = inject(DialogService);
  protected modalService = inject(ConfirmationService);

  fileDialog = false;
  ref?: DynamicDialogRef;
  responsedto!: IResponseDto;
  responseDialog = false;
  entites: IGammeProduit[] = [];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: IGammeProduit;
  loading = false;
  isSaving = false;
  displayDialog = false;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

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
        size: this.itemsPerPage,
        search: query,
      })
      .subscribe({
        next: (res: HttpResponse<IGammeProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    this.page = event.first / event.rows;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows,
        search: '',
      })
      .subscribe({
        next: (res: HttpResponse<IGammeProduit[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: () => this.onError(),
      });
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
    });
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  delete(entity: IGammeProduit): void {
    this.confirmDelete(entity.id);
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  onUpload(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.uploadFileResponse(this.entityService.uploadFile(formData));
  }

  search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  showFileDialog(): void {
    this.fileDialog = true;
  }

  addNewEntity(): void {
    this.ref = this.dialogService.open(FormGammeComponent, {
      data: { gamme: null },
      width: '40%',

      header: "Ajout d'une nouvelle gamme de produit",
    });
    this.ref.onClose.subscribe((entity: IGammeProduit) => {
      if (entity) {
        this.loadPage(0);
      }
    });
  }

  onEdit(entity: IGammeProduit): void {
    this.ref = this.dialogService.open(FormGammeComponent, {
      data: { gamme: entity },
      width: '40%',

      header: 'Modification de ' + entity.libelle,
    });
    this.ref.onClose.subscribe((e: IGammeProduit) => {
      if (e) {
        this.loadPage(0);
      }
    });
  }

  protected onSuccess(data: IGammeProduit[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/gamme-produit'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
      },
    });
    this.entites = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.displayDialog = false;
    this.loadPage(0);
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGammeProduit>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
    }
    this.responseDialog = true;
    this.fileDialog = false;
    this.loadPage(0);
  }
}
