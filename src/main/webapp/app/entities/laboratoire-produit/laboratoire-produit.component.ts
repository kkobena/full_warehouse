import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LaboratoireProduitService } from './laboratoire-produit.service';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
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

@Component({
    selector: 'jhi-laboratoire-produit',
    templateUrl: './laboratoire-produit.component.html',
    styles: [
        `
      body .ui-inputtext {
        width: 100% !important;
      }
    `,
    ],
    providers: [MessageService, DialogService, ConfirmationService],
    encapsulation: ViewEncapsulation.None,
    imports: [
        WarehouseCommonModule,
        DialogModule,
        ButtonModule,
        RippleModule,
        ConfirmDialogModule,
        ToastModule,
        FileUploadModule,
        ToolbarModule,
        TableModule,
        RouterModule,
        InputTextModule,
        TooltipModule,
        DynamicDialogModule,
        FormsModule,
        FormLaboratoireComponent,
    ]
})
export class LaboratoireProduitComponent implements OnInit {
  fileDialog = false;
  ref?: DynamicDialogRef;
  responsedto!: IResponseDto;
  responseDialog = false;
  entites: ILaboratoire[] = [];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: ILaboratoire;
  loading = false;
  isSaving = false;
  displayDialog = false;

  constructor(
    protected entityService: LaboratoireProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private dialogService: DialogService,
    protected modalService: ConfirmationService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(() => {
      this.loadPage();
    });
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
        next: (res: HttpResponse<ILaboratoire[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
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
      })
      .subscribe({
        next: (res: HttpResponse<ILaboratoire[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: () => this.onError(),
      });
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
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

  delete(entity: ILaboratoire): void {
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
    this.ref = this.dialogService.open(FormLaboratoireComponent, {
      data: { laboratoire: null },
      width: '40%',

      header: "Ajout d'un nouveau laboratoire",
    });
    this.ref.onClose.subscribe((entity: ILaboratoire) => {
      if (entity) {
        this.loadPage(0);
      }
    });
  }

  onEdit(entity: ILaboratoire): void {
    this.ref = this.dialogService.open(FormLaboratoireComponent, {
      data: { laboratoire: entity },
      width: '40%',
      header: 'Modification de ' + entity.libelle,
    });
    this.ref.onClose.subscribe((e: ILaboratoire) => {
      if (e) {
        this.loadPage(0);
      }
    });
  }

  protected onSuccess(data: ILaboratoire[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/laboratoire'], {
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
