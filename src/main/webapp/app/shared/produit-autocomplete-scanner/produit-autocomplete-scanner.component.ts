import { Component, effect, forwardRef, inject, input, OnDestroy, OnInit, output, signal, viewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AutoComplete } from 'primeng/autocomplete';
import { FloatLabel } from 'primeng/floatlabel';
import { TranslatePipe } from '@ngx-translate/core';
import { CommonModule, DecimalPipe, NgClass } from '@angular/common';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../constants/pagination.constants';
import { IProduit } from '../model/produit.model';
import { ProduitService } from '../../entities/produit/produit.service';
import { debounceTime, Subject, Subscription } from 'rxjs';
import { ScanDetectorService } from '../scan-detector.service';

@Component({
  selector: 'jhi-produit-autocomplete-scanner',
  imports: [CommonModule, AutoComplete, FormsModule, FloatLabel, TranslatePipe, NgClass, DecimalPipe],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ProduitAutocompleteScannerComponent),
      multi: true,
    },
  ],
  templateUrl: './produit-autocomplete-scanner.component.html',
})
export class ProduitAutocompleteScannerComponent implements ControlValueAccessor, OnDestroy, OnInit {
  produits = signal<IProduit[]>([]);
  produitbox = viewChild.required<AutoComplete>('produitbox');
  selectProduit = signal<IProduit | null>(null);
  includeDetails = input<boolean>(false);
  autofocus = input<boolean>(true);
  showClear = input<boolean>(true);
  pageSize = input<number>(10);
  style = input<{}>({ width: '100%' });
  inputStyle = input<{}>({ width: '100%' });
  enableScanner = input<boolean>(true);
  selectedProduit = output<IProduit | null>();
  onKeyEnter = output<boolean>();
  onBarcodeScanned = output<string>();
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;
  private readonly produitService = inject(ProduitService);
  private readonly scanDetectorService = inject(ScanDetectorService);
  private readonly searchTrigger$ = new Subject<string>();
  private readonly searchSubscription: Subscription;
  private keydownListener?: (event: KeyboardEvent) => void;

  constructor() {
    effect(() => {
      const selected = this._produitSelected();
      this.onChange(selected);
      this.produitbox().hide();
    });
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

  ngOnInit(): void {
    if (this.enableScanner()) {
      this.setupBarcodeScanner();
    }
  }

  ngOnDestroy(): void {
    this.searchSubscription.unsubscribe();
    this.removeBarcodeScanner();
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

  setDisabledState?(isDisabled: boolean): void {
    const inputEl = this.produitbox().inputEL?.nativeElement;
    if (inputEl) {
      inputEl.disabled = isDisabled;
    }
  }

  // Déclenchée par l'autocomplete (entrée utilisateur)
  searchFn(event: any): void {
    this.searchTrigger$.next(event.query);
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

  onNgModelChange(value: IProduit | null): void {
    this.produitSelected = value;
  }

  getFocus(): void {
    setTimeout(() => {
      const el = this.produitbox().inputEL?.nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  private onChange: (_: any) => void = () => {};

  private onTouched: () => void = () => {};

  private setupBarcodeScanner(): void {
    // OFFLINE CAPABILITY: The barcode scanner detection works entirely offline
    // It uses keyboard event timing to detect rapid input from barcode scanners
    // Only the product search API call requires a network connection
    // GLOBAL SCAN: Works regardless of which field has focus
    this.keydownListener = (event: KeyboardEvent) => {
      const scannedCode = this.scanDetectorService.keyPressed(event.key);

      if (scannedCode) {
        // Prevent default behavior to avoid typing into focused fields
        event.preventDefault();
        event.stopPropagation();

        // Get reference to the product input
        const inputEl = this.produitbox().inputEL?.nativeElement;

        // Clear any text that might have been typed into the product input
        if (inputEl) {
          inputEl.value = '';
        }

        // Clear any other focused input that might have received scanner characters
        const activeElement = document.activeElement as HTMLInputElement;
        if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {
          // Store the current value to potentially restore it
          const currentValue = activeElement.value;

          // Check if the current value ends with partial barcode data
          // If it does, remove it (barcode chars would be the last chars typed rapidly)
          if (currentValue.length > 0) {
            activeElement.value = '';
          }
        }

        // Emit the scanned barcode
        this.onBarcodeScanned.emit(scannedCode);

        // Search for product by barcode (requires network connection)
        this.searchByBarcode(scannedCode);

        // Focus the product search field after scan for better UX
        if (inputEl) {
          setTimeout(() => {
            inputEl.focus();
          }, 100);
        }
      }
    };

    document.addEventListener('keydown', this.keydownListener, true);
  }

  private removeBarcodeScanner(): void {
    if (this.keydownListener) {
      document.removeEventListener('keydown', this.keydownListener, true);
      this.keydownListener = undefined;
    }
  }

  private searchByBarcode(barcode: string): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 1,
        withdetail: this.includeDetails(),
        search: barcode,
      })
      .subscribe(res => {
        const result = res.body || [];
        if (result.length === 1) {
          const selected = result[0];
          this.produitSelected = selected;
          this.selectProduit.set(selected);
          this.selectedProduit.emit(selected);
          this.produits.set([selected]);
        } else {
          // No product found with this barcode
          this.produitSelected = null;
          this.selectProduit.set(null);
          this.produits.set([]);
        }
      });
  }

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
        if (result.length === 1) {
          const selected = result[0];
          this.produitSelected = selected;
          this.selectProduit.set(selected);
          this.selectedProduit.emit(selected);
        } else {
          this.produitSelected = null;
          this.selectProduit.set(null);
        }
      });
  }
}
