import { Component, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { Suggestion } from './model/suggestion.model';
import { SuggestionService } from './suggestion.service';
import { LazyLoadEvent, MenuItem, PrimeIcons, PrimeTemplate } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { SuggestionLine } from './model/suggestion-line.model';
import {
  APPEND_TO,
  ITEMS_PER_PAGE,
  PRODUIT_COMBO_MIN_LENGTH,
  PRODUIT_NOT_FOUND
} from '../../../shared/constants/pagination.constants';
import { Button } from 'primeng/button';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TableModule } from 'primeng/table';
import { Tooltip } from 'primeng/tooltip';
import { Keys } from '../../../shared/model/keys.model';
import { NgxSpinnerModule } from 'ngx-spinner';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { EtaProduitComponent } from '../../../shared/eta-produit/eta-produit.component';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { SplitButton } from 'primeng/splitbutton';
import { AutoComplete } from 'primeng/autocomplete';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { IProduit } from '../../../shared/model/produit.model';
import { IFournisseur } from '../../../shared/model/fournisseur.model';
import { saveAs } from 'file-saver';
import { ProduitService } from '../../produit/produit.service';
import { Observable } from 'rxjs';
import { FloatLabelModule } from 'primeng/floatlabel';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { SpinerService } from '../../../shared/spiner.service';

