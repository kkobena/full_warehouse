import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GammeProduitService } from './gamme-produit.service';

import { FormGammeComponent } from './form-gamme/form-gamme.component';
import { IGammeProduit } from '../../shared/model/gamme-produit.model';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IResponseDto } from '../../shared/util/response-dto';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { Panel } from 'primeng/panel';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FileUploadDialogComponent } from '../groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { finalize } from 'rxjs/operators';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-gamme-produit',
  templateUrl: './gamme-produit.component.html',
  styleUrl: './gamme-produit.component.scss',
  imports: [
    ButtonModule,
    ToolbarModule,
    TableModule,
    InputTextModule,
    TooltipModule,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
    Panel,
    SpinnerComponent
  ]
})
export class GammeProduitComponent implements OnInit {

  protected responsedto!: IResponseDto;
  protected entites: IGammeProduit[] = [];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected selectedEl?: IGammeProduit;
  protected loading = false;
  protected isSaving = false;
  private readonly entityService = inject(GammeProduitService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
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
        search: query
      })
      .subscribe({
        next: (res: HttpResponse<IGammeProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError()
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    this.page = event.first / event.rows;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows,
        search: ''
      })
      .subscribe({
        next: (res: HttpResponse<IGammeProduit[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: () => this.onError()
      });
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


  protected delete(entity: IGammeProduit): void {
    this.confirmDelete(entity.id);
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
      'lg'
    );
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormGammeComponent,
      {
        gamme: null,
        header: 'Ajout d\'une nouvelle gamme de produit'
      },
      () => {
        this.loadPage(0);
      },
      'lg'
    );
  }

  protected onEdit(entity: IGammeProduit): void {
    showCommonModal(
      this.modalService,
      FormGammeComponent,
      {
        gamme: entity,
        header: 'Modification de ' + entity.libelle
      },
      () => {
        this.loadPage(0);
      },
      'lg'
    );
  }

  private onSuccess(data: IGammeProduit[] | null, headers: HttpHeaders, page: number): void {
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
  }


  private uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      error: () => this.onSaveError()
    });
  }

  private onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) {
      this.responsedto = responseDto;
    }
    this.loadPage(0);
  }
}
