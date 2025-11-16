import { Component, inject, OnInit, viewChild } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ModifAjustementService } from './motif-ajustement.service';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { IMotifAjustement } from '../../shared/model/motif-ajustement.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { FormMotifAjustementComponent } from './form-motif-ajustement/form-motif-ajustement.component';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

@Component({
  selector: 'jhi-modif-ajustement',
  templateUrl: './modif-ajustement.component.html',
  styleUrl: './modif-ajustement.component.scss',
  imports: [
    ButtonModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
    ConfirmDialogComponent,
    IconField,
    InputIcon,
  ],
})
export class ModifAjustementComponent implements OnInit {
  protected entites?: IMotifAjustement[];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected loading!: boolean;
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly modalService = inject(NgbModal);
  private readonly entityService = inject(ModifAjustementService);

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
        size: ITEMS_PER_PAGE,
        search: query,
      })
      .subscribe({
        next: (res: HttpResponse<IMotifAjustement[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
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
        next: (res: HttpResponse<IMotifAjustement[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: () => this.onError(),
      });
  }

  private confirmDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
      'Suppression',
      'Êtes-vous sûr de vouloir supprimer ?',
    );
  }

  protected delete(entity: IMotifAjustement): void {
    this.confirmDialog(entity.id);
  }

  protected search(event: any): void {
    this.loadPage(0, event.target.value);
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormMotifAjustementComponent,
      {
        entity: null,
        header: "Ajout d'un nouveau motif d'ajustement",
      },
      () => {
        this.loadPage(0);
      },
      'lg',
    );
  }

  protected onEdit(entity: IMotifAjustement): void {
    showCommonModal(
      this.modalService,
      FormMotifAjustementComponent,
      {
        entity: entity,
        header: 'Modification de ' + entity.libelle,
      },
      () => {
        this.loadPage(0);
      },
      'lg',
    );
  }

  private onSuccess(data: IMotifAjustement[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;

    this.entites = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }
}