@Component({
  selector: 'jhi-edit-suggestion',
  imports: [
    Button,
    CommonModule,
    RouterModule,
    PrimeTemplate,
    TableModule,
    Tooltip,
    NgxSpinnerModule,
    ConfirmDialogModule,
    EtaProduitComponent,
    IconField,
    InputIcon,
    InputTextModule,
    ToolbarModule,
    SplitButton,
    AutoComplete,
    InputGroup,
    InputGroupAddon,
    ReactiveFormsModule,
    FormsModule,
    FloatLabelModule,
    ConfirmDialogComponent
  ],
  templateUrl: './edit-suggestion.component.html',

  styles: ``
})
export class EditSuggestionComponent implements OnInit {
  private readonly suggestionService = inject(SuggestionService);
  private readonly produitService = inject(ProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected search: string = '';
  protected searchProduit: string = '';
  protected page = 0;
  protected selections: SuggestionLine[] = [];
  protected index = 0;
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected loading!: boolean;
  protected suggestions: SuggestionLine[] = [];
  protected ngbPaginationPage = 1;
  protected splitbuttons: MenuItem[];
  produitSelected?: IProduit | null = null;
  fournisseurs: IFournisseur[] = [];
  produits: IProduit[] = [];
  quantiteSaisie = 1;
  quantityBox = viewChild.required<ElementRef>('quantityBox');
  //fournisseurBox = viewChild.required<any>('fournisseurBox');
  produitbox = viewChild.required<any>('produitbox');
  protected writableSignal = signal<Suggestion>(null);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly spinner = inject(SpinerService);

  constructor() {
    this.splitbuttons = [
      {
        label: 'Csv',
        icon: PrimeIcons.FILE_EXCEL,

        command: () => {
          this.exportCSV();
        }
      },
      {
        label: 'Pdf',
        icon: PrimeIcons.FILE_PDF,
        command: () => {
        }
      }
    ];
  }

  exportCSV(): void {
    this.suggestionService.exportToCsv(this.writableSignal().id).subscribe(blod => saveAs(blod));
  }

  searchFn(event: any): void {
    this.searchProduit = event.query;
    this.loadProduits();
  }

  onSelect(): void {
    setTimeout(() => {
      const el = this.quantityBox().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  onProviderSelect(): void {
    this.focusPrdoduitBox();
  }

  focusPrdoduitBox(): void {
    setTimeout(() => {
      this.produitbox()?.inputEL.nativeElement.focus();
      this.produitbox()?.inputEL.nativeElement.select();
    }, 50);
  }

  loadProduits(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 5,
        withdetail: false,
        search: this.searchProduit
      })
      .subscribe((res: HttpResponse<any[]>) => this.onProduitSuccess(res.body));
  }

  private buildParameters(): any {
    return {
      search: this.search,
      suggestionId: this.writableSignal()?.id
    };
  }

  private onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  commander(): void {
    this.spinner.show();
    this.suggestionService.commander(this.writableSignal()?.id).subscribe({
      next: () => {
        this.spinner.hide();
        this.gotoCommandeComponent();
      },
      error: err => {
        this.spinner.hide();
        this.onCommonError(err);
      }
    });
  }

  sanitize(): void {
    this.spinner.show();
    this.suggestionService.sanitize(this.writableSignal()?.id).subscribe({
      next: () => {
        this.onSearch();
        this.spinner.hide();
      },
      error: err => {
        this.spinner.hide();
        this.onCommonError(err);
      }
    });
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ suggestion }) => {
      this.writableSignal.set(suggestion);
      this.onSearch();
    });
  }

  deleteAll(): void {

    this.confimDialog().onConfirm(() =>
      this.onDelete({
        ids: this.selections.map(suggestion => suggestion.id)
      }), 'Suppression', 'Êtes-vous sûr de vouloir supprimer ?');
  }

  previousState(): void {
    window.history.back();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.suggestionService
      .queryItems({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildParameters()
      })
      .subscribe({
        next: (res: HttpResponse<SuggestionLine[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError()
      });
  }

  onSearch(search: string = ''): void {
    this.search = search;
    if (this.index == 0) {
      this.loadPage(0);
    }
  }

  onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    if (qytMvt <= 0) {
      return;
    }
    this.onAddOrderLine(qytMvt);
  }

  onQuantity(): void {
    const qytMvt = Number(this.quantityBox().nativeElement.value);
    if (qytMvt <= 0) {
      return;
    }
    this.onAddOrderLine(qytMvt);
  }

  private createItem(produit: IProduit, quantity: number): SuggestionLine {
    return {
      ...new SuggestionLine(),
      produitId: produit.id,
      quantity
    };
  }

  onAddOrderLine(qytMvt: number): void {
    if (this.produitSelected) {
      this.subscribeToSaveLineResponse(
        this.suggestionService.createOrUpdateItem(this.createItem(this.produitSelected, qytMvt), this.writableSignal().id)
      );
    }
  }

  private subscribeToSaveLineResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveLineSuccess(),
      error: (err: any) => this.onCommonError(err)
    });
  }

  private updateProduitQtyBox(): void {
    if (this.quantityBox()) {
      this.quantityBox().nativeElement.value = 1;
    }
    this.produitSelected = null;
    this.focusPrdoduitBox();
  }

  private onSaveLineSuccess(): void {
    this.suggestionService.find(this.writableSignal().id).subscribe(res => {
      this.writableSignal.set(res.body);
      this.updateProduitQtyBox();
      this.loadPage();
    });
  }

  gotoCommandeComponent(): void {
    this.router.navigate(['/commande']);
  }

  onUpdateQuantityRequested(item: SuggestionLine, event: any): void {
    const newQuantity = Number(event.target.value);
    if (newQuantity > 0) {
      item.quantity = newQuantity;
      this.subscribeToSaveLineResponse(this.suggestionService.updateQuantity(item));
    }
  }

  private onDelete(ids: Keys): void {
    this.spinner.show();
    this.suggestionService.deleteItem(ids).subscribe({
      next: () => {
        this.selections = [];
        this.spinner.hide();
        this.suggestionService.find(this.writableSignal().id).subscribe(res => {
          this.writableSignal.set(res.body);
          this.loadPage();
        });
      },
      error: error => {
        this.spinner.hide();
        this.onCommonError(error);
      }
    });
  }

  private onCommonError(error: any): void {
  }

  delete(id: number): void {
    this.confimDialog().onConfirm(() =>
      this.onDelete({
        ids: [id]
      }), 'Suppression', 'Êtes-vous sûr de vouloir supprimer ?');
  }

  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.suggestionService
        .queryItems({
          page: this.page,
          size: event.rows,
          ...this.buildParameters()
        })
        .subscribe({
          next: (res: HttpResponse<SuggestionLine[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError()
        });
    }
  }

  private onSuccess(data: SuggestionLine[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.suggestions = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
}
