import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupeFournisseurService } from './groupe-fournisseur.service';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IGroupeFournisseur } from '../../shared/model/groupe-fournisseur.model';
import { IResponseDto } from '../../shared/util/response-dto';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';

import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { TooltipModule } from 'primeng/tooltip';
import { TextareaModule } from 'primeng/textarea';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { FormGroupeFournisseurComponent } from './form/form-groupe-fournisseur.component';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FileUploadDialogComponent } from '../groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { finalize } from 'rxjs/operators';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-groupe-fournisseur',
  templateUrl: './groupe-fournisseur.component.html',
  styleUrl: './groupe-fournisseur.component.scss',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
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
    ConfirmDialogComponent,
    ToastAlertComponent,
    SpinnerComponent,
  ],
})
export class GroupeFournisseurComponent implements OnInit {
  protected responsedto!: IResponseDto;
  protected responseDialog?: boolean;
  protected entites?: IGroupeFournisseur[];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected loading = false;
  protected isSaving = false;
  protected displayDialog?: boolean;
  private readonly entityService = inject(GroupeFournisseurService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

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
        search: query,
      })
      .subscribe({
        next: (res: HttpResponse<IGroupeFournisseur[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
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

  protected confirmDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir supprimer cet groupe .',
    );
  }

  protected onEdit(entity: IGroupeFournisseur): void {
    showCommonModal(
      this.modalService,
      FormGroupeFournisseurComponent,
      {
        entity: entity,
        header: `FORMULAIRE DE MODIFICATION DE ${entity.libelle}`,
      },
      () => {
        this.loadPage();
      },
      'xl',
    );
  }

  protected delete(entity: IGroupeFournisseur): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  protected confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  protected search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormGroupeFournisseurComponent,
      {
        entity: null,
        header: 'FORMULAIRE DE CREATION DE GROUPE FOURNISSEUR ',
      },
      () => {
        this.loadPage(0);
      },
      'xl',
    );
  }

  protected showFileDialog(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        this.spinner().show();
        this.uploadFileResponse(this.entityService.uploadFile(result));
      },
      'xl',
    );
  }

  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
    }
    this.responseDialog = true;
    this.loadPage(0);
  }

  private onSuccess(data: IGroupeFournisseur[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.entites = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }

  private onSaveError(): void {
    this.isSaving = false;
    this.alert().showInfo();
  }
}
