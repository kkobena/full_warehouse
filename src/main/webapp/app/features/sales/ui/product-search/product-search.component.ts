import {
  Component,
  DestroyRef,
  effect,
  inject,
  Injector,
  input,
  isDevMode,
  OnDestroy,
  OnInit,
  output,
  signal,
  viewChild,
  ElementRef,
  HostListener,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { AutoComplete } from 'primeng/autocomplete';
import { FloatLabel } from 'primeng/floatlabel';
import { TranslatePipe } from '@ngx-translate/core';
import { DecimalPipe } from '@angular/common';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../../../shared/constants/pagination.constants';
import { ProduitSearch } from '../../../../shared/model/produit.model';
import { ProduitService } from '../../../../entities/produit/produit.service';
import { catchError, debounceTime, filter, of, Subject, Subscription } from 'rxjs';
import { ScanDetectorService, ScanEvent } from '../../../../shared/scan-detector.service';

/**
 * Composant de recherche produit avec scanner intégré
 * 
 * Version adaptée de ProduitSearchAutocompleteScannerComponent pour le nouveau système
 * Fonctionnalités:
 * - Recherche produit par libellé/code
 * - Scanner de code-barres intégré
 * - Détection automatique scan vs recherche manuelle
 * - Affichage stock et prix
 */
@Component({
  selector: 'app-product-search',
  templateUrl: './product-search.component.html',
  styleUrls: ['./product-search.component.scss'],
  imports: [AutoComplete, FormsModule, FloatLabel, TranslatePipe, DecimalPipe],
})
export class ProductSearchComponent implements OnInit, OnDestroy {
  // ViewChild
  produitbox = viewChild.required<AutoComplete>('produitbox');

  // Inputs
  autofocus = input<boolean>(true);
  showClear = input<boolean>(true);
  pageSize = input<number>(10);
  storageId = input<number | null>(null);
  style = input<{}>({ width: '100%' });
  inputStyle = input<{}>({ width: '100%' });
  enableScanner = input<boolean>(true);
  disabled = input<boolean>(false);

  // Outputs
  productSelected = output<ProduitSearch | null>();
  productScanned = output<ProduitSearch>();
  onKeyEnter = output<boolean>();
  onBarcodeScanned = output<string>();

  // State
  produits = signal<ProduitSearch[]>([]);
  selectProduit = signal<ProduitSearch | null>(null);
  private _produitSelected = signal<ProduitSearch | null>(null);

  // Constants
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;

  // Services
  private readonly produitService = inject(ProduitService);
  private readonly scanDetectorService = inject(ScanDetectorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly searchTrigger$ = new Subject<string>();

  // Scanner state
  private isScanning = false;
  private isManualSearching = false;
  private manualSearchTimeout: ReturnType<typeof setTimeout> | null = null;
  private keydownListener?: (event: KeyboardEvent) => void;
  private animationFrameId: number | null = null;
  private scanSubscription?: Subscription;

  constructor() {
    // Debounce search
    this.searchTrigger$
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(search => this.loadProduits(search));
  }

  get produitSelected(): ProduitSearch | null {
    return this._produitSelected();
  }

  set produitSelected(value: any) {
    if (typeof value === 'string') {
      return;
    }
    if (this.isScanning) {
      if (isDevMode()) {
        console.debug('[Scanner] Setter bloqué pendant le scan');
      }
      return;
    }
    if (this._produitSelected() !== value) {
      this._produitSelected.set(value as ProduitSearch | null);
    }
  }

  ngOnInit(): void {
    if (this.enableScanner()) {
      this.setupBarcodeScanner();
    }

    // Watch for changes in enableScanner
    effect(
      () => {
        const enabled = this.enableScanner();
        if (enabled && !this.keydownListener) {
          this.setupBarcodeScanner();
        } else if (!enabled && this.keydownListener) {
          this.removeBarcodeScanner();
          this.isScanning = false;
          this.stopInputClearLoop();
        }
      },
      { injector: this.injector }
    );
  }

  ngOnDestroy(): void {
    this.removeBarcodeScanner();
    this.stopInputClearLoop();
    this.scanSubscription?.unsubscribe();
    if (this.manualSearchTimeout) {
      clearTimeout(this.manualSearchTimeout);
      this.manualSearchTimeout = null;
    }
  }

  // ===== Event Handlers =====

  searchFn(event: any): void {
    // Abort scan if user is manually searching
    if (this.isScanning) {
      this.isScanning = false;
      this.stopInputClearLoop();
    }

    this.isManualSearching = true;

    if (this.manualSearchTimeout) {
      clearTimeout(this.manualSearchTimeout);
    }
    this.manualSearchTimeout = setTimeout(() => {
      this.isManualSearching = false;
      this.manualSearchTimeout = null;
    }, 600);

    this.searchTrigger$.next(event.query);
  }

  onSelect(): void {
    const selected = this.produitSelected;
    this.selectProduit.set(selected);
    this.productSelected.emit(selected);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      // Vérifier si le champ est vide en utilisant selectProduit (signal lié au modèle)
      const selected = this.selectProduit();
      const isEmpty = selected === null || selected === undefined;
      
      if (isEmpty) {
        this.onKeyEnter.emit(true);
        event.preventDefault();
        event.stopPropagation();
      }
    }
  }

  /**
   * HostListener natif pour intercepter toutes les touches Enter
   * Complément au onKeyDown de PrimeNG
   */
  @HostListener('keydown', ['$event'])
  onHostKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      const selected = this.selectProduit();
      const isEmpty = selected === null || selected === undefined;
      
      if (isEmpty) {
        this.onKeyEnter.emit(true);
        event.preventDefault();
        event.stopPropagation();
      }
    }
  }

  onNgModelChange(value: ProduitSearch | null): void {
    if (this.isScanning) {
      return;
    }
    this.produitSelected = value;
  }

  // ===== Public Methods =====

  getFocus(): void {
    requestAnimationFrame(() => {
      const el = this.produitbox()?.inputEL?.nativeElement;
      const autocomplete = this.produitbox();

      if (el) {
        el.focus();
        el.select();
      }

      if (autocomplete) {
        autocomplete.hide();
        setTimeout(() => autocomplete.hide(), 50);
      }
    });
  }

  reset(): void {
    this.isScanning = false;
    this.isManualSearching = false;
    if (this.manualSearchTimeout) {
      clearTimeout(this.manualSearchTimeout);
      this.manualSearchTimeout = null;
    }
    this.stopInputClearLoop();
    this.produits.set([]);
    this._produitSelected.set(null);
    this.selectProduit.set(null);

    const autocomplete = this.produitbox();
    if (autocomplete) {
      autocomplete.hide();
      setTimeout(() => autocomplete.hide(), 100);
    }

    const inputEl = autocomplete?.inputEL?.nativeElement;
    if (inputEl) {
      inputEl.value = '';
    }
  }

  // ===== Scanner Logic =====

  private setupBarcodeScanner(): void {
    this.scanSubscription?.unsubscribe();

    this.scanSubscription = this.scanDetectorService.onScanEvent$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter((event: ScanEvent) => event.type === 'start' || event.type === 'complete')
      )
      .subscribe((event: ScanEvent) => {
        if (event.type === 'start') {
          this.onScanStart();
        } else if (event.type === 'complete' && event.code) {
          this.onScanComplete(event.code);
        }
      });

    this.keydownListener = (event: KeyboardEvent) => {
      this.scanDetectorService.keyPressed(event.key);
    };

    document.addEventListener('keydown', this.keydownListener, true);
  }

  private onScanStart(): void {
    const autocomplete = this.produitbox();
    if (this.isManualSearching || autocomplete?.overlayVisible) {
      return;
    }
    this.isScanning = true;
    this.clearInputValue();
    this.startInputClearLoop();
  }

  private onScanComplete(scannedCode: string): void {
    const autocomplete = this.produitbox();
    if (this.isManualSearching || autocomplete?.overlayVisible) {
      this.isScanning = false;
      this.stopInputClearLoop();
      return;
    }

    this.stopInputClearLoop();
    this.clearInputValue();
    this.clearActiveElement();

    this.onBarcodeScanned.emit(scannedCode);
    this.searchByBarcode(scannedCode);

    setTimeout(() => {
      this.isScanning = false;
    }, 400);
  }

  private clearInputValue(): void {
    const inputEl = this.produitbox()?.inputEL?.nativeElement;
    if (inputEl) {
      inputEl.value = '';
    }
  }

  private clearActiveElement(): void {
    const activeElement = document.activeElement as HTMLInputElement;
    if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {
      activeElement.value = '';
    }
  }

  private startInputClearLoop(): void {
    const clearLoop = () => {
      if (!this.isScanning) {
        return;
      }
      this.clearInputValue();
      this.animationFrameId = requestAnimationFrame(clearLoop);
    };
    this.animationFrameId = requestAnimationFrame(clearLoop);
  }

  private stopInputClearLoop(): void {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
  }

  private removeBarcodeScanner(): void {
    this.scanSubscription?.unsubscribe();
    this.scanSubscription = undefined;

    if (this.keydownListener) {
      document.removeEventListener('keydown', this.keydownListener, true);
      this.keydownListener = undefined;
    }

    this.scanDetectorService.forceReset();
  }

  // ===== Backend API =====

  private searchByBarcode(barcode: string): void {
    this.produitService
      .search(
        {
          page: 0,
          size: 1,
          search: barcode,
          storageId: this.storageId(),
        },
        this.storageId() !== null
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(err => {
          this.isScanning = false;
          this.produits.set([]);
          return of({ body: [] });
        })
      )
      .subscribe(res => {
        const result = res.body || [];
        if (result.length === 1) {
          const selected = result[0];
          this.produits.set([]);
          this.selectProduit.set(selected);
          this.productScanned.emit(selected);
        } else if (result.length > 1) {
          this.produits.set(result);
          this.selectProduit.set(null);
          this.isScanning = false;
        } else {
          this.selectProduit.set(null);
          this.produits.set([]);
          this.isScanning = false;
        }
      });
  }

  private loadProduits(search: string): void {
    this.produitService
      .search(
        {
          page: 0,
          size: this.pageSize(),
          search,
          storageId: this.storageId(),
        },
        this.storageId() !== null
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(err => {
          return of({ body: [] });
        })
      )
      .subscribe(res => {
        const result = res.body || [];
        this.produits.set(result);
        if (result.length === 1) {
          const selected = result[0];
          this.produitSelected = selected;
          this.selectProduit.set(selected);
          this.productSelected.emit(selected);
        } else {
          this.selectProduit.set(null);
        }
      });
  }
}

