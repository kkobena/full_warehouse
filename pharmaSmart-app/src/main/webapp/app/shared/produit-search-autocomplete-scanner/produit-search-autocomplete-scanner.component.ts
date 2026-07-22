import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  OnDestroy,
  OnInit,
  output,
  signal,
  viewChild
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {TranslatePipe} from '@ngx-translate/core';
import {DecimalPipe} from '@angular/common';
import {
  APPEND_TO,
  PRODUIT_COMBO_MIN_LENGTH,
  PRODUIT_NOT_FOUND
} from '../constants/pagination.constants';
import {ProduitSearch} from '../model';
import {ProduitService} from '../../entities/produit/produit.service';
import {catchError, debounceTime, filter, of, Subject, Subscription} from 'rxjs';
import {ScanDetectorService, ScanEvent} from '../scan-detector.service';
import {FloatLabelComponent, SelectSearchComponent} from '../ui';

@Component({
  selector: 'app-produit-search-autocomplete-scanner',
  imports: [SelectSearchComponent, FormsModule, FloatLabelComponent, TranslatePipe, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './produit-search-autocomplete-scanner.component.html',
})
export class ProduitSearchAutocompleteScannerComponent implements OnDestroy, OnInit {
  produits = signal<ProduitSearch[]>([]);
  private readonly produitboxCmp = viewChild.required('produitbox', {read: SelectSearchComponent});
  private readonly produitboxEl = viewChild.required('produitbox', {read: ElementRef<HTMLElement>});
  selectProduit = signal<ProduitSearch | null>(null);
  includeDetails = input<boolean>(true);
  autofocus = input<boolean>(true);
  showClear = input<boolean>(true);
  pageSize = input<number>(5);
  storageId = input<number>(null);
  style = input<{}>({width: '100%'});
  inputStyle = input<{}>({width: '100%'});
  enableScanner = input<boolean>(true);
  disabled = input<boolean>(false);
  protected readonly isDisabledEffective = () => this.disabled();

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
  /**
   * Passé à `[typeahead]` de `app-select-search` dans le seul but d'être « observé » :
   * ng-select désactive alors son propre filtrage client (par `bindLabel`), qui sinon
   * masquait les résultats retournés par le backend sur un code CIP (le texte tapé ne
   * matche pas `libelle`). La recherche réelle continue de passer par `searchTrigger$`
   * via `(searched)` → `searchFn()`, seul canal qui respecte `PRODUIT_COMBO_MIN_LENGTH`.
   */
  protected readonly typeaheadSink$ = new Subject<string>();

  private isScanning = false;
  private isManualSearching = false;
  private manualSearchTimeout: ReturnType<typeof setTimeout> | null = null;
  private keydownListener?: (event: KeyboardEvent) => void;
  private animationFrameId: number | null = null;
  private scanSubscription?: Subscription;
  private readonly _produitSelected = signal<ProduitSearch | null>(null);

  constructor() {
    this.searchTrigger$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe(search => this.loadProduits(search));
    this.typeaheadSink$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  /**
   * Binding interne de `app-select-search` (via `[(ngModel)]` dans le template de CE
   * composant). N'expose plus de `ControlValueAccessor` : le consommateur pilote la
   * sélection via l'`@Output() selectedProduit` et remet à zéro l'affichage via `reset()`,
   * comme `commande-product-search` — dont ce composant reprend ici le même schéma non-CVA.
   */
  get produitSelected(): ProduitSearch | null {
    return this._produitSelected();
  }

  set produitSelected(value: any) {
    if (typeof value === 'string') {
      return;
    }
    if (this.isScanning) {
      return;
    }
    if (this._produitSelected() !== value) {
      this._produitSelected.set(value as ProduitSearch | null);
    }
  }

  ngOnInit(): void {
    // Setup initial state
    if (this.enableScanner()) {
      this.setupBarcodeScanner();
    }

    if (this.autofocus()) {
      this.getFocus();
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
      {injector: this.injector},
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

  searchFn(term: string): void {
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

    this.searchTrigger$.next(term);
  }

  onSelect(): void {
    const selected = this.produitSelected;
    this.selectProduit.set(selected);
    this.selectedProduit.emit(selected);
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
      const el = this.produitboxEl()?.nativeElement.querySelector('input');

      if (el) {
        el.focus();
        el.select();
      }

      // Une seule fermeture du dropdown avec un délai raisonnable
      const cmp = this.produitboxCmp();
      if (cmp) {
        cmp.close();
        // Un seul timeout de secours si vraiment nécessaire
        setTimeout(() => cmp.close(), 50);
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

    const cmp = this.produitboxCmp();
    if (cmp) {
      cmp.close();
      // Un seul délai de fermeture au lieu de l'interval agressif
      setTimeout(() => cmp.close(), 100);
    }

    const inputEl = this.produitboxEl()?.nativeElement.querySelector('input');
    if (inputEl) {
      inputEl.value = '';
    }
  }

  private setupBarcodeScanner(): void {
    // Nettoyer l'ancienne subscription pour éviter les fuites mémoire
    this.scanSubscription?.unsubscribe();

    // Utiliser l'Observable RxJS au lieu des callbacks
    this.scanSubscription = this.scanDetectorService.onScanEvent$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter((event: ScanEvent) => event.type === 'start' || event.type === 'complete' || event.type === 'reset'),
      )
      .subscribe((event: ScanEvent) => {
        if (event.type === 'start') {
          this.onScanStart();
        } else if (event.type === 'complete' && event.code) {
          this.onScanComplete(event.code);
        } else if (event.type === 'reset') {
          // Un scan détecté (frappe rapide) a été abandonné côté service sans jamais
          // produire de code valide : sans ce cas, `isScanning` restait bloqué à `true`
          // et la boucle de nettoyage de la saisie continuait à vider le champ à chaque
          // frame, rendant la saisie manuelle impossible jusqu'au rechargement de la page.
          this.isScanning = false;
          this.stopInputClearLoop();
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
    const isOpen = this.produitboxCmp()?.isOpen();
    if (this.isManualSearching || isOpen) {
      return;
    }
    this.isScanning = true;
    this.clearInputValue();
    this.startInputClearLoop();
  }

  private onScanComplete(scannedCode: string): void {
    // Ignorer si l'utilisateur est en train de faire une recherche manuelle
    // On vérifie : le flag isManualSearching OU si le dropdown est visible
    const isOpen = this.produitboxCmp()?.isOpen();
    if (this.isManualSearching || isOpen) {
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
    const inputEl = this.produitboxEl()?.nativeElement.querySelector('input');
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
    // Garde-fou défensif : quel que soit l'état du détecteur de scan, cette boucle ne
    // doit jamais tourner indéfiniment — un vrai scan dure quelques centaines de ms
    // (cf. `scanMaxTime`). Sans cette limite, un scénario non couvert dans le service de
    // détection (frappe considérée à tort comme un scan qui ne se termine jamais) bloque
    // la saisie manuelle jusqu'au rechargement de la page.
    const startedAt = Date.now();
    const maxDurationMs = 2000;
    const clearLoop = () => {
      if (!this.isScanning || Date.now() - startedAt > maxDurationMs) {
        this.isScanning = false;
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
          return of({body: []});
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
          return of({body: []});
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
          this.produitboxCmp()?.close();
        } else {
          this.selectProduit.set(null);
        }
      });
  }
}
