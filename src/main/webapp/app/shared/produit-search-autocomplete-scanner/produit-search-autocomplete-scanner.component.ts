import { Component, effect, forwardRef, inject, input, OnDestroy, OnInit, output, signal, viewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AutoComplete } from 'primeng/autocomplete';
import { FloatLabel } from 'primeng/floatlabel';
import { TranslatePipe } from '@ngx-translate/core';
import { DecimalPipe } from '@angular/common';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../constants/pagination.constants';
import { ProduitSearch } from '../model/produit.model';
import { ProduitService } from '../../entities/produit/produit.service';
import { debounceTime, Subject, Subscription } from 'rxjs';
import { ScanDetectorService } from '../scan-detector.service';

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
  templateUrl: './produit-search-autocomplete-scanner.component.html',
})
export class ProduitSearchAutocompleteScannerComponent implements ControlValueAccessor, OnDestroy, OnInit {
  produits = signal<ProduitSearch[]>([]);
  produitbox = viewChild.required<AutoComplete>('produitbox');
  selectProduit = signal<ProduitSearch | null>(null);
  private isScanning = false; // Variable booléenne normale pour synchronisation immédiate
  includeDetails = input<boolean>(true);
  autofocus = input<boolean>(true);
  showClear = input<boolean>(true);
  pageSize = input<number>(5);
  storageId = input<number>(null);
  style = input<{}>({ width: '100%' });
  inputStyle = input<{}>({ width: '100%' });
  enableScanner = input<boolean>(true);
  selectedProduit = output<ProduitSearch | null>();
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
      const autocomplete = this.produitbox();
      if (autocomplete) {
        autocomplete.hide();
      }
    });
    this.searchSubscription = this.searchTrigger$.pipe(debounceTime(300)).subscribe(search => this.loadProduits(search));
  }

  private _produitSelected = signal<ProduitSearch | null>(null);

  // Getter / Setter pour ngModel
  get produitSelected(): ProduitSearch | null {
    return this._produitSelected();
  }

  set produitSelected(value: any) {
    if (typeof value === 'string') {
      return;
    }
    // BLOQUER TOUTE mise à jour pendant le scan pour éviter que le binding réaffiche le produit
    if (this.isScanning) {
      console.warn('Setter bloqué pendant le scan. Valeur ignorée:', value?.libelle || value);
      return;
    }
    if (this._produitSelected() !== value) {
      console.warn('Setter autorisé. Mise à jour avec:', value?.libelle || value);
      this._produitSelected.set(value as ProduitSearch | null);
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
    // Bloquer TOUTE mise à jour du parent pendant le scan
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

  onNgModelChange(value: ProduitSearch | null): void {
    // Bloquer pendant le scan pour éviter que PrimeNG déclenche des mises à jour
    if (this.isScanning) {
      return;
    }
    this.produitSelected = value;
  }

  getFocus(): void {
    setTimeout(() => {
      const el = this.produitbox().inputEL?.nativeElement;
      const autocomplete = this.produitbox();

      el.focus();
      el.select();

      // Forcer la fermeture du dropdown pour éviter d'afficher "Aucun produit"
      if (autocomplete) {
        autocomplete.hide();

        // Forcer à nouveau après un court délai
        setTimeout(() => {
          autocomplete.hide();
        }, 10);

        setTimeout(() => {
          autocomplete.hide();
        }, 50);

        setTimeout(() => {
          autocomplete.hide();
        }, 100);
      }
    }, 50);
  }

  reset(): void {
    // Désactiver le mode scan
    this.isScanning = false;

    // Mettre un tableau vide pour éviter le message "Aucun produit"
    // Note: On ne met PAS un produit factice car ça pourrait créer des problèmes
    this.produits.set([]);

    // Réinitialiser tous les états internes
    this._produitSelected.set(null);
    this.selectProduit.set(null);

    // Forcer la fermeture du panel de suggestions de manière agressive
    const autocomplete = this.produitbox();
    if (autocomplete) {
      autocomplete.hide();

      // Continuer à forcer la fermeture pendant 200ms
      const hideInterval = setInterval(() => {
        autocomplete.hide();
      }, 10);

      setTimeout(() => {
        clearInterval(hideInterval);
      }, 200);
    }

    // Vider l'input du composant AutoComplete
    const inputEl = autocomplete?.inputEL?.nativeElement;
    if (inputEl) {
      inputEl.value = '';
    }

    // Forcer la mise à jour du ngModel
    this.onChange(null);
  }

  private onChange: (_: any) => void = () => {};

  private onTouched: () => void = () => {};

  private setupBarcodeScanner(): void {
    // Variable pour stocker l'intervalle de vidage
    let clearInputInterval: any = null;

    // Configurer le callback appelé dès le DÉBUT du scan (touches rapides détectées)
    this.scanDetectorService.setScanStartCallback(() => {
      this.isScanning = true;

      const inputEl = this.produitbox().inputEL?.nativeElement;

      // Vider immédiatement l'input
      if (inputEl) {
        inputEl.value = '';
      }

      // Démarrer une boucle qui vide l'input en continu pendant le scan
      clearInputInterval = setInterval(() => {
        if (inputEl && inputEl.value !== '') {
          inputEl.value = '';
        }
      }, 5);
    });

    // Configurer le callback pour recevoir le code complet une fois le scan terminé
    this.scanDetectorService.setScanCallback((scannedCode: string) => {
      // Arrêter la boucle de vidage
      if (clearInputInterval) {
        clearInterval(clearInputInterval);
        clearInputInterval = null;
      }

      const inputEl = this.produitbox().inputEL?.nativeElement;

      // Vider l'input une dernière fois
      if (inputEl) {
        inputEl.value = '';
      }

      // Clear any other focused input that might have received scanner characters
      const activeElement = document.activeElement as HTMLInputElement;
      if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {
        activeElement.value = '';
      }

      // Emit the scanned barcode
      this.onBarcodeScanned.emit(scannedCode);

      this.searchByBarcode(scannedCode);

      setTimeout(() => {
        this.isScanning = false;
      }, 600);
    });

    this.keydownListener = (event: KeyboardEvent) => {
      this.scanDetectorService.keyPressed(event.key);
    };

    document.addEventListener('keydown', this.keydownListener, true);
  }

  private removeBarcodeScanner(): void {
    if (this.keydownListener) {
      document.removeEventListener('keydown', this.keydownListener, true);
      this.keydownListener = undefined;
    }
    // Nettoyer les callbacks pour éviter les fuites mémoire
    this.scanDetectorService.setScanCallback(null);
    this.scanDetectorService.setScanStartCallback(null);
  }

  private searchByBarcode(barcode: string): void {
    // isScanning est déjà activé dans le callback du scanner (setupBarcodeScanner)

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
      .subscribe(res => {
        const result = res.body || [];
        if (result.length === 1) {
          const selected = result[0];

          // Vider les suggestions pour éviter que PrimeNG réaffiche
          this.produits.set([]);

          // NE PAS mettre à jour produitSelected pour éviter l'affichage dans l'input
          // On émet directement au parent sans passer par le modèle
          this.selectProduit.set(selected);
          this.selectedProduit.emit(selected);

          // La boucle de vidage continue de tourner (démarrée dans setScanStartCallback)
          // Elle sera arrêtée par le timeout dans setScanCallback
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
