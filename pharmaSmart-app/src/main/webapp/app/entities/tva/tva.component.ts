import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {TvaService} from './tva.service';
import {HttpResponse} from '@angular/common/http';
import {ITva} from '../../shared/model/tva.model';
import {ButtonComponent, DataTableComponent, SelectableRowDirective} from '../../shared/ui';
import {NgbModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {showCommonModal} from '../sales/selling-home/sale-helper';
import {FormTvaComponent} from './form-tva/form-tva.component';
import {CommonModule} from '@angular/common';
import {NotificationService} from '../../shared/services/notification.service';
import {ErrorService} from '../../shared/error.service';
import {TranslatePipe} from '@ngx-translate/core';
import {
  NgbConfirmDialogService
} from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

@Component({
  selector: 'app-tva',
  templateUrl: './tva.component.html',
  styleUrl: './tva.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslatePipe, ButtonComponent, DataTableComponent, SelectableRowDirective, NgbTooltip],
})
export class TvaComponent implements OnInit {
  protected readonly tvas = signal<ITva[] | undefined>(undefined);
  protected selectedTva: ITva | null = null;
  protected readonly loading = signal<boolean | undefined>(undefined);
  private readonly tvaService = inject(TvaService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  ngOnInit(): void {
    this.loadPage();
  }

  delete(tva: ITva): void {
    if (tva) {
      this.confirmDelete(tva.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirm(id);
  }

  confirm(id: number): void {
    this.confirmDialog.onConfirm(
      () => {
        // Sans gestionnaire d'erreur, le refus du serveur — « encore utilisé par N
        // produit(s) » — se perdait : la ligne restait et l'on croyait à un échec du clic.
        this.tvaService.delete(id).subscribe({
          next: () => this.loadPage(),
          error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
        });
      },
      'Confirmation',
      'Voulez-vous supprimer cet enregistrement ?',
    );
  }

  protected loadPage(): void {
    this.loading.set(true);
    this.tvaService
      .query({
        page: 0,
        size: 100,
      })
      .subscribe({
        next: (res: HttpResponse<ITva[]>) => this.onSuccess(res.body),
        error: () => this.onError(),
      });
  }

  protected addNewEntity(): void {
    showCommonModal(
      this.modalService,
      FormTvaComponent,
      {
        header: 'Ajouter un taux tva',
      },
      () => {
        this.loadPage();
      },
      'lg',
    );
  }

  private onSuccess(data: ITva[] | null): void {
    this.tvas.set(data || []);
    this.loading.set(false);
  }

  private onError(): void {
    this.loading.set(false);
  }
}
