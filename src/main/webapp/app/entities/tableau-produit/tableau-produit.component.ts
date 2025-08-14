import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { TableauProduitService } from './tableau-produit.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITableau } from '../../shared/model/tableau.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { Panel } from 'primeng/panel';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormTableauComponent } from './form-tableau/form-tableau.component';

@Component({
  selector: 'jhi-groupe-fournisseur',
  templateUrl: './tableau-produit.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ToolbarModule,
    ButtonModule,
    InputTextModule,
    RouterModule,
    TableModule,
    TooltipModule,
    Panel,
    ConfirmDialogComponent,
  ],
})
export class TableauProduitComponent implements OnInit {
  protected fileDialog?: boolean;
  protected responsedto!: IResponseDto;
  protected responseDialog?: boolean;
  protected entites?: ITableau[];
  protected selectedEl?: ITableau;
  protected loading = false;
  protected displayDialog?: boolean;
  private readonly entityService = inject(TableauProduitService);
  private readonly router = inject(Router);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly ngModalService = inject(NgbModal);
  ngOnInit(): void {
    this.loadPage();
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  protected loadPage(): void {
    this.loading = true;
    this.entityService.query().subscribe({
      next: (res: HttpResponse<ITableau[]>) => this.onSuccess(res.body),
      error: () => this.onError(),
    });
  }

  protected lazyLoading(): void {
    this.loadPage();
  }

  protected confirmDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
      'Confirmation',
      'Voulez-vous supprimer cet enregistrement ?',
    );
  }

  protected addNewEntity(): void {
    const modalRef = this.ngModalService.open(FormTableauComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true,
    });
    modalRef.componentInstance.entity = null;
    modalRef.componentInstance.header = 'Ajouter un tableau';
    modalRef.result.then(r => {
      this.loadPage();
    });
    this.displayDialog = true;
  }

  protected onEdit(entity: ITableau): void {
    const modalRef = this.ngModalService.open(FormTableauComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true,
    });
    modalRef.componentInstance.entity = entity;
    modalRef.componentInstance.header = `Modifier un tableau [ ${entity.code} ]`;
    modalRef.result.then(r => {
      this.loadPage();
    });
    this.displayDialog = true;
  }

  protected delete(entity: ITableau): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  protected confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  protected showFileDialog(): void {
    this.fileDialog = true;
  }

  private onSuccess(data: ITableau[] | null): void {
    this.router.navigate(['/tableaux']);
    this.entites = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }
}
