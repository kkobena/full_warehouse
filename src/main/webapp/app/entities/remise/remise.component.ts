import { Component, inject, OnInit, viewChild } from '@angular/core';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { IResponseDto } from '../../shared/util/response-dto';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IRemise } from '../../shared/model/remise.model';
import { RemiseService } from './remise.service';

import { RemiseClientFormModalComponent } from './remise-client-form-modal/remise-client-form-modal.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../shared/error.service';
import { FormsModule } from '@angular/forms';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

@Component({
  selector: 'jhi-remise',
  imports: [
    ToolbarModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TooltipModule,
    ToggleSwitchModule,
    ConfirmDialogComponent,
    ToastAlertComponent,
  ],
  templateUrl: './remise.component.html',
  styleUrl: './remise.component.scss',
})
export class RemiseComponent implements OnInit {
  protected responsedto!: IResponseDto;
  protected entites?: IRemise[];
  protected loading = false;
  private readonly ngModalService = inject(NgbModal);
  private readonly entityService = inject(RemiseService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.loadPage();
  }

  protected loadPage(): void {
    this.loading = true;
    this.entityService.query({ typeRemise: 'CLIENT' }).subscribe({
      next: (res: HttpResponse<IRemise[]>) => this.onSuccess(res.body),
      error: () => this.onError(),
    });
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

  protected onOpenRemiseClientForm(remise?: IRemise): void {
    const modalRef = this.ngModalService.open(RemiseClientFormModalComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true,
    });
    modalRef.componentInstance.entity = remise;
    modalRef.componentInstance.title = remise?.id ? 'Modifier la remise' : 'Ajouter une remise client';
    modalRef.closed.subscribe(r => {
      this.loadPage();
    });
  }

  protected delete(entity: IRemise): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  protected confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  protected onStatusChange(entity: IRemise): void {
    this.subscribeToSaveResponse(this.entityService.changeStatus(entity));
  }

  private onSuccess(data: IRemise[] | null): void {
    //    this.router.navigate(['/remises']);
    this.entites = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }

  private onSaveSuccess(): void {
    this.loadPage();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
    this.loadPage();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IRemise>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err),
    });
  }
}
