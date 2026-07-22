import { Component, ElementRef, forwardRef, inject, input, OnDestroy, output, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { DecimalPipe } from '@angular/common';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../constants/pagination.constants';
import { IProduit } from '../model';
import { ProduitService } from '../../entities/produit/produit.service';
import { debounceTime, Subject, Subscription } from 'rxjs';
import { FloatLabelComponent, SelectSearchComponent } from '../ui';

@Component({
  selector: 'jhi-produit-autocomplete',
  imports: [SelectSearchComponent, FormsModule, FloatLabelComponent, TranslatePipe, DecimalPipe],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ProduitAutocompleteComponent),
      multi: true,
    },
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './produit-autocomplete.component.html',
})
export class ProduitAutocompleteComponent implements ControlValueAccessor, OnDestroy {
  produits = signal<IProduit[]>([]);
  private readonly produitboxCmp = viewChild.required('produitbox', { read: SelectSearchComponent });
  private readonly produitboxEl = viewChild.required('produitbox', { read: ElementRef<HTMLElement> });
  selectProduit = signal<IProduit | null>(null);
  includeDetails = input<boolean>(false);
  autofocus = input<boolean>(true);
  showClear = input<boolean>(true);
  pageSize = input<number>(10);
  style = input<{}>({ width: '100%' });
  inputStyle = input<{}>({ width: '100%' });
  selectedProduit = output<IProduit | null>();
  onKeyEnter = output<boolean>();
  onClear = output<boolean>();
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;
  private readonly produitService = inject(ProduitService);
  private readonly searchTrigger$ = new Subject<string>();
  private readonly searchSubscription: Subscription;

  /**
   * Passé à `[typeahead]` de `app-select-search` dans le seul but d'être « observé » :
   * ng-select désactive alors son propre filtrage client (par `bindLabel`), qui sinon
   * masquerait les résultats retournés par le backend sur un code CIP (le texte tapé ne
   * matche pas `libelle`). La recherche réelle continue de passer par `searchTrigger$`
   * via `(searched)` → `searchFn()`, seul canal qui respecte `PRODUIT_COMBO_MIN_LENGTH`.
   */
  protected readonly typeaheadSink$ = new Subject<string>();

  constructor() {
    this.searchSubscription = this.searchTrigger$.pipe(debounceTime(300)).subscribe(search => this.loadProduits(search));
  }

  private _produitSelected = signal<IProduit | null>(null);

  // Getter / Setter pour ngModel
  get produitSelected(): IProduit | null {
    return this._produitSelected();
  }

  set produitSelected(value: IProduit | null) {
    if (this._produitSelected() !== value) {
      this._produitSelected.set(value);
      this.onChange(value);
    }
  }

  ngOnDestroy(): void {
    this.searchSubscription.unsubscribe();
  }

  writeValue(value: any): void {
    this.produitSelected = value;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  private readonly cvaDisabled = signal(false);

  /**
   * Angular appelle `setDisabledState` dès `ngOnChanges`, avant que la vue (et donc
   * `produitbox`, un `viewChild.required`) n'existe. Lire un viewChild required à ce
   * stade lève une erreur et casse silencieusement le reste de l'initialisation du
   * composant (champ figé, plus aucune saisie possible) — cf. le même bug corrigé sur
   * `ProduitSearchAutocompleteScannerComponent`. On se contente donc d'un signal.
   */
  setDisabledState?(isDisabled: boolean): void {
    this.cvaDisabled.set(isDisabled);
  }

  // Déclenchée par l'autocomplete (entrée utilisateur)
  searchFn(term: string): void {
    this.searchTrigger$.next(term);
  }

  onSelect(): void {
    const selected = this.produitSelected;
    this.selectProduit.set(selected);
    this.selectedProduit.emit(selected);
    this.onTouched();
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && this.produitSelected === null) {
      this.onKeyEnter.emit(true);
    }
  }

  clear(): void {
    this.onClear.emit(true);
  }

  onNgModelChange(value: IProduit | null): void {
    this.produitSelected = value;
  }

  getFocus(): void {
    setTimeout(() => {
      const el = this.produitboxEl()?.nativeElement.querySelector('input');
      el?.focus();
      el?.select();
    }, 50);
  }

  private onChange: (_: any) => void = () => {};

  private onTouched: () => void = () => {};

  private loadProduits(search: string): void {
    this.produitService
      .queryLite({
        page: 0,
        size: this.pageSize(),
        withdetail: this.includeDetails(),
        search,
      })
      .subscribe(res => {
        const result = res.body || [];
        this.produits.set(result);
        // Don't auto-select or clear while typing - let the user choose from suggestions
      });
  }
}
