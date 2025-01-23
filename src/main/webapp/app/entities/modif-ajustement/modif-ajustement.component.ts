import { Component, OnInit } from '@angular/core';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ModifAjustementService } from './motif-ajustement.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IMotifAjustement } from '../../shared/model/motif-ajustement.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormMotifAjustementComponent } from './form-motif-ajustement/form-motif-ajustement.component';
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
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'jhi-modif-ajustement',
    templateUrl: './modif-ajustement.component.html',
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
        DynamicDialogModule,
        FormsModule,
        FormMotifAjustementComponent,
    ]
})
export class ModifAjustementComponent implements OnInit {
  fileDialog?: boolean;
  ref?: DynamicDialogRef;
  responsedto!: IResponseDto;
  responseDialog?: boolean;
  entites?: IMotifAjustement[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: IMotifAjustement;
  loading!: boolean;
  isSaving = false;
  customUpload = true;
  displayDialog?: boolean;

  constructor(
    protected entityService: ModifAjustementService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private messageService: MessageService,
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
        size: ITEMS_PER_PAGE,
        search: query,
      })
      .subscribe({
        next: (res: HttpResponse<IMotifAjustement[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
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
        next: (res: HttpResponse<IMotifAjustement[]>) => this.onSuccess(res.body, res.headers, this.page),
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

  delete(entity: IMotifAjustement): void {
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
    this.ref = this.dialogService.open(FormMotifAjustementComponent, {
      data: { entity: null },
      width: '40%',
      header: "Ajout d'un nouveau motif",
    });
    this.ref.onClose.subscribe(() => {
      this.loadPage(0);
    });
  }

  onEdit(entity: IMotifAjustement): void {
    this.ref = this.dialogService.open(FormMotifAjustementComponent, {
      data: { entity },
      width: '40%',
      header: 'Modification de ' + entity.libelle,
    });
    this.ref.onClose.subscribe(() => {
      this.loadPage(0);
    });
  }

  protected onSaveError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: "L'opération a échouée",
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
      this.responseDialog = true;
      this.fileDialog = false;
      this.loadPage(0);
    }
  }

  protected onSuccess(data: IMotifAjustement[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/motif-ajustement'], {
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
}
