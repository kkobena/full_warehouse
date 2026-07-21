import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {TvaService} from './tva.service';
import {HttpResponse} from '@angular/common/http';
import {ITva} from '../../shared/model/tva.model';
import {ButtonComponent, DataTableComponent, SelectableRowDirective} from '../../shared/ui';
import {NgbModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {showCommonModal} from '../sales/selling-home/sale-helper';
import {FormTvaComponent} from './form-tva/form-tva.component';
import {CommonModule} from '@angular/common';
import {TranslatePipe} from '@ngx-translate/core';
import {
  NgbConfirmDialogService
} from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

@Component({
  selector: 'app-tva',
  templateUrl: './tva.component.html',
  styleUrl: './tva.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, TranslatePipe, ButtonComponent, DataTableComponent, SelectableRowDirective, NgbTooltip],
})
export class TvaComponent implements OnInit {
  protected tvas?: ITva[];
  // `null` et non `undefined` : le `model()` de `app-data-table` ne transporte que
  // `T | T[] | null`, et la liaison bidirectionnelle doit pouvoir lui réécrire la valeur.
  protected selectedTva: ITva | null = null;
  protected loading!: boolean;
  protected isSaving = false;
  private readonly tvaService = inject(TvaService);
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
        this.tvaService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
      'Confirmation',
      'Voulez-vous supprimer cet enregistrement ?',
    );
  }

  protected loadPage(): void {
    this.loading = true;
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
    this.tvas = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
  }
}
