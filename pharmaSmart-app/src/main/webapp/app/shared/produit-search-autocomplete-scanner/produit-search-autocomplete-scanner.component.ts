import {
  Component,
  DestroyRef,
  effect,
  forwardRef,
  inject,
  Injector,
  input,
  isDevMode,
  OnDestroy,
  OnInit,
  output,
  signal,
  viewChild,
  ChangeDetectionStrategy
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AutoComplete } from 'primeng/autocomplete';
import { FloatLabel } from 'primeng/floatlabel';
import { TranslatePipe } from '@ngx-translate/core';
import { DecimalPipe } from '@angular/common';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../constants/pagination.constants';
import { ProduitSearch } from '../model/produit.model';
import { ProduitService } from '../../entities/produit/produit.service';
import { catchError, debounceTime, filter, of, Subject, Subscription } from 'rxjs';
import { ScanDetectorService, ScanEvent } from '../scan-detector.service';

@Component({
  selector: 'jhi-produit-search-autocomplete-scanner',
  imports: [AutoComplete, FormsModule, FloatLabel, TranslatePipe, DecimalPipe],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ProduitSearchAutocompleteScannerComponent),
      multi: true,
    },
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './produit-search-autocomplete-scanner.component.html',
})
export class ProduitSearchAutocompleteScannerComponent implements ControlValueAccessor, OnDestroy, OnInit {
  produits = signal<ProduitSearch[]>([]);
  produitbox = viewChild.required<AutoComplete>('produitbox');
  selectProduit = signal<ProduitSearch | null>(null);
  includeDetails = input<boolean>(true);
  autofocus = input<boolean>(true);
  showClear = input<boolean>(true);
  pageSize = input<number>(5);
  storageId = input<number>(null);
  style = input<{}>({ width: '100%' });
  inputStyle = input<{}>({ width: '100%' });
  enableScanner = input<boolean>(true);
  disabled = input<boolean>(false);

  selectedProduit = output<ProduitSearch | null>();
  scannedProduit = output<ProduitSearch>();
  onKeyEnter = output<boolean>();
  onBarcodeScanned = output<string>();

  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;

  private readonly produitService = inject(ProduitService);
  private readonly scanDetectorService = inject(ScanDetectorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly searchTrigger$ = new Subject<string>();

  private isScanning = false;
  private isManualSearching = false;
  private manualSearchTimeout: ReturnType<typeof setTimeout> | null = null;
  private keydownListener?: (event: KeyboardEvent) => void;
  private animationFrameId: number | null = null;
  private scanSubscription?: Subscription;
  private readonly _produitSelected = signal<ProduitSearch | null>(null);

  constructor() {
    effect(() => {
      const selected = this._produitSelected();
      this.onChange(selected);
      const autocomplete = this.produitbox();
      if (autocomplete) {
        autocomplete.hide();
      }
    });

    this.searchTrigger$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe(search => this.loadProduits(search));
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
      console.log('[Scanner] Setter bloqué pendant le scan');
      return;
    }
    if (this._produitSelected() !== value) {
      this._produitSelected.set(value as ProduitSearch | null);
      this.onChange(value);
    }
  }

