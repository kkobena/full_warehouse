import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { Ajustement, IAjustement } from 'app/shared/model/ajustement.model';
import { IProduit } from '../../shared/model/produit.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AjustementService } from './ajustement.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { IMotifAjustement } from '../../shared/model/motif-ajustement.model';
import { ModifAjustementService } from '../modif-ajustement/motif-ajustement.service';
import { Ajust, IAjust } from '../../shared/model/ajust.model';
import {
  APPEND_TO,
  PRODUIT_COMBO_MIN_LENGTH,
  PRODUIT_COMBO_RESULT_SIZE,
  PRODUIT_NOT_FOUND
} from '../../shared/constants/pagination.constants';
import { ErrorService } from '../../shared/error.service';
import { FinalyseComponent } from './finalyse/finalyse.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { BadgeModule } from 'primeng/badge';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { Select } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { FloatLabel } from 'primeng/floatlabel';
import { ProduitAutocompleteComponent } from '../../shared/produit-autocomplete/produit-autocomplete.component';
import { QuantiteProdutSaisieComponent } from '../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { TagModule } from 'primeng/tag';
import { ButtonGroup } from 'primeng/buttongroup';
import { BackButtonComponent } from '../../shared/cta/back-button.component';
import { RemoveButtonTextComponent } from '../../shared/cta/remove-button.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';

@Component({
  selector: 'jhi-ajustement-detail',
  templateUrl: './ajustement-detail.component.html',
  imports: [
    WarehouseCommonModule,
    CardModule,
    FormsModule,
    ToolbarModule,
    ButtonModule,
    InputTextModule,
    BadgeModule,
    TableModule,
    TooltipModule,
    Select,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
    ToastAlertComponent,
    FloatLabel,
    ProduitAutocompleteComponent,
    QuantiteProdutSaisieComponent,
    TagModule,
    ButtonGroup,
    BackButtonComponent,
    RemoveButtonTextComponent
  ]
})
export class AjustementDetailComponent implements OnInit, AfterViewInit {
  protected ajustement: IAjust | null = null;
  protected produitSelected?: IProduit | null = null;
  protected motifSelected!: IMotifAjustement | null;
  protected isSaving = false;
  protected motifs: IMotifAjustement[] = [];
  protected items: IAjustement[] = [];
  protected search: string;
  protected readonly includeDetails = true;
  protected context: any;
  protected motif = viewChild.required<Select>('motif');
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected selectedEl: IAjustement[] = [];
  protected readonly appendTo = APPEND_TO;
  private readonly produitComponent = viewChild.required<ProduitAutocompleteComponent>('produitComponent');
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly produitQteCmpt = viewChild.required<QuantiteProdutSaisieComponent>('produitQteCmpt');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly ajustementService = inject(AjustementService);
  private readonly modifAjustementService = inject(ModifAjustementService);


  protected get canAddQuantity(): boolean {
    return this.motifValue && !!this.produitSelected;
  }

  protected get motifValue(): string | null {
    return this.motif()?.value || null;
  }

  protected get disabledButton(): boolean {
    return isNaN(this.produitQteCmpt().value);
  }

  ngOnInit(): void {
    this.search = '';
    this.selectedEl = [];
    this.loadMotifs('');
  }

  deleteSelectedItems(): void {
    const ids = this.selectedEl.map(e => e.id);
    this.ajustementService.deleteItemsByIds(ids).subscribe(() => {
      this.selectedEl = [];
      this.onSuccess();
    });
  }

  confirmDeleteItem(item: IAjustement): void {
    this.confimDialog().onConfirm(
      () => this.removeLine(item),
      'SUPPRESSION',
      'Voullez-vous supprimer cette ligne ?',
      null,
      () => this.focusPrdoduitBox()
    );
  }

  confirmDeleteItems(): void {
    this.confimDialog().onConfirm(
      () => this.deleteSelectedItems(),
      'SUPPRESSION',
      'Voullez-vous supprimer toutes les lignes ?',
      null,
      () => this.focusPrdoduitBox()
    );
  }

  onUpdateQuantity(ajustement: IAjustement, event: any): void {
    ajustement.qtyMvt = Number(event.target.value);
    this.subscribeAddItemResponse(this.ajustementService.updateItem(ajustement));
  }

