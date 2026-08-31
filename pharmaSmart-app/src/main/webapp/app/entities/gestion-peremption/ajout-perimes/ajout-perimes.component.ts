import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnInit,
  signal,
  viewChild
} from "@angular/core";
import {
  ITEMS_PER_PAGE,
  PRODUIT_COMBO_RESULT_SIZE
} from "../../../shared/constants/pagination.constants";
import {TranslatePipe} from "@ngx-translate/core";
import {FormsModule} from "@angular/forms";
import {IProduit} from "../../../shared/model";
import {NgxSpinnerModule} from "ngx-spinner";
import {ProductToDestroyService} from "../product-to-destroy.service";
import {
  ProductToDestroy,
  ProductToDestroyFilter,
  ProductToDestroyPayload
} from "../model/product-to-destroy";
import {
  ProduitAutocompleteComponent
} from "../../../shared/produit-autocomplete/produit-autocomplete.component";
import {
  QuantiteProdutSaisieComponent
} from "../../../shared/quantite-produt-saisie/quantite-produt-saisie.component";
import {
  AppTableLazyLoadEvent,
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  EditableCellComponent,
  FloatLabelComponent,
  IconFieldComponent,
  KeyFilterDirective,
  ToolbarComponent
} from "../../../shared/ui";
import {HttpHeaders, HttpResponse} from "@angular/common/http";
import {PeremptionStatut} from "../model/peremption-statut";
import {PharmaDatePickerComponent} from "../../../shared/date-picker/pharma-date-picker.component";
import {NgbDateStruct, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {NGB_DATE_TO_ISO} from "../../../shared/util/warehouse-util";
import {SpinnerComponent} from "../../../shared/spinner/spinner.component";
import {CommonModule} from "@angular/common";
import {
  NgbConfirmDialogService
} from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {NotificationService} from "../../../shared/services/notification.service";
import {ErrorService} from "../../../shared/error.service";

@Component({
  selector: "app-ajout-perimes",
  imports: [
    CommonModule,
    FloatLabelComponent,
    TranslatePipe,
    FormsModule,
    NgxSpinnerModule,
    KeyFilterDirective,
    ProduitAutocompleteComponent,
    ToolbarComponent,
    QuantiteProdutSaisieComponent,
    ButtonComponent,
    NgbTooltip,
    DataTableComponent,
    BadgeComponent,
    IconFieldComponent,
    EditableCellComponent,
    PharmaDatePickerComponent,
    SpinnerComponent
  ],
  templateUrl: "./ajout-perimes.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ["./ajout-perimes.component.scss"]
})
export class AjoutPerimesComponent implements OnInit {
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected readonly page = signal<number | undefined>(undefined);
  protected readonly loading = signal<boolean | undefined>(undefined);
  protected readonly ngbPaginationPage = signal(1);
  protected readonly totalItems = signal(0);
  protected includeDetail = true;
  protected readonly data = signal<ProductToDestroy[]>([]);
  protected readonly produitSelected = signal<IProduit | null>(null);
  protected searchTerm?: string;
  protected datePeremention = viewChild.required<PharmaDatePickerComponent>("datePeremention");
  protected readonly dateValue = signal<NgbDateStruct | null>(null);
  protected numLotCmpt = viewChild.required<ElementRef>("numLot");
  protected readonly isSaving = signal(false);
  private produitQteCmpt = viewChild.required<QuantiteProdutSaisieComponent>("produitQteCmpt");
  private produitComponent = viewChild.required<ProduitAutocompleteComponent>("produitComponent");
  private readonly confimDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");

  private readonly productToDestroyService = inject(ProductToDestroyService);

  protected get numLotIsEmpty(): boolean {
    return !this.numLotCmpt()?.nativeElement.value;
  }

  protected get numLotValue(): string | null {
    return this.numLotCmpt()?.nativeElement.value || null;
  }

  protected get canAddQuantity(): boolean {
    return this.numLotValue && !!this.produitSelected() && !!this.dateValue();
  }

  protected get disableButton(): boolean {
    return this.produitQteCmpt().value <= 0;
  }

  ngOnInit(): void {
    this.onSearch();
  }

  onDatePerementionChange(date: NgbDateStruct | null): void {
    setTimeout(() => {
      this.produitQteCmpt().focusProduitControl();
    }, 100);
  }

  onRemoveItem(item: ProductToDestroy): void {
    this.confimDialog.onConfirm(
      () => {
        this.removeItem(item);
      },
      "Confirmation",
      "Êtes-vous sûr de vouloir supprimer cet article ?",
      null,
      () => {
        this.produitComponent().getFocus();
      }
    );
  }

  protected goBack(): void {
    window.history.back();
  }

  protected onClose(): void {
    this.confimDialog.onConfirm(
      () => {
        this.spinner().show();
        this.productToDestroyService.closeCurrent().subscribe({
          next: () => {
            this.spinner().hide();
            this.data.set([]);
            this.notificationService.info("La clôture a été effectuée avec succès.");
          },
          error: err => {
            this.spinner().hide();
            this.onError(err);
          }
        });
      },
      "Confirmation",
      "Êtes-vous sûr de vouloir clôtuer ? Les quantités saisies seront définitivement retirées du stock."
    );
  }

  protected onSelect(selectedProduit?: IProduit): void {
    this.produitSelected.set(selectedProduit || null);
    setTimeout(() => {
      const el = this.numLotCmpt().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  protected onNumLot(event: any): void {
    const numLot = event.target.value;
    if (!numLot || !this.produitSelected()) {
      return;
    }
    this.datePeremention().getFocus();
  }

  protected onSearch(): void {
    this.loadPage();
  }

  protected addQuantity(qte: number): void {
    if (qte > 0) {
      if (this.produitSelected()?.totalQuantity <= 0) {
        this.notificationService.error("Le produit n'a plus de stock");
      } else {
        if (qte > this.produitSelected()?.totalQuantity) {
          this.produitQteCmpt().focusProduitControl();
          this.notificationService.error(`La quantité saisie est supérieure à la quantité totale disponible pour ce produit.`);
        } else {
          this.isSaving.set(true);
          this.onAddItem(qte);
        }
      }
    }
  }

  protected modifyProductQuantity(item: ProductToDestroy, inputValue: any): void {
    this.isSaving.set(true);
    this.productToDestroyService
      .modifyProductQuantity({
        id: item.id,
        ...this.buildItem(inputValue as number)
      })
      .subscribe({
        next: () => {
          this.onAddSuccess();
        },
        error: err => {
          this.isSaving.set(false);
          this.loadPage();
          this.onError(err);
        }
      });
  }

  protected getSeverity(status: PeremptionStatut) {
    if (status.days < 0) {
      return "danger";
    } else if (status.days === 0) {
      return "warn";
    }
    return "info";
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page.set(event.first / event.rows);
      this.loading.set(true);
      this.productToDestroyService
        .queryForEdit({
          page: this.page(),
          size: event.rows,
          ...this.buidParams()
        })
        .subscribe({
          next: (res: HttpResponse<ProductToDestroy[]>) => this.onSuccess(res.body, res.headers, this.page()),
          error: () => this.onError()
        });
    }
  }

  private removeItem(item: ProductToDestroy): void {
    this.productToDestroyService
      .delete({
        ids: [item.id]
      })
      .subscribe({
        next: () => {
          this.loadPage();
          this.produitComponent().getFocus();
        },
        error: err => {
          this.onError(err);
        }
      });
  }

  private onSuccess(data: ProductToDestroy[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.page.set(page);
    this.data.set(data || []);
    this.ngbPaginationPage.set(this.page());
    this.loading.set(false);
  }

  private onFetchError(): void {
    this.ngbPaginationPage.set(this.page() ?? 1);
    this.loading.set(false);
  }

  private onAddItem(qte: number): void {
    this.productToDestroyService.addItem(this.buildItem(qte)).subscribe({
      next: () => {
        this.onAddSuccess();
      },
      error: err => {
        this.isSaving.set(false);
        this.onError(err);
      }
    });
  }

  private onAddSuccess(): void {
    this.isSaving.set(false);
    this.loadPage();
    this.produitSelected.set(null);
    this.numLotCmpt().nativeElement.value = "";
    this.produitComponent().getFocus();
    this.dateValue.set(null);
    this.produitQteCmpt().reset();
  }

  private buildItem(quantity: number): ProductToDestroyPayload {
    return {
      editing: true,
      produitId: this.produitSelected()?.id,
      datePeremption: NGB_DATE_TO_ISO(this.dateValue()),
      quantity,
      numLot: this.numLotCmpt()?.nativeElement.value,
      stockInitial: this.produitSelected()?.totalQuantity
    };
  }

  private onError(err?: any): void {
    this.notificationService.error(this.errorService.getErrorMessage(err));
  }

  private loadPage(page?: number): void {
    const pageToLoad: number = page || this.page() || 1;
    this.productToDestroyService
      .queryForEdit({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        ...this.buidParams()
      })
      .subscribe({
        next: (res: HttpResponse<ProductToDestroy[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onFetchError()
      });
  }

  private buidParams(): ProductToDestroyFilter {
    return {
      editing: true,
      searchTerm: this.searchTerm
    };
  }
}
