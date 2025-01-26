import { Component, OnInit } from '@angular/core';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ModifAjustementService } from './motif-ajustement.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IMotifAjustement } from '../../shared/model/motif-ajustement.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
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
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';

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
  ],
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

  constructor(
    protected entityService: ModifAjustementService,
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
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
    });
  }

  delete(entity: IMotifAjustement): void {
    this.confirmDelete(entity.id);
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  search(event: any): void {
    this.loadPage(0, event.target.value);
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
