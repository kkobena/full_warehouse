import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  model,
  OnInit,
  signal,
  viewChild
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {TooltipModule} from 'primeng/tooltip';
import {Drawer} from 'primeng/drawer';
import {
  NgbNav,
  NgbNavChangeEvent,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavOutlet
} from '@ng-bootstrap/ng-bootstrap';
import {Select} from 'primeng/select';
import {SaleCreationComponent} from '../sale-creation/sale-creation.component';
import {SaleAssuranceComponent} from '../sale-assurance/sale-assurance.component';
import {SaleCarnetComponent} from '../sale-carnet/sale-carnet.component';
import {SaleDevisComponent} from '../sale-devis/sale-devis.component';
import {CustomerOverlayPanelComponent, PendingSalesListComponent} from '../../ui';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {UserVendeurService} from '../../../../entities/sales/service/user-vendeur.service';
import {IUser} from '../../../../core/user/user.model';
import {
  ConfirmDialogComponent
} from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {ToastAlertComponent} from '../../../../shared/toast-alert/toast-alert.component';
import {CustomerDisplayService} from '../../data-access/services/customer-display.service';
import {MagasinService} from '../../../../entities/magasin/magasin.service';
import {AccountService} from '../../../../core/auth/account.service';
import {CashRegisterService} from '../../../../entities/cash-register/cash-register.service';
import {RemiseCacheService} from '../../data-access/services/remise-cache.service';
import {SalesApiService} from '../../data-access/services/sales-api.service';
import {finalize, interval} from 'rxjs';
import {ProduitSearch, SalesStatut} from '../../../../shared/model';
import {SaleForEditInfo, SaleId} from '../../../../shared/model/sales.model';
import {GlobalScannerService} from '../../../../shared/global-scanner.service';
import {ProduitService} from '../../../../entities/produit/produit.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {ScanAudioFeedbackService} from '../../../../shared/services/scan-audio-feedback.service';
import {getNavChangeMessage, SaleType} from '../../../../entities/sales/selling-home/sale-helper';
import {TranslateService} from '@ngx-translate/core';
import {AuthorizationService} from '../../data-access/services/authorization.service';

@Component({
  selector: 'app-sales-home',
  templateUrl: './sales-home.component.html',
  styleUrls: ['./sales-home.component.scss'],
  host: {
    '(window:keydown)': 'handleGlobalKeyboardEvent($event)',
  },
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TooltipModule,
    Drawer,
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    Select,
    SaleCreationComponent,
    SaleAssuranceComponent,
    SaleCarnetComponent,
    SaleDevisComponent,
    PendingSalesListComponent,
    CustomerOverlayPanelComponent,
    ConfirmDialogComponent,
    ToastAlertComponent,
  ],
})
export class SalesHomeComponent implements OnInit, AfterViewInit {
  readonly isPresale = input(false);
  readonly isDevis = input(false);
  initSaleForEditInfo = model<SaleForEditInfo>(null);
  showStock = signal(false);
  protected salesFacade = inject(SalesFacade);
  protected userVendeurService = inject(UserVendeurService); // Pour liste vendeurs uniquement
  protected confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  protected alert = viewChild.required<ToastAlertComponent>('alert');
  // Références aux composants enfants (tabs) pour déléguer l'ajout de produits
  protected saleCreation = viewChild<SaleCreationComponent>(SaleCreationComponent);
  protected saleAssurance = viewChild<SaleAssuranceComponent>(SaleAssuranceComponent);
  protected saleCarnet = viewChild<SaleCarnetComponent>(SaleCarnetComponent);
  protected saleDevis = viewChild<SaleDevisComponent>(SaleDevisComponent);
  protected active = signal('comptant');
  protected sidebarCollapsed = signal(false);
  // Thème devis: 'purple' | 'teal' | 'indigo' (temporaire pour test)
  protected devisTheme = signal<'purple' | 'teal' | 'indigo'>('teal');
  protected userSeller = signal<IUser | null>(null);
  protected appendTo = 'body'; // Utilisé dans p-select du template
  protected produitSelected: any | null = null;
  protected disableButton = true;
  protected PRODUIT_COMBO_RESULT_SIZE = 10;
  protected pendingSalesSidebar = signal(false);
  protected countPendingSales = signal('0');
  // Responsive state - passé aux composants enfants
  protected isSmallScreen = signal(false);
  protected isCashRegisterOpen = signal(false);
  protected showTheme = signal(false);
  protected devisThemeClass = computed(() => this.isDevisMode() ? `devis-mode-${this.devisTheme()}` : '');
  private router = inject(Router);
  private readonly apiService = inject(SalesApiService);
  private customerDisplayService = inject(CustomerDisplayService);
  private magasinService = inject(MagasinService);
  private accountService = inject(AccountService);
  private cashRegisterService = inject(CashRegisterService);
  private destroyRef = inject(DestroyRef);
  private remiseCacheService = inject(RemiseCacheService);
  protected remises = this.remiseCacheService.remises;
  private readonly translate = inject(TranslateService);
  private globalScanner = inject(GlobalScannerService);
  private produitService = inject(ProduitService);
  private notificationService = inject(NotificationService);
  private scanAudio = inject(ScanAudioFeedbackService);
  private authorizationService = inject(AuthorizationService);
  private isPresaleFromRoute = signal(false);
  protected isPresaleMode = computed(() => this.isPresale() || this.isPresaleFromRoute());
  private isDevisFromRoute = signal(false);
  protected isDevisMode = computed(() => this.isDevis() || this.isDevisFromRoute());
  // Scan global - file d'attente multi-scan
  private scanQueue: string[] = [];
  private processingQueue = false;
  private pendingScanCode = signal<string | null>(null);

