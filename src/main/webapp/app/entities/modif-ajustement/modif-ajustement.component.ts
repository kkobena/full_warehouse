import { Component, OnInit } from '@angular/core';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ActivatedRoute, Router } from '@angular/router';
import { ModifAjustementService } from './motif-ajustement.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IMotifAjustement } from '../../shared/model/motif-ajustement.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormMotifAjustementComponent } from './form-motif-ajustement/form-motif-ajustement.component';

@Component({
  selector: 'jhi-modif-ajustement',
  templateUrl: './modif-ajustement.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
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
    protected modalService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(() => {
      this.loadPage();
    });
  }

  loadPage(page?: number, search?: String): void {
    const pageToLoad: number = page || this.page;
    const query: String = search || '';
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: ITEMS_PER_PAGE,
        search: query,
      })
      .subscribe(
        (res: HttpResponse<IMotifAjustement[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        () => this.onError()
      );
  }

  lazyLoading(event: LazyLoadEvent): void {
    this.page = event.first! / event.rows!;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows,
        search: '',
      })
      .subscribe(
        (res: HttpResponse<IMotifAjustement[]>) => this.onSuccess(res.body, res.headers, this.page),
        () => this.onError()
      );
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

  protected onSaveError(): void {
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "L'opération a échouée" });
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  delete(entity: IMotifAjustement): void {
    this.confirmDelete(entity.id!);
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

  protected uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe(
      (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      () => this.onSaveError()
    );
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
}
