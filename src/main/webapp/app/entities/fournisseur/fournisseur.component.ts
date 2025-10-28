import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FournisseurService } from './fournisseur.service';
import { RouterModule } from '@angular/router';
import { SelectItem } from 'primeng/api';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';

import { GroupeFournisseurService } from '../groupe-fournisseur/groupe-fournisseur.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IGroupeFournisseur } from '../../shared/model/groupe-fournisseur.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { TooltipModule } from 'primeng/tooltip';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { FournisseurUpdateComponent } from './fournisseur-update.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { FileUploadDialogComponent } from '../groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { ErrorService } from '../../shared/error.service';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-fournisseur',
  templateUrl: './fournisseur.component.html',
  styleUrl: './fournisseur.component.scss',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    KeyFilterModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    ConfirmDialogComponent,
    ToastAlertComponent,
    SpinnerComponent
  ]
})
export class FournisseurComponent implements OnInit {
  protected responsedto!: IResponseDto;
  protected entites: IFournisseur[] = [];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected loading = false;
  protected groupes: SelectItem[] = [];
  private readonly entityService = inject(FournisseurService);
  private readonly groupeFournisseurService = inject(GroupeFournisseurService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);
   private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.loadPage();
    this.groupeFournisseurService
      .query({
        search: ''
      })
      .subscribe((res: HttpResponse<IGroupeFournisseur[]>) => {
        if (res.body) {
          res.body.forEach(item => {
            this.groupes.push({ label: item.libelle, value: item.id });
          });
        }
      });
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
        next: (res: HttpResponse<IFournisseur[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: err => this.onError(err)
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    this.page = event.first / event.rows;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows
      })
      .subscribe({
        next: (res: HttpResponse<IFournisseur[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: err => this.onError(err)
      });
  }

  protected confirmDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.entityService.delete(id).subscribe({
          next: () => {
            this.loadPage(0);
          },
          error: err => this.onError(err)
        });
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir supprimer cet fournisseur ?'
    );
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FournisseurUpdateComponent,
      { fournisseur: null, header: 'Ajout de nouveau fournisseur' },
      () => {
        this.loadPage();
      },
      'xl'
    );
  }

  protected onEdit(entity: IFournisseur): void {
    showCommonModal(
      this.modalService,
      FournisseurUpdateComponent,
      { fournisseur: entity, header: `Modification du fournisseur [ ${entity.libelle} ]` },
      () => {
        this.loadPage();
      },
      'xl'
    );
  }

  protected delete(entity: IFournisseur): void {
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

  protected showFileDialog(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        this.spinner().show();
        this.uploadFileResponse(this.entityService.uploadFile(result));
      },
      'xl'
    );
  }

  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: (res: HttpResponse<IResponseDto>) => {
        this.alert().showInfo('Fichier importé avec succès');
      },
      error: err => this.onError(err)
    });
  }

  private onSuccess(data: IFournisseur[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.entites = data || [];
    this.loading = false;
  }

  private onError(error: HttpErrorResponse): void {
    this.loading = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }
}
