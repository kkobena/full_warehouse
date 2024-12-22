import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { FormRayonComponent } from './form-rayon/form-rayon.component';
import { RayonService } from './rayon.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IRayon } from '../../shared/model/rayon.model';
import { IMagasin } from '../../shared/model/magasin.model';
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
import { DropdownModule } from 'primeng/dropdown';
import { DialogModule } from 'primeng/dialog';
import { MagasinService } from '../magasin/magasin.service';

@Component({
  selector: 'jhi-rayon',
  templateUrl: './rayon.component.html',
  styleUrls: ['./rayon.component.scss'],
  providers: [MessageService, DialogService, ConfirmationService],
  standalone: true,
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    ConfirmDialogModule,
    ToastModule,
    DropdownModule,
    FileUploadModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    DynamicDialogModule,
    FormsModule,
    DialogModule,
  ],
})
export class RayonComponent implements OnInit {
  magasin?: IMagasin;
  clone?: IMagasin;
  magasins: IMagasin[];
  displayDialog?: boolean;
  fileDialog?: boolean;
  responseDialog?: boolean;
  dialogueClone?: boolean;
  responsedto?: IResponseDto;
  ref?: DynamicDialogRef;
  loading?: boolean;
  entites: IRayon[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  ngbPaginationPage = 1;
  customUpload = true;
  selectedEl: IRayon[];
  multipleSite = false;

  constructor(
    protected entityService: RayonService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: ConfirmationService,
    private dialogService: DialogService,
    private messageService: MessageService,
    private magasinService: MagasinService,
  ) {
    this.magasins = [];
    this.selectedEl = [];
    this.entites = [];
  }

  ngOnInit(): void {
    this.findUserMagasin();
    this.loadPage(0);
  }

  onUpload(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.uploadFileResponse(this.entityService.uploadRayonFile(formData));
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  onChange(event: any): void {
    this.magasin = event.value;
    this.selectedEl = [];
    this.loadPage(0);
  }

  loadPage(page?: number, search?: string): void {
    // const pageToLoad: number = page || this.page;
    const pageToLoad: number = page || this.page || 1;
    const query: string = search || '';
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad - 1,
        size: ITEMS_PER_PAGE,
        search: query,
      })
      .subscribe({
        next: (res: HttpResponse<IRayon[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
          search: '',
          magasinId: this.magasin?.id,
        })
        .subscribe({
          next: (res: HttpResponse<IRayon[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  showFileDialog(): void {
    this.fileDialog = true;
  }

  addNewEntity(): void {
    this.ref = this.dialogService.open(FormRayonComponent, {
      data: { entity: null, magasin: this.magasin },
      width: '50%',
      header: 'Ajouter un nouveau rayon',
    });
    this.ref.onClose.subscribe((entity: IRayon) => {
      if (entity) {
        this.loadPage(0);
      }
    });
  }

  onEdit(entity: IRayon): void {
    this.ref = this.dialogService.open(FormRayonComponent, {
      data: entity,
      width: '50%',
      header: 'Modification de ' + entity.libelle,
    });

    this.ref.onClose.subscribe((e: IRayon) => {
      if (e) {
        this.loadPage(0);
      }
    });
  }

  delete(entity: IRayon): void {
    if (entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
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

  search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  cloner(): void {
    this.dialogueClone = true;
  }

  onCloneChange(event: any): void {
    if (this.clone.id === this.magasin?.id) {
      this.clone = undefined;
      this.messageService.add({
        severity: 'error',
        summary: 'Avertissement',
        detail: 'Le point de stockage de destination doit être différent',
      });
    } else {
      this.clone = event.value;
    }
  }

  clonerRayon(): void {
    //  const rayons = this.selectedEl.map(e => e.id);
    if (this.selectedEl && this.clone) {
      this.uploadFileResponse(this.entityService.cloner(this.selectedEl, this.clone.id));
    }
  }

  onBasculer(entity: IRayon): void {}

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
    this.dialogueClone = false;
    this.loadPage(0);
  }

  protected onSaveError(): void {}

  protected onSuccess(data: IRayon[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.ngbPaginationPage = this.page;
    this.entites = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
  }

  private findUserMagasin(): void {
    /*  this.magasinService.findConnectedUserStockages().then(magasin => {
        this.magasins = magasin;

      });*/
  }
}
