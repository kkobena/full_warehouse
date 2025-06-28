import { Component, inject, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupeFournisseurService } from './groupe-fournisseur.service';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IGroupeFournisseur } from '../../shared/model/groupe-fournisseur.model';
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
import { KeyFilterModule } from 'primeng/keyfilter';
import { TooltipModule } from 'primeng/tooltip';
import { TextareaModule } from 'primeng/textarea';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Panel } from 'primeng/panel';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormGroupeFournisseurComponent } from './form/form-groupe-fournisseur.component';

@Component({
  selector: 'jhi-groupe-fournisseur',
  templateUrl: './groupe-fournisseur.component.html',
  providers: [MessageService, ConfirmationService, DialogService],
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
    FormsModule,
    ReactiveFormsModule,
    TextareaModule,
    InputTextModule,
    KeyFilterModule,
    TooltipModule,
    IconField,
    InputIcon,
    Panel,
  ],
})
export class GroupeFournisseurComponent implements OnInit {
  protected fileDialog?: boolean;
  protected responsedto!: IResponseDto;
  protected responseDialog?: boolean;
  protected entites?: IGroupeFournisseur[];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected selectedEl?: IGroupeFournisseur;
  protected loading = false;
  protected isSaving = false;
  protected displayDialog?: boolean;
  private readonly router = inject(Router);
  private readonly modalService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly entityService = inject(GroupeFournisseurService);
  private ref!: DynamicDialogRef;
  private readonly dialogService = inject(DialogService);

  ngOnInit(): void {
    this.loadPage();
  }

  onUpload(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.uploadFileResponse(this.entityService.uploadFile(formData));
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
        next: (res: HttpResponse<IGroupeFournisseur[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
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
        next: (res: HttpResponse<IGroupeFournisseur[]>) => this.onSuccess(res.body, res.headers, this.page),
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

  onEdit(entity: IGroupeFournisseur): void {
    this.ref = this.dialogService.open(FormGroupeFournisseurComponent, {
      data: { entity: entity },
      header: `FORMULAIRE DE MODIFICATION DE ${entity.libelle}`,
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: IGroupeFournisseur) => {
      if (resp) {
        this.loadPage();
      }
    });
  }

  delete(entity: IGroupeFournisseur): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  addNewEntity(): void {
    this.ref = this.dialogService.open(FormGroupeFournisseurComponent, {
      data: { entity: null },
      header: 'FORMULAIRE DE CREATION DE GROUPE FOURNISSEUR ',
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: IGroupeFournisseur) => {
      if (resp) {
        this.loadPage(0);
      }
    });
  }

  showFileDialog(): void {
    this.fileDialog = true;
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

  protected onSuccess(data: IGroupeFournisseur[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/groupe-fournisseur'], {
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

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'info',
      summary: 'Enregistrement',
      detail: 'Opération effectuée avec succès',
    });
  }
}
