import { Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import {
  APPEND_TO,
  ITEMS_PER_PAGE,
  PRODUIT_COMBO_MIN_LENGTH,
  PRODUIT_COMBO_RESULT_SIZE,
  PRODUIT_NOT_FOUND,
} from '../../../shared/constants/pagination.constants';
import { FloatLabel } from 'primeng/floatlabel';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { IProduit } from '../../../shared/model/produit.model';
import { InputText } from 'primeng/inputtext';
import { NgxSpinnerModule } from 'ngx-spinner';
import { KeyFilter } from 'primeng/keyfilter';
import { ProductToDestroyService } from '../product-to-destroy.service';
import { ProductToDestroy, ProductToDestroyFilter, ProductToDestroyPayload } from '../model/product-to-destroy';
import { ProduitAutocompleteComponent } from '../../../shared/produit-autocomplete/produit-autocomplete.component';
import { ToolbarModule } from 'primeng/toolbar';
import { QuantiteProdutSaisieComponent } from '../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { CtaComponent } from '../../../shared/cta/cta.component';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { DecimalPipe } from '@angular/common';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { PeremptionStatut } from '../model/peremption-statut';
import { RemoveButtonTextComponent } from '../../../shared/cta/remove-button-text.component';
import { BackButtonComponent } from '../../../shared/cta/back-button.component';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { DatePickerComponent } from '../../../shared/date-picker/date-picker.component';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-ajout-perimes',
  imports: [
    FloatLabel,
    TranslatePipe,
    FormsModule,
    InputText,
    NgxSpinnerModule,
    KeyFilter,
    ProduitAutocompleteComponent,
    ToolbarModule,
    QuantiteProdutSaisieComponent,
    CtaComponent,
    ConfirmDialogComponent,
    ToastAlertComponent,
    DecimalPipe,
    TableModule,
    Tag,
    RemoveButtonTextComponent,
    BackButtonComponent,
    IconField,
    InputIcon,
    DatePickerComponent,
    SpinnerComponent,
  ],
  templateUrl: './ajout-perimes.component.html',
  styleUrls: ['./ajout-perimes.component.scss'],
})
export class AjoutPerimesComponent implements OnInit {
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly ITEMS_PER_PAGE = ITEMS_PER_PAGE;
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected loading!: boolean;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected includeDetail = true;
  protected produits: IProduit[] = [];
  protected data: ProductToDestroy[] = [];
  protected produitSelected?: IProduit | null = null;
  protected searchTerm?: string;
  protected datePeremention = viewChild.required<DatePickerComponent>('datePeremention');
  protected numLotCmpt = viewChild.required<ElementRef>('numLot');
  protected isSaving = false;
  private produitQteCmpt = viewChild.required<QuantiteProdutSaisieComponent>('produitQteCmpt');
  private produitComponent = viewChild.required<ProduitAutocompleteComponent>('produitComponent');
  private confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  private readonly productToDestroyService = inject(ProductToDestroyService);

  protected get numLotIsEmpty(): boolean {
    return !this.numLotCmpt()?.nativeElement.value;
  }

  protected get numLotValue(): string | null {
    return this.numLotCmpt()?.nativeElement.value || null;
  }

  protected get canAddQuantity(): boolean {
    return this.numLotValue && !!this.produitSelected && !!this.datePeremention().value;
  }

  protected get disableButton(): boolean {
    return this.produitQteCmpt().value <= 0;
  }

  ngOnInit(): void {
    this.onSearch();
  }

  onDatePerementionChange(date: Date): void {
    setTimeout(() => {
      this.produitQteCmpt().focusProduitControl();
    }, 100);
  }