  ngOnInit(): void {
    // Setup initial state
    if (this.enableScanner()) {
      this.setupBarcodeScanner();
    }

    // Watch for changes in enableScanner (pour basculer entre scanner local et global)
    effect(
      () => {
        const enabled = this.enableScanner();
        if (enabled && !this.keydownListener) {
          // Scanner activé et pas encore configuré
          this.setupBarcodeScanner();
        } else if (!enabled && this.keydownListener) {
          // Scanner désactivé mais encore configuré
          this.removeBarcodeScanner();
          this.isScanning = false;
          this.stopInputClearLoop();
        }
      },
      { injector: this.injector },
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

  writeValue(value: any): void {
    if (this.isScanning) {
      return;
    }
    this.produitSelected = value;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    const inputEl = this.produitbox().inputEL()?.nativeElement;
    if (inputEl) {
      inputEl.disabled = isDisabled;
    }
  }

  searchFn(event: any): void {
    // If a scan was wrongly detected (e.g., from fast typing), abort it.
    // User interaction with the search box should always have priority.
    if (this.isScanning) {
      this.isScanning = false;
      this.stopInputClearLoop();
    }

    // Flag that a manual search is in progress to prevent the scanner from starting.
    this.isManualSearching = true;

    // Debounce the reset of the manual search flag.
    if (this.manualSearchTimeout) {
      clearTimeout(this.manualSearchTimeout);
    }
    this.manualSearchTimeout = setTimeout(() => {
      this.isManualSearching = false;
      this.manualSearchTimeout = null;
    }, 600); // Must be longer than the scan detector's timeout.

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

  onNgModelChange(value: ProduitSearch | null): void {
    if (this.isScanning) {
      return;
    }
    this.produitSelected = value;
  }

  getFocus(): void {
    // Utiliser requestAnimationFrame pour une meilleure performance
    requestAnimationFrame(() => {
      const el = this.produitbox()?.inputEL()?.nativeElement;
      const autocomplete = this.produitbox();

      if (el) {
        el.focus();
        el.select();
      }

      // Une seule fermeture du dropdown avec un délai raisonnable
      if (autocomplete) {
        autocomplete.hide();
        // Un seul timeout de secours si vraiment nécessaire
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
      // Un seul délai de fermeture au lieu de l'interval agressif
      setTimeout(() => autocomplete.hide(), 100);
    }

    const inputEl = autocomplete?.inputEL()?.nativeElement;
    if (inputEl) {
      inputEl.value = '';
    }

    this.onChange(null);
  }

  private onChange: (_: any) => void = () => {};
  private onTouched: () => void = () => {};

  private setupBarcodeScanner(): void {
    // Nettoyer l'ancienne subscription pour éviter les fuites mémoire
    this.scanSubscription?.unsubscribe();

    // Utiliser l'Observable RxJS au lieu des callbacks
    this.scanSubscription = this.scanDetectorService.onScanEvent$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter((event: ScanEvent) => event.type === 'start' || event.type === 'complete'),
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
    // Ignorer si l'utilisateur est en train de faire une recherche manuelle
    // On vérifie : le flag isManualSearching OU si le dropdown est visible
    const autocomplete = this.produitbox();
    if (this.isManualSearching || autocomplete?.overlayVisible) {
      return;
    }
    this.isScanning = true;
    this.clearInputValue();
    this.startInputClearLoop();
  }

  private onScanComplete(scannedCode: string): void {
    // Ignorer si l'utilisateur est en train de faire une recherche manuelle
    // On vérifie : le flag isManualSearching OU si le dropdown est visible
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

    // Délai avant de désactiver le mode scan pour éviter les mises à jour indésirables
    setTimeout(() => {
      this.isScanning = false;
    }, 400);
  }

  private clearInputValue(): void {
    const inputEl = this.produitbox()?.inputEL()?.nativeElement;
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

  /**
   * Utilise requestAnimationFrame au lieu de setInterval pour vider l'input pendant le scan.
   * Plus performant et synchronisé avec le rafraîchissement de l'écran.
   */
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
    // Nettoyer la subscription
    this.scanSubscription?.unsubscribe();
    this.scanSubscription = undefined;

    // Nettoyer le listener
    if (this.keydownListener) {
      document.removeEventListener('keydown', this.keydownListener, true);
      this.keydownListener = undefined;
    }

    // Reset le service pour éviter les états incohérents
    this.scanDetectorService.forceReset();
  }

  private searchByBarcode(barcode: string): void {
    this.produitService
      .search(
        {
          page: 0,
          size: 1,
          search: barcode,
          storageId: this.storageId(),
        },
        this.storageId() !== null,
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(err => {
          this.isScanning = false;
          this.produits.set([]);
          return of({ body: [] });
        }),
      )
      .subscribe(res => {
        const result = res.body || [];
        if (result.length === 1) {
          const selected = result[0];
          this.produits.set([]);
          this.selectProduit.set(selected);
          // Émettre sur scannedProduit (pas selectedProduit) pour distinguer du flux manuel
          this.scannedProduit.emit(selected);
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
        this.storageId() !== null,
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(err => {
          return of({ body: [] });
        }),
      )
      .subscribe(res => {
        const result = res.body || [];
        this.produits.set(result);
        if (result.length === 1) {
          const selected = result[0];
          this.produitSelected = selected;
          this.selectProduit.set(selected);
          this.selectedProduit.emit(selected);
        } else {
          this.selectProduit.set(null);
        }
      });
  }
}