  ngAfterViewInit(): void {
    this.fosusMotifControl();
  }

  protected onAddItem(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.ajustement?.id) {
        this.subscribeAddItemResponse(this.ajustementService.addItem(this.createItem(this.produitSelected, qytMvt)));
      } else {
        this.subscribeCreateNewResponse(this.ajustementService.create(this.createAjustement(this.produitSelected, qytMvt)));
      }
    }
  }

  protected loadAll(ajsut: number | null): void {
    this.ajustementService.query({ ajustementId: ajsut }).subscribe((res: HttpResponse<IAjustement[]>) => (this.items = res.body || []));
  }

  protected loadMotifs(query?: string): void {
    this.modifAjustementService
      .query({
        page: 0,
        size: 9999,
        search: query
      })
      .subscribe((res: HttpResponse<IMotifAjustement[]>) => this.onMotifSuccess(res.body));
  }

  protected onSave(): void {
    showCommonModal(
      this.modalService,
      FinalyseComponent,
      { header: 'Finalisation de l\'ajustement', entity: this.ajustement },
      (reason) => this.onSaveFinalyseSuccess(reason), 'lg', null
    );
  }

  protected onSelectProduct(selectedProduit?: IProduit): void {
    this.produitSelected = selectedProduit || null;
    this.setQuantityBoxFocused();
  }

  protected onSaveKeyDown(save: boolean): void {
    if (save && this.ajustement && this.ajustement.id) {
      this.onSave();
    }
  }

  protected onSelectMotif(): void {
    this.focusPrdoduitBox();
  }

  protected addQuantity(qte: number): void {
    this.onAddItem(qte);
  }

  protected removeLine(ajustement: IAjustement): void {
    this.ajustementService.deleteItem(ajustement.id).subscribe(() => {
      this.loadAll(ajustement.id);
    });
  }

  protected onFilterItems(): void {
    const query = {
      ajustementId: this.ajustement?.id,
      search: this.search
    };
    this.ajustementService.query(query).subscribe((res: HttpResponse<IAjustement[]>) => (this.items = res.body || []));
  }

  protected onSaveFinalyseSuccess(success: any): void {
    this.isSaving = false;
    this.ajustement = null;
    this.selectedEl = [];
    this.items = [];
    this.produitSelected = null;
    this.motifSelected = null;
    this.alert().showInfo(success);
  }

  protected onMotifSuccess(data: IMotifAjustement[] | null): void {
    this.motifs = data || [];
  }

  protected subscribeCreateNewResponse(result: Observable<HttpResponse<IAjust>>): void {
    result.subscribe({
      next: (res: HttpResponse<IAjust>) => this.onSaveSuccess(res.body),
      error: (err: HttpErrorResponse) => this.onSaveError(err)
    });
  }

  protected subscribeAddItemResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: (err: any) => this.onSaveError(err)
    });
  }

  protected onSaveError(err: HttpErrorResponse): void {
    this.isSaving = false;
    this.onCommonError(err);
  }

  protected onSaveSuccess(ajsut?: IAjustement): void {
    this.isSaving = false;
    this.onSuccess(ajsut);
  }

  protected onSuccess(ajsut?: IAjust): void {
    if (ajsut) {
      this.ajustement = ajsut;
    }
    this.loadAll(this.ajustement.id);
    this.produitSelected = null;
    this.produitQteCmpt().reset();
    this.focusPrdoduitBox();
  }


  private onCommonError(error: HttpErrorResponse): void {
    this.alert().showInfo(this.errorService.getErrorMessage(error));
  }

  private focusPrdoduitBox(): void {
    this.produitComponent().getFocus();
  }

  private createAjustement(produit: IProduit, quantity: number): IAjust {
    return {
      ...new Ajust(),
      ajustements: [this.createItem(produit, quantity)]
    };
  }

  private createItem(produit: IProduit, quantity: number): IAjustement {
    return {
      ...new Ajustement(),
      produitId: produit.id,
      qtyMvt: quantity,
      ajustId: this.ajustement?.id,
      motifAjustementId: this.motifSelected?.id
    };
  }

  private setQuantityBoxFocused(): void {
    this.produitQteCmpt().focusProduitControl();
    this.produitQteCmpt().reset(1);
  }

  private fosusMotifControl(): void {
    setTimeout(() => {
      this.motif().focus();
    }, 100);
  }
}
