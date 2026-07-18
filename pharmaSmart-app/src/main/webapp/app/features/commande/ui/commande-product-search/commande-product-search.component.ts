import {Component, DestroyRef, inject, input, OnDestroy, OnInit, output, signal, viewChild, ChangeDetectionStrategy} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {AutoComplete, AutoCompleteModule} from 'primeng/autocomplete';
import {FloatLabel} from 'primeng/floatlabel';
import {DecimalPipe} from '@angular/common';
import {catchError, debounceTime, filter, of, Subject, Subscription} from 'rxjs';
import {ProduitSearch} from '../../../../shared/model';
import {ProduitService} from '../../../../entities/produit/produit.service';
import {ScanDetectorService, ScanEvent} from '../../../../shared/scan-detector.service';
import {APPEND_TO, PRODUIT_COMBO_MIN_LENGTH} from '../../../../shared/constants/pagination.constants';

/**
 * Composant de recherche produit dédié au module commande.
 * - Recherche via `/api/produits/search` (endpoint ProduitSearch, plus léger que /lite)
 * - Debounce 300ms sur la saisie manuelle
 * - Intégration scanner code-barres (ScanDetectorService)
 * - API publique : getFocus(), reset()
 */
@Component({
  selector: 'app-commande-product-search',
  templateUrl: './commande-product-search.component.html',
  styleUrls: ['./commande-product-search.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [AutoCompleteModule, FormsModule, FloatLabel, DecimalPipe],
})
export class CommandeProductSearchComponent implements OnInit, OnDestroy {
  produitbox = viewChild.required<AutoComplete>('produitbox');

  autofocus = input<boolean>(true);
  pageSize = input<number>(10);
  disabled = input<boolean>(false);
  searchByStorage = input<boolean>(false);
  storageId = input<number>(null);

  productSelected = output<ProduitSearch | null>();
  productScanned = output<ProduitSearch>();
  onKeyEnter = output<void>();

  readonly produits = signal<ProduitSearch[]>([]);
  readonly selectProduit = signal<ProduitSearch | null>(null);

  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly APPEND_TO = APPEND_TO;

  private _produitSelected = signal<ProduitSearch | null>(null);
  private readonly produitService = inject(ProduitService);
  private readonly scanDetectorService = inject(ScanDetectorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchTrigger$ = new Subject<string>();

  private isScanning = false;
  private isManualSearching = false;
  private manualSearchTimeout: ReturnType<typeof setTimeout> | null = null;
  private keydownListener?: (event: KeyboardEvent) => void;
  private animationFrameId: number | null = null;
  private scanSubscription?: Subscription;

  constructor() {
    this.searchTrigger$
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(search => this.loadProduits(search));
  }

  get produitSelected(): ProduitSearch | null {
    return this._produitSelected();
  }

  set produitSelected(value: any) {
    if (typeof value === 'string') return;
    if (this.isScanning) return;
    if (this._produitSelected() !== value) {
      this._produitSelected.set(value as ProduitSearch | null);
    }
  }

  ngOnInit(): void {
    this.setupBarcodeScanner();
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

  searchFn(event: any): void {
    if (this.isScanning) {
      this.isScanning = false;
      this.stopInputClearLoop();
    }
    this.isManualSearching = true;
    if (this.manualSearchTimeout) clearTimeout(this.manualSearchTimeout);
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
      const isEmpty = this.selectProduit() === null || this.selectProduit() === undefined;
      if (isEmpty) {
        this.onKeyEnter.emit();
        event.preventDefault();
        event.stopPropagation();
      }
    }
  }

  onNgModelChange(value: ProduitSearch | null): void {
    if (this.isScanning) return;
    this.produitSelected = value;
  }

  getFocus(): void {
    requestAnimationFrame(() => {
      const el = this.produitbox()?.inputEL()?.nativeElement;
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
    const inputEl = autocomplete?.inputEL()?.nativeElement;
    if (inputEl) inputEl.value = '';
  }

  private setupBarcodeScanner(): void {
    this.scanSubscription?.unsubscribe();
    this.scanSubscription = this.scanDetectorService.onScanEvent$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter((event: ScanEvent) => event.type === 'start' || event.type === 'complete'),
      )
      .subscribe((event: ScanEvent) => {
        if (event.type === 'start') this.onScanStart();
        else if (event.type === 'complete' && event.code) this.onScanComplete(event.code);
      });

    this.keydownListener = (event: KeyboardEvent) => this.scanDetectorService.keyPressed(event.key);
    document.addEventListener('keydown', this.keydownListener, true);
  }

  private onScanStart(): void {
    const autocomplete = this.produitbox();
    if (this.isManualSearching || autocomplete?.overlayVisible) return;
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
    this.searchByBarcode(scannedCode);
    setTimeout(() => {
      this.isScanning = false;
    }, 400);
  }

  private clearInputValue(): void {
    const inputEl = this.produitbox()?.inputEL()?.nativeElement;
    if (inputEl) inputEl.value = '';
  }

  private startInputClearLoop(): void {
    const clearLoop = () => {
      if (!this.isScanning) return;
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

  private searchByBarcode(barcode: string): void {
    this.produitService
      .search({page: 0, size: 1, search: barcode, storageId: this.storageId()}, this.searchByStorage())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => {
          this.isScanning = false;
          this.produits.set([]);
          return of({body: []});
        }),
      )
      .subscribe(res => {
        const result = res.body ?? [];
        if (result.length === 1) {
          const found = result[0];
          this.produits.set([]);
          this.selectProduit.set(found);
          this._produitSelected.set(found);
          this.productScanned.emit(found);
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
    const inputEl = this.produitbox()?.inputEL()?.nativeElement;
    if (inputEl && !inputEl.value?.trim()) return;
    this.produitService
      .search({page: 0, size: this.pageSize(), search, storageId: this.storageId()}, this.searchByStorage())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of({body: []})),
      )
      .subscribe(res => {
        this.produits.set(res.body ?? []);
        this.selectProduit.set(null);
      });
  }
}
