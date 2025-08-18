import { Component, inject, OnInit, viewChild } from '@angular/core';
import { TvaService } from './tva.service';
import { HttpResponse } from '@angular/common/http';
import { ITva } from '../../shared/model/tva.model';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { Panel } from 'primeng/panel';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { FormTvaComponent } from './form-tva/form-tva.component';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'jhi-tva',
  templateUrl: './tva.component.html',
  imports: [CommonModule, ButtonModule, TableModule, Panel, Toolbar, Tooltip, ConfirmDialogComponent, TranslatePipe]
})
export class TvaComponent implements OnInit {
  protected tvas?: ITva[];
  protected selectedTva?: ITva;
  protected loading!: boolean;
  protected isSaving = false;
  protected displayDialog?: boolean;
  private readonly tvaService = inject(TvaService);
  private readonly modalService = inject(NgbModal);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  ngOnInit(): void {
    this.loadPage();
  }

  delete(tva: ITva): void {
    if (tva) {
      this.confirmDelete(tva.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  confirmDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.tvaService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
      'Confirmation',
      'Voulez-vous supprimer cet enregistrement ?'
    );
  }

  protected loadPage(): void {
    this.loading = true;
    this.tvaService
      .query({
        page: 0,
        size: 100
      })
      .subscribe({
        next: (res: HttpResponse<ITva[]>) => this.onSuccess(res.body),
        error: () => this.onError()
      });
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormTvaComponent,
      {
        header: 'Ajouter un taux tva'
      },
      () => {
        this.loadPage();
      },
      'sm'
    );
  }

  private onSuccess(data: ITva[] | null): void {
    this.tvas = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }
}
