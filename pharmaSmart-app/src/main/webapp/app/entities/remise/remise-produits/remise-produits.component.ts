import {ChangeDetectionStrategy, Component, inject, OnInit, input, signal } from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgbModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {IRemise} from '../../../shared/model';
import {RemiseService} from '../remise.service';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {
  RemiseProduitFormModalComponent
} from '../remise-produit-form-modal/remise-produit-form-modal.component';
import {ErrorService} from '../../../shared/error.service';
import {NotificationService} from "../../../shared/services/notification.service";
import {
  NgbConfirmDialogService
} from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {ButtonComponent, DataTableComponent, SwitchComponent, ToolbarComponent} from '../../../shared/ui';

@Component({
  selector: 'app-remise-produits',
  imports: [
    FormsModule,
    NgbTooltip,
    ButtonComponent,
    DataTableComponent,
    SwitchComponent,
    ToolbarComponent,
  ],
  templateUrl: './remise-produits.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './remise-produits.component.scss',
})
export class RemiseProduitsComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  protected readonly entites = signal<IRemise[] | undefined>(undefined);
  protected readonly loading = signal(false);
  private readonly ngModalService = inject(NgbModal);
  private readonly entityService = inject(RemiseService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly errorService = inject(ErrorService);

  loadPage(): void {
    this.loading.set(true);
    this.entityService.query({typeRemise: 'PRODUIT'}).subscribe({
      next: (res: HttpResponse<IRemise[]>) => this.onSuccess(res.body),
      error: () => this.onError(),
    });
  }

  lazyLoading(): void {
    this.loadPage();
  }

  confirm(id: number): void {
    this.confirmDialog.onConfirm(
      () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
      'Confirmation',
      'Voulez-vous supprimer cet enregistrement ?',
    );
  }

  onOpenRemiseForm(remise?: IRemise): void {
    const modalRef = this.ngModalService.open(RemiseProduitFormModalComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true,
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
    this.confirm(id);
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
    this.entites.set(data || []);
    this.loading.set(false);
  }

  private onError(): void {
    this.loading.set(false);
  }

  private onSaveSuccess(): void {
    this.loadPage();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
    this.loadPage();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IRemise>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err),
    });
  }
}
