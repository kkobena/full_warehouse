import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, inject, OnInit, viewChild } from '@angular/core';
import { RouterModule } from '@angular/router';
import { PosteService } from './poste.service';
import { IPoste } from '../../shared/model/poste.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../shared/error.service';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { FormPosteComponent } from './form-poste/form-poste.component';
import { Tag } from 'primeng/tag';

@Component({
  selector: 'jhi-poste',
  templateUrl: './poste.component.html',
  styleUrl: './poste.component.scss',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    InputTextModule,
    TooltipModule,
    FormsModule,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
    ToastAlertComponent,
    Tag
  ]
})
export class PosteComponent implements OnInit {
  protected loading?: boolean;
  protected entites: IPoste[] = [];
  protected filteredEntities: IPoste[] = [];
  protected searchQuery = '';
  private readonly entityService = inject(PosteService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading = true;
    this.entityService.fetchAll().subscribe({
      next: (res: HttpResponse<IPoste[]>) => this.onSuccess(res.body),
      error: err => this.onError(err)
    });
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormPosteComponent,
      {
        entity: null,
        title: 'Ajouter un nouveau poste'
      },
      () => {
        this.loadAll();
      },
      'lg'
    );
  }

  protected onEdit(entity: IPoste): void {
    showCommonModal(
      this.modalService,
      FormPosteComponent,
      {
        entity: entity,
        title: 'Modification de ' + entity.name
      },
      () => {
        this.loadAll();
      },
      'lg'
    );
  }

  protected delete(entity: IPoste): void {
    if (entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  protected confirmDelete(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadAll();
        });
      },
      'Suppression',
      'Êtes-vous sûr de vouloir supprimer ce poste ?'
    );
  }

  protected search(event: any): void {
    this.searchQuery = event.target.value.toLowerCase();
    this.applyFilter();
  }

  private applyFilter(): void {
    if (!this.searchQuery) {
      this.filteredEntities = [...this.entites];
    } else {
      this.filteredEntities = this.entites.filter(
        entity =>
          entity.name?.toLowerCase().includes(this.searchQuery) ||
          entity.address?.toLowerCase().includes(this.searchQuery) ||
          entity.posteNumber?.toLowerCase().includes(this.searchQuery)
      );
    }
  }

  private onSuccess(data: IPoste[] | null): void {
    this.entites = data || [];
    this.filteredEntities = [...this.entites];
    this.loading = false;
  }

  private onError(error: HttpErrorResponse): void {
    this.loading = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }
}
