import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { IResponseDto } from '../../../shared/util/response-dto';
import { IRemise } from '../../../shared/model/remise.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { RemiseService } from '../remise.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RemiseProduitFormModalComponent } from '../remise-produit-form-modal/remise-produit-form-modal.component';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { ToggleSwitch } from 'primeng/toggleswitch';

@Component({
  selector: 'jhi-remise-produits',
  providers: [MessageService, ConfirmationService],
  imports: [FormsModule, ToastModule, ConfirmDialogModule, TableModule, ToolbarModule, TooltipModule, ButtonModule, ToggleSwitch],
  templateUrl: './remise-produits.component.html',
})
export class RemiseProduitsComponent implements OnInit {
  responsedto!: IResponseDto;
  entites?: IRemise[];
  loading = false;
  ngModalService = inject(NgbModal);
  entityService = inject(RemiseService);
  activatedRoute = inject(ActivatedRoute);
  router = inject(Router);
  messageService = inject(MessageService);
  modalService = inject(ConfirmationService);

  loadPage(): void {
    this.loading = true;
    this.entityService.query({ typeRemise: 'PRODUIT' }).subscribe({
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

  onOpenRemiseForm(remise?: IRemise): void {
    const modalRef = this.ngModalService.open(RemiseProduitFormModalComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true,
      animation: true,
    });
    modalRef.componentInstance.entity = remise;
    modalRef.componentInstance.title = remise.id ? 'Modifier la remise' : 'Ajouter une remise produit';
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

  protected getVnoTaux(entity: IRemise): string {
    const taut = entity.grilles.filter(grille => grille.grilleType === 'VNO')[0]?.remiseValue;
    if (taut) {
      return taut + ' %';
    }
    return '';
  }

  protected getVoTaux(entity: IRemise): string {
    const taut = entity.grilles.filter(grille => grille.grilleType === 'VO')[0]?.remiseValue;
    if (taut) {
      return taut + ' %';
    }
    return '';
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