  constructor() {
    this.showStock.set(this.authorizationService.canShowStock());
    this.salesFacade.resetCurrentSale();
    // Auto-disable button when no product selected
    effect(() => {
      this.disableButton = !this.produitSelected;
    });

    // Effect pour traiter un scan en attente quand le chargement se termine
    effect(() => {
      const code = this.pendingScanCode();
      const isLoading = this.salesFacade.loading();
      if (code && !isLoading) {
        this.pendingScanCode.set(null);
        setTimeout(() => this.enqueueScan(code), 100);
      }
    });
  }

  ngOnInit(): void {
    this.checkScreenSize();
    this.salesFacade.resetCurrentSale();

    // Activer le scanner global et s'abonner aux scans
    this.globalScanner.enable();
    this.globalScanner.onScan$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(code => this.enqueueScan(code));
    // Initialiser le caissier (utilisateur connecté)
    const currentUser = this.accountService.trackCurrentAccount()();
    if (currentUser) {
      this.salesFacade.setCashier(currentUser as IUser);
    }
    // Synchroniser le mode prevente dans le store
    this.salesFacade.setIsPresale(this.isPresale());
    // Synchroniser le mode devis dans le store
    this.salesFacade.setIsDevis(this.isDevis());
    // S'abonner au rechargement de vente (après annulation forçage stock)
    this.salesFacade.saleReloadedToEditSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      history.replaceState({}, '');
      this.iniLoadSaleForEdit();
    });

    // S'abonner à la reprise d'une vente en attente pour switcher le tab APRÈS hydratation du store
    this.salesFacade.resumePendingSaleSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const saleType = this.salesFacade.saleType();
      if (saleType === 'ASSURANCE') {
        this.active.set('assurance');
      } else if (saleType === 'CARNET') {
        this.active.set('carnet');
      } else {
        this.active.set('comptant');
      }
      this.focusActiveTab();
    });

    // Vérifier si une caisse est ouverte
    this.hasCashRegisterOpen();

    const saleInfo = this.router.currentNavigation()?.extras?.state?.['saleInfo'] ?? history.state?.saleInfo;
    if (saleInfo) {
      const isEdit = saleInfo.isEdit;
      const saleId: SaleId = saleInfo.saleId;
      const isPresale = saleInfo.isPresale === true;
      this.isPresaleFromRoute.set(isPresale);
      this.initSaleForEditInfo.set({saleId, isPresale, isEdit});
      this.loadSale(saleId);
    }

    // Initialiser le vendeur depuis le store ou depuis le caissier par défaut
    let currentSeller = this.salesFacade.seller();
    if (!currentSeller && currentUser) {
      currentSeller = currentUser as IUser;
      this.salesFacade.setSeller(currentSeller);
    }

    if (currentSeller) {
      this.userSeller.set(currentSeller);
      // Initialise l'afficheur client avec le nom du magasin
      this.magasinService
        .findCurrentUserMagasin()
        .then(magasin => {
          const storeName = magasin?.name || 'PHARMA SMART';
          this.customerDisplayService.initialize(storeName, currentSeller);
        })
        .catch(error => {
          console.error('Error loading store name:', error);
          this.customerDisplayService.initialize('PHARMA SMART', currentSeller);
        });
    }
  }

  ngAfterViewInit(): void {
    if (!this.isPresaleMode()) {
      this.loadPendingSalesCount();
      interval(60000)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          this.reloadPendingSalesCount();
        });
    }
  }
  protected loadPendingSalesCount(): void {
    this.apiService
      .countPendingSales({
        userId: this.salesFacade.cashier()?.id,
        statut: [SalesStatut.ACTIVE]
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(resp => this.countPendingSales.set(resp?.body?.toString() ?? '0'));
  }

  /**
   * Transforme la vente comptant courante en vente ASSURANCE.
   */
  protected onChangeCashSaleToVo(): void {
    this.salesFacade.transformCashSaleToAssurance();
  }

  /**
   * Transforme la vente comptant courante en vente CARNET.
   */
  protected onChangeCashSaleToCarnet(): void {
    this.salesFacade.transformCashSaleToCarnet();
  }

  /**
   * Recharge l'état de la caisse (appelé après ouverture de caisse depuis un composant enfant)
   */
  protected onCashRegisterStatusChanged(): void {
    this.cashRegisterService.getConnectedUserHasOpenCashRegister().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.isCashRegisterOpen.set(res.body ?? false);
      },
      error: () => {
        this.isCashRegisterOpen.set(false);
      },
    });
  }

  /**
   * Basculer vers l'onglet COMPTANT après finalisation d'une vente ASSURANCE/CARNET
   */
  protected onSwitchToComptant(): void {
    this.active.set('comptant');
    this.focusActiveTab();
  }

  /**
   * Gère les raccourcis globaux (navigation inter-onglets, ventes en attente).
   * Les F-keys contextuelles (F1-F10) sont gérées par les composants enfants.
   */
  handleGlobalKeyboardEvent(event: KeyboardEvent): void {
    // 1. Raccourcis clavier (Alt+1/2/3, F11)
    // Utilise event.code (indépendant du layout clavier) pour éviter l'insertion
    // de caractères spéciaux (¹ ² ³, emoji, etc.) quand un input a le focus.
    if (event.altKey && !event.ctrlKey && ['Digit1', 'Digit2', 'Digit3'].includes(event.code)) {
      event.preventDefault();
      const digit = event.code.replace('Digit', '');
      // En mode devis, pas de tab assurance (Alt+2 ignoré)
      if (this.isDevisMode() && digit === '2') {
        return;
      }
      const tabMap: Record<string, string> = this.isDevisMode()
        ? {'1': 'comptant', '3': 'carnet'}  // En mode devis: 1=comptant, 3=carnet
        : {'1': 'comptant', '2': 'assurance', '3': 'carnet'};
      const targetTab = tabMap[digit];
      if (targetTab) {
        this.switchToTab(targetTab);
      }
      return;
    }

    if (event.key === 'F11' && !this.isPresaleMode()) {
      event.preventDefault();
      this.openPendingSales();
      return;
    }

    // 2. Scanner global - toujours traiter pour détecter les codes-barres
    const result = this.globalScanner.processKeyEvent(event);
    if (result.isScanInProgress) {
      // Ne pas bloquer la saisie dans les inputs en mode TIMING
      // (les caractères du scanner apparaissent dans l'input, nettoyés après dispatch)
      // En mode PREFIX_SUFFIX, on peut toujours bloquer car la détection est certaine
      const activeEl = document.activeElement;
      const isInInput = activeEl instanceof HTMLInputElement || activeEl instanceof HTMLTextAreaElement;
      if (!isInInput) {
        event.preventDefault();
      }
    }
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.salesFacade.setSelectedProduct(null);
    const fromTab = this.active();
    const toTab = evt.nextId;
    const currentSale = this.salesFacade.currentSale();
    const asCurrentSale = currentSale && currentSale.salesLines && currentSale.salesLines.length > 0;
    if (!this.isDevisMode() && asCurrentSale) {
      evt.preventDefault();
      this.confirmDialog().onConfirm(
        () => {
          this.dispatchTabTransition(fromTab, toTab);
          this.active.set(toTab);
          this.focusActiveTab();
        },
        'Changement de type de vente',
       this.isFromVo(fromTab)?'La vente sera mise en attente. Voulez-vous continuer ?': this.getMessateOnNavChange(evt),
      );
    } else {
      if (asCurrentSale) {
        evt.preventDefault();
        this.confirmDialog().onConfirm(
          () => {
            this.salesFacade.resetCurrentSale();
            this.goToNextTab(toTab);
          },
          'Changement de type de vente',
          'La proforma en cours sera perdue. Souhaitez-vous continuer ?'
        );
      } else {
        this.goToNextTab(toTab);
      }

    }
  }

  protected onSelectUser(): void {
    const seller = this.userSeller();
    if (seller) {
      this.salesFacade.setSeller(seller);
    }
  }

  // ===== Transitions entre onglets =====

  protected toggleSidebar(): void {
    this.sidebarCollapsed.update(collapsed => !collapsed);
  }

  protected previousState(): void {
    window.history.back();
  }

  protected openPendingSales(): void {
    this.pendingSalesSidebar.set(true);
  }

  protected closePendingSales(): void {
    this.pendingSalesSidebar.set(false);
  }

  protected onDrawerHide(): void {
    this.pendingSalesSidebar.set(false);
    // PrimeNG Drawer relies on animationend to remove the mask from the DOM.
    // If the callback doesn't fire (double disableModality call), the mask stays
    // and blocks all pointer events. Force cleanup after the animation duration.
    setTimeout(() => {
      document.querySelectorAll('.p-drawer-mask').forEach(el => el.remove());
      document.body.classList.remove('p-overflow-hidden');
    }, 400);
  }

  protected onSaleResumed(_sale: any): void {
    // Fermer le drawer. Le switch de tab se fait dans le handler RESUME_PENDING_SALE
    // (après hydratation du store) pour garantir que saleType est correct.
    this.pendingSalesSidebar.set(false);
  }

  private goToNextTab(toTab: any): void {
    this.salesFacade.setSelectedCustomer(null);
    this.active.set(toTab);
    this.focusActiveTab();
  }

  private checkScreenSize(): void {
    this.isSmallScreen.set(window.innerWidth < 1800);
  }

  private reloadPendingSalesCount(): void {
    // if (this.salesFacade.currentSale() == null) {
    this.loadPendingSalesCount();
    // }
  }

  private getMessateOnNavChange(evt: NgbNavChangeEvent): string {
    return getNavChangeMessage(evt.nextId, this.translate);
  }