  onRemoveItem(item: ProductToDestroy): void {
    this.confimDialog().onConfirm(
      () => {
        this.removeItem(item);
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir supprimer cet article ?',
      null,
      () => {
        this.produitComponent().getFocus();
      },
    );
  }

  protected onClose(): void {
    this.confimDialog().onConfirm(
      () => {
        this.spinner().show();
        this.productToDestroyService.closeCurrent().subscribe({
          next: () => {
            this.spinner().hide();
            this.data = [];
            this.alert()?.showInfo('La clôture a été effectuée avec succès.');
          },
          error: err => {
            this.spinner().hide();
            this.onError(err);
          },
        });
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir clôtuer ? Les quantités saisies seront définitivement retirées du stock.',
    );
  }

  protected onSelect(selectedProduit?: IProduit): void {
    this.produitSelected = selectedProduit || null;
    setTimeout(() => {
      const el = this.numLotCmpt().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  protected onNumLot(event: any): void {
    const numLot = event.target.value;
    if (!numLot || !this.produitSelected) {
      return;
    }
    this.datePeremention().getFocus();
  }

  protected onSearch(): void {
    this.loadPage();
  }

  protected addQuantity(qte: number): void {
    if (qte > 0) {
      if (this.produitSelected?.totalQuantity <= 0) {
        this.alert().showError("Le produit n'a plus de stock");
      } else {
        if (qte > this.produitSelected?.totalQuantity) {
          this.produitQteCmpt().focusProduitControl();
          this.alert().showError(`La quantité saisie est supérieure à la quantité totale disponible pour ce produit.`);
        } else {
          this.isSaving = true;
          this.onAddItem(qte);
        }
      }
    }
  }

  protected modifyProductQuantity(item: ProductToDestroy, inputValue: any): void {
    this.isSaving = true;
    this.productToDestroyService
      .modifyProductQuantity({
        id: item.id,
        ...this.buildItem(inputValue as number),
      })
      .subscribe({
        next: () => {
          this.onAddSuccess();
        },
        error: err => {
          this.isSaving = false;
          this.loadPage();
          this.onError(err);
        },
      });
  }

  protected getSeverity(status: PeremptionStatut) {
    if (status.days < 0) {
      return 'danger';
    } else if (status.days === 0) {
      return 'warn';
    }
    return 'info';
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.productToDestroyService
        .queryForEdit({
          page: this.page,
          size: event.rows,
          ...this.buidParams(),
        })
        .subscribe({
          next: (res: HttpResponse<ProductToDestroy[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  private removeItem(item: ProductToDestroy): void {
    this.productToDestroyService
      .delete({
        ids: [item.id],
      })
      .subscribe({
        next: () => {
          this.loadPage();
          this.produitComponent().getFocus();
        },
        error: err => {
          this.onError(err);
        },
      });
  }

  private onSuccess(data: ProductToDestroy[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  private onFetchError(): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
  }

  private onAddItem(qte: number): void {
    this.productToDestroyService.addItem(this.buildItem(qte)).subscribe({
      next: () => {
        this.onAddSuccess();
      },
      error: err => {
        this.isSaving = false;
        this.onError(err);
      },
    });
  }

  private onAddSuccess(): void {
    this.isSaving = false;
    this.loadPage();
    this.produitSelected = null;
    this.numLotCmpt().nativeElement.value = '';
    this.produitComponent().getFocus();
    this.datePeremention().value = null;
    this.produitQteCmpt().reset();
  }

  private buildItem(quantity: number): ProductToDestroyPayload {
    return {
      editing: true,
      produitId: this.produitSelected?.id,
      datePeremption: this.datePeremention().submitValue,
      quantity,
      numLot: this.numLotCmpt()?.nativeElement.value,
      stockInitial: this.produitSelected?.totalQuantity,
    };
  }

  private onError(err?: any): void {
    const errorMessage = err?.error?.message || null;
    this.alert().showError(errorMessage);
  }

  private loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.productToDestroyService
      .queryForEdit({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        ...this.buidParams(),
      })
      .subscribe({
        next: (res: HttpResponse<ProductToDestroy[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onFetchError(),
      });
  }

  private buidParams(): ProductToDestroyFilter {
    return {
      editing: true,
      searchTerm: this.searchTerm,
    };
  }
}
