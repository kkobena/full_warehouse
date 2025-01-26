import { Component, inject, OnInit } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ToastModule } from 'primeng/toast';
import { IResponseDto } from '../../shared/util/response-dto';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IRemise } from '../../shared/model/remise.model';
import { RemiseService } from './remise.service';
import { DropdownModule } from 'primeng/dropdown';
import { CalendarModule } from 'primeng/calendar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { StyleClassModule } from 'primeng/styleclass';
import { RemiseClientFormModalComponent } from './remise-client-form-modal/remise-client-form-modal.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';

@Component({
  selector: 'jhi-remise',
  providers: [MessageService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ConfirmDialogModule,
    DialogModule,
    ToolbarModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    RouterModule,
    TableModule,
    TooltipModule,
    KeyFilterModule,
    ToastModule,
    DropdownModule,
    CalendarModule,
    InputSwitchModule,
    StyleClassModule,
  ],
  templateUrl: './remise.component.html',
})
export class RemiseComponent implements OnInit {
  responsedto!: IResponseDto;
  entites?: IRemise[];
  loading = false;

  //types: RemiseType[] = [RemiseType.remiseProduit, RemiseType.remiseClient];

  ngModalService = inject(NgbModal);
  entityService = inject(RemiseService);
  activatedRoute = inject(ActivatedRoute);
  router = inject(Router);
  messageService = inject(MessageService);
  modalService = inject(ConfirmationService);

  loadPage(): void {
    this.loading = true;
    this.entityService.query({ typeRemise: 'CLIENT' }).subscribe({
      next: (res: HttpResponse<IRemise[]>) => this.onSuccess(res.body),
      error: () => this.onError(),
    });
  }

  lazyLoading(): void {
    this.loadPage();
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
          this.loadPage();
        });
      },
    });
  }

  onOpenRemiseClientForm(remise?: IRemise): void {
    const modalRef = this.ngModalService.open(RemiseClientFormModalComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true,
      animation: true,
    });
    modalRef.componentInstance.entity = remise;
    modalRef.componentInstance.title = remise?.id ? 'Modifier la remise' : 'Ajouter une remise client';
    modalRef.closed.subscribe(r => {
      this.loadPage();
    });
  }

  delete(entity: IRemise): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  ngOnInit(): void {
    this.loadPage();
  }

  confirmDelete(id: number): void {
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

  private onSaveError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: "L'enregistrement n'a pas été effectué!",
    });
    this.loadPage();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IRemise>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }
}