private isFromVo(fromTab: string): boolean {
   return fromTab === SaleType.ASSURANCE || fromTab === SaleType.CARNET;
}
  /**
   * Dispatch de la transition entre deux onglets quand une vente est en cours.
   * Utilisé par onNavChange() et switchToTab().
   */
  private dispatchTabTransition(fromTab: string, toTab: string): void {
    if (fromTab === SaleType.COMPTANT && toTab === SaleType.ASSURANCE) {
      this.switchComptantToAssurance();
    } else if (fromTab === SaleType.COMPTANT && toTab === SaleType.CARNET) {
      this.switchComptantToCarnet();
    } else if (fromTab === SaleType.ASSURANCE && toTab === SaleType.COMPTANT) {
      this.switchAssuranceToComptant();
    } else if (fromTab === SaleType.ASSURANCE && toTab === SaleType.CARNET) {
      this.switchAssuranceToCarnet();
    } else if (fromTab === SaleType.CARNET && toTab === SaleType.COMPTANT) {
      this.switchCarnetToComptant();
    } else if (fromTab === SaleType.CARNET && toTab === SaleType.ASSURANCE) {
      this.switchCarnetToAssurance();
    }
  }

  /** COMPTANT → ASSURANCE : vide le client puis transforme la vente en assurance */
  private switchComptantToAssurance(): void {
    this.salesFacade.setSelectedCustomer(null);
    this.onChangeCashSaleToVo();
  }

  /** COMPTANT → CARNET : vide le client puis transforme la vente en carnet */
  private switchComptantToCarnet(): void {
    this.salesFacade.setSelectedCustomer(null);
    this.onChangeCashSaleToCarnet();
  }

  /** ASSURANCE → COMPTANT : annule la vente VO en cours (mise en attente) */
  private switchAssuranceToComptant(): void {
    this.salesFacade.resetCurrentSale();
  }

  /** ASSURANCE → CARNET : transforme la vente assurance en carnet (conserve le client) */
  private switchAssuranceToCarnet(): void {
    this.salesFacade.setSelectedCustomer(null);
    this.salesFacade.resetCurrentSale();
  }

  /** CARNET → COMPTANT : annule la vente carnet en cours */
  private switchCarnetToComptant(): void {
    this.salesFacade.resetCurrentSale();
  }

  /** CARNET → ASSURANCE : vide le client puis transforme la vente en assurance */
  private switchCarnetToAssurance(): void {
    this.salesFacade.setSelectedCustomer(null);
    this.salesFacade.resetCurrentSale();
  }

  /**
   * Met le focus sur le champ de recherche produit du composant enfant actif
   * NOTE: Appelé lors du changement de tab ou après actions (ajout/modification produit)
   * Pour le focus INITIAL sur recherche client (ASSURANCE/CARNET), voir ngAfterViewChecked des composants enfants
   */
  private focusActiveTab(): void {
    setTimeout(() => {
      switch (this.active()) {
        case 'comptant':
          if (this.isDevisMode()) {
            this.saleDevis()?.focusProductSearch();
          } else {
            this.saleCreation()?.focusProductSearch();
          }
          break;
        case 'assurance':
          this.saleAssurance()?.focusProductSearch();
          break;
        case 'carnet':
          this.saleCarnet()?.focusProductSearch();
          break;
      }
    }, 100);
  }

  /**
   * Vérifie si l'utilisateur a une caisse ouverte
   */
  private hasCashRegisterOpen(): void {
    this.cashRegisterService
      .getConnectedUserHasOpenCashRegister()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.isCashRegisterOpen.set(res.body ?? false);
        },
        error: () => {
          this.isCashRegisterOpen.set(false);
        },
      });
  }

  /**
   * Charge une vente pour édition (vente clôturée ASSURANCE/CARNET)
   *
   */
  private loadSale(saleId: SaleId): void {
    this.salesFacade.loadSale(saleId);
  }



  private iniLoadSaleForEdit(): void {
    const sale = this.salesFacade.currentSale();
    const isLoading = this.salesFacade.loading();
    const error = this.salesFacade.error();

    // Si erreur de chargement
    if (error && !isLoading) {
      this.router.navigate(['/sales']);
      return;
    }

    // Si vente chargée avec succès
    if (sale && sale.saleId && !isLoading) {
      if (this.isPresaleMode() && sale.statut !== SalesStatut.PROCESSING) {
        this.salesFacade.resetCurrentSale();
        this.router.navigate(['/sales']);
        return;
      }

      if (sale.statut === SalesStatut.CLOSED) {
        this.salesFacade.resetCurrentSale();
        this.router.navigate(['/sales']);
        return;
      }
      // Basculer vers l'onglet approprié selon le type de vente
      if (sale.natureVente === 'ASSURANCE') {
        this.active.set('assurance');
      } else if (sale.natureVente === 'CARNET') {
        this.active.set('carnet');
      } else {
        this.active.set('comptant');
      }

      // Charger le vendeur si présent et pas encore défini
      if (sale.sellerId && !this.userSeller()) {
        const seller = this.userVendeurService.vendeurs().find(u => u.id === sale.sellerId);
        if (seller) {
          this.userSeller.set(seller);
        }
      }

      // Focus sur le tab actif après chargement
      this.focusActiveTab();
    }
  }

  /**
   * Bascule vers un onglet avec confirmation si une vente est en cours.
   * Réutilise la même logique que onNavChange() pour la confirmation.
   */
  private switchToTab(tab: string): void {
    const fromTab = this.active();
    if (tab === fromTab) {
      return;
    }

    const currentSale = this.salesFacade.currentSale();
    if (currentSale && currentSale.salesLines && currentSale.salesLines.length > 0) {
      this.confirmDialog().onConfirm(
        () => {
          this.dispatchTabTransition(fromTab, tab);
          this.active.set(tab);
          this.focusActiveTab();
        },
        'Changement de type de vente',
        'Vous avez une vente en cours. Voulez-vous vraiment changer de type de vente ?',
      );
    } else {
      this.salesFacade.setSelectedCustomer(null);
      this.active.set(tab);
      this.focusActiveTab();
    }
  }

  // ===== Scan global - file d'attente et dispatch =====

  private enqueueScan(code: string): void {
    if (this.salesFacade.loading()) {
      this.pendingScanCode.set(code);
      return;
    }
    this.scanQueue.push(code);
    this.processNextScan();
  }

  private processNextScan(): void {
    if (this.processingQueue || this.scanQueue.length === 0) {
      return;
    }
    this.processingQueue = true;
    const code = this.scanQueue.shift()!;
    this.searchAndDispatch(code);
  }

  private searchAndDispatch(code: string): void {
    this.produitService
      .search({page: 0, size: 5, search: code}, false)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.processingQueue = false;
          this.processNextScan();
        }),
      )
      .subscribe({
        next: res => {
          const results = res.body || [];
          if (results.length === 1) {
            this.scanAudio.beepSuccess();
            this.dispatchScannedProduct(results[0]);
          } else if (results.length === 0) {
            this.scanAudio.beepError();
            this.notificationService.error(`Produit non trouvé : ${code}`, 'Scan');
          } else {
            this.scanAudio.beepSuccess();
            this.dispatchScannedProduct(results[0]);
          }
        },
        error: () => {
          this.scanAudio.beepError();
          this.notificationService.error('Erreur de recherche produit', 'Scan');
        },
      });
  }

  private dispatchScannedProduct(product: ProduitSearch): void {
    // Nettoyer l'input actif (code-barres du scanner visible dans un champ)
    this.clearActiveInputAfterScan();

    switch (this.active()) {
      case 'comptant':
        if (this.isDevisMode()) {
          this.saleDevis()?.onProductScanned(product);
        } else {
          this.saleCreation()?.onProductScanned(product);
        }
        break;
      case 'assurance':
        this.saleAssurance()?.onProductScanned(product);
        break;
      case 'carnet':
        this.saleCarnet()?.onProductScanned(product);
        break;
    }
  }

  /**
   * Nettoie les traces du scan : reset le product-search et vide l'input actif
   * si ce n'est pas le champ produit (ex: champ paiement).
   */
  private clearActiveInputAfterScan(): void {
    // 1. Reset le product-search (supprime le texte code-barres + cache dropdown)
    switch (this.active()) {
      case 'comptant':
        if (this.isDevisMode()) {
          this.saleDevis()?.productSearchComponent()?.reset();
        } else {
          this.saleCreation()?.productSearchComponent()?.reset();
        }
        break;
      case 'assurance':
        this.saleAssurance()?.productSearchComponent()?.reset();
        break;
      case 'carnet':
        this.saleCarnet()?.productSearchComponent()?.reset();
        break;
    }

    // 2. Si le focus est dans un autre input (ex: champ paiement), vider le code-barres
    const activeEl = document.activeElement;
    if (activeEl instanceof HTMLInputElement || activeEl instanceof HTMLTextAreaElement) {
      const isProductSearch = activeEl.closest('app-product-search');
      if (!isProductSearch) {
        activeEl.value = '';
        activeEl.dispatchEvent(new Event('input', {bubbles: true}));
      }
    }
  }
}
