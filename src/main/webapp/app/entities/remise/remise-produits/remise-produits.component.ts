import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { IResponseDto } from '../../../shared/util/response-dto';
import { IRemise } from '../../../shared/model/remise.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { RemiseService } from '../remise.service';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RemiseProduitFormModalComponent } from '../remise-produit-form-modal/remise-produit-form-modal.component';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';

@Component({
  selector: 'jhi-remise-produits',
  imports: [
    FormsModule,
    TableModule,
    ToolbarModule,
    TooltipModule,
    ButtonModule,
    ToggleSwitch,
    ConfirmDialogComponent,
    ToastAlertComponent
  ],
  templateUrl: './remise-produits.component.html',
  styleUrl: './remise-produits.component.scss',
})
export class RemiseProduitsComponent implements OnInit {
  protected responsedto!: IResponseDto;
  protected entites?: IRemise[];
  protected loading = false;
  private readonly ngModalService = inject(NgbModal);
  private readonly entityService = inject(RemiseService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);

  loadPage(): void {
    this.loading = true;
    this.entityService.query({ typeRemise: 'PRODUIT' }).subscribe({
      next: (res: HttpResponse<IRemise[]>) => this.onSuccess(res.body),
      error: () => this.onError()
    });
  }

  lazyLoading(): void {
    this.loadPage();
  }

  confirmDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
      'Confirmation',
      'Voulez-vous supprimer cet enregistrement ?'
    );
  }

  onOpenRemiseForm(remise?: IRemise): void {
    const modalRef = this.ngModalService.open(RemiseProduitFormModalComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true
    });
    modalRef.componentInstance.entity = remise;
    modalRef.componentInstance.title = remise?.id ? 'Modifier la remise' : 'Ajouter une remise produit';
    modalRef.result.then(r => {
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
    const taut = entity.grilles?.filter(grille => grille.grilleType === 'VNO')[0]?.remiseValue;
    if (taut) {
      return taut + ' %';
    }
    return '';
  }

  protected getVoTaux(entity: IRemise): string {
    const taut = entity.grilles?.filter(grille => grille.grilleType === 'VO')[0]?.remiseValue;
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

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
    this.loadPage();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IRemise>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err)
    });
  }
}
