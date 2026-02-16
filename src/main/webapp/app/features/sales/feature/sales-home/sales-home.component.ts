import { AfterViewInit, Component, computed, DestroyRef, effect, inject, input, model, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { Drawer } from 'primeng/drawer';
import { NgbNav, NgbNavChangeEvent, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { Select } from 'primeng/select';
import { SaleCreationComponent } from '../sale-creation/sale-creation.component';
import { SaleAssuranceComponent } from '../sale-assurance/sale-assurance.component';
import { SaleCarnetComponent } from '../sale-carnet/sale-carnet.component';
import { CustomerOverlayPanelComponent, PendingSalesListComponent } from '../../ui';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { UserVendeurService } from '../../../../entities/sales/service/user-vendeur.service';
import { IUser } from '../../../../core/user/user.model';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../../shared/toast-alert/toast-alert.component';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { MagasinService } from '../../../../entities/magasin/magasin.service';
import { AccountService } from '../../../../core/auth/account.service';
import { CashRegisterService } from '../../../../entities/cash-register/cash-register.service';
import { RemiseCacheService } from '../../data-access/services/remise-cache.service';
import { SalesApiService } from '../../data-access/services/sales-api.service';
import { interval } from 'rxjs';
import { SalesStatut } from '../../../../shared/model';
import { SaleForEditInfo, SaleId } from '../../../../shared/model/sales.model';

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
    PendingSalesListComponent,
    CustomerOverlayPanelComponent,
    ConfirmDialogComponent,
    ToastAlertComponent,
  ],
})
export class SalesHomeComponent implements OnInit, AfterViewInit {
  private router = inject(Router);
  protected salesFacade = inject(SalesFacade);
  private readonly apiService = inject(SalesApiService);
  protected userVendeurService = inject(UserVendeurService); // Pour liste vendeurs uniquement
  private customerDisplayService = inject(CustomerDisplayService);
  private magasinService = inject(MagasinService);
  private accountService = inject(AccountService);
  private cashRegisterService = inject(CashRegisterService);
  private destroyRef = inject(DestroyRef);
  private remiseCacheService = inject(RemiseCacheService);
  protected confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  protected alert = viewChild.required<ToastAlertComponent>('alert');
  // Références aux composants enfants (tabs) pour déléguer l'ajout de produits
  protected saleCreation = viewChild<SaleCreationComponent>(SaleCreationComponent);
  protected saleAssurance = viewChild<SaleAssuranceComponent>(SaleAssuranceComponent);
  protected saleCarnet = viewChild<SaleCarnetComponent>(SaleCarnetComponent);
  protected active = signal('comptant');
  protected sidebarCollapsed = signal(false);
  readonly isPresale = input(false);
  private isPresaleFromRoute = signal(false);
  protected isPresaleMode = computed(() => this.isPresale() || this.isPresaleFromRoute());
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
  protected remises = this.remiseCacheService.remises;
  initSaleForEditInfo = model<SaleForEditInfo>(null);

  constructor() {
    this.salesFacade.resetCurrentSale();
    // Auto-disable button when no product selected
    effect(() => {
      this.disableButton = !this.produitSelected;
    });
  }

  private checkScreenSize(): void {
    this.isSmallScreen.set(window.innerWidth < 1800);
  }

  ngOnInit(): void {
    this.checkScreenSize();
    this.salesFacade.resetCurrentSale();
    // Initialiser le caissier (utilisateur connecté)
    const currentUser = this.accountService.trackCurrentAccount()();
    if (currentUser) {
      this.salesFacade.setCashier(currentUser as IUser);
    }
    // Synchroniser le mode prevente dans le store
    this.salesFacade.setIsPresale(this.isPresale());
    // S'abonner au rechargement de vente (après annulation forçage stock)
    this.salesFacade.saleReloadedToEditSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      history.replaceState({}, '');
      this.iniLoadSaleForEdit();
    });

    // Vérifier si une caisse est ouverte
    this.hasCashRegisterOpen();
    const saleInfo = this.router.currentNavigation()?.extras?.state?.['saleInfo'] ?? history.state?.saleInfo;
    if (saleInfo) {
      const isEdit = saleInfo.isEdit;
      const saleId: SaleId = saleInfo.saleId;
      const isPresale = saleInfo.isPresale === true;
      this.isPresaleFromRoute.set(isPresale);
      this.initSaleForEditInfo.set({ saleId, isPresale, isEdit });
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

  private reloadPendingSalesCount(): void {
    // if (this.salesFacade.currentSale() == null) {
    this.loadPendingSalesCount();
    // }
  }

  loadPendingSalesCount(): void {
    this.apiService
      .countPendingSales({
        userId: this.salesFacade.cashier()?.id,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(resp => this.countPendingSales.set(resp?.body?.toString() ?? '0'));
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    const newTab = evt.nextId;
    const currentSale = this.salesFacade.currentSale();
    if (currentSale && currentSale.salesLines && currentSale.salesLines.length > 0) {
      this.confirmDialog().onConfirm(
        () => {
          this.active.set(newTab);
          this.focusActiveTab();
        },
        'Changement de type de vente',
        'Vous avez une vente en cours. Voulez-vous vraiment changer de type de vente ?',
      );
      evt.preventDefault();
    } else {
      this.active.set(newTab);
      this.focusActiveTab();
    }
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
          this.saleCreation()?.focusProductSearch();
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
   * Recharge l'état de la caisse (appelé après ouverture de caisse depuis un composant enfant)
   */
  onCashRegisterStatusChanged(): void {
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
  onSwitchToComptant(): void {
    this.active.set('comptant');
    this.focusActiveTab();
  }

  protected onSelectUser(): void {
    const seller = this.userSeller();
    if (seller) {
      this.salesFacade.setSeller(seller);
    }
  }

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

  protected onSaleResumed(sale: any): void {
    // La vente a été reprise, fermer le drawer
    this.pendingSalesSidebar.set(false);
    // Le facade a déjà chargé la vente via resumePendingSale
    // Basculer vers l'onglet approprié selon le type de vente
    if (sale.natureVente === 'ASSURANCE') {
      this.active.set('assurance');
    } else if (sale.natureVente === 'CARNET') {
      this.active.set('carnet');
    } else {
      this.active.set('comptant');
    }
  }

  /**
   * Charge une vente pour édition (vente clôturée ASSURANCE/CARNET)
   * Conforme à l'ancien: selling-home.component.ts onLoadPrevente()
   */
  private loadSale(saleId: SaleId): void {
    // Appel du rxMethod - il met à jour le store automatiquement
    this.salesFacade.loadSale(saleId);

    // Écouter les changements du store pour basculer l'onglet
    // (après que la vente soit chargée)
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
      }
      // const editData = this.initSaleForEditInfo();
      if (sale.statut === SalesStatut.CLOSED) {
        this.salesFacade.resetCurrentSale();
        this.router.navigate(['/sales']);
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

  // ===== Raccourcis clavier globaux =====

  /**
   * Gère les raccourcis globaux (navigation inter-onglets, ventes en attente).
   * Les F-keys contextuelles (F1-F10) sont gérées par les composants enfants.
   */
  handleGlobalKeyboardEvent(event: KeyboardEvent): void {
    // Alt+1/2/3 : Switch type de vente
    if (event.altKey && !event.ctrlKey && ['1', '2', '3'].includes(event.key)) {
      event.preventDefault();
      const tabMap: Record<string, string> = { '1': 'comptant', '2': 'assurance', '3': 'carnet' };
      this.switchToTab(tabMap[event.key]);
      return;
    }

    // F11 : Ouvrir ventes en attente (pas en mode prévente)
    if (event.key === 'F11' && !this.isPresaleMode()) {
      event.preventDefault();
      this.openPendingSales();
      return;
    }
  }

  /**
   * Bascule vers un onglet avec confirmation si une vente est en cours.
   * Réutilise la même logique que onNavChange() pour la confirmation.
   */
  private switchToTab(tab: string): void {
    if (tab === this.active()) return;

    const currentSale = this.salesFacade.currentSale();
    if (currentSale && currentSale.salesLines && currentSale.salesLines.length > 0) {
      this.confirmDialog().onConfirm(
        () => {
          this.active.set(tab);
          this.focusActiveTab();
        },
        'Changement de type de vente',
        'Vous avez une vente en cours. Voulez-vous vraiment changer de type de vente ?',
      );
    } else {
      this.active.set(tab);
      this.focusActiveTab();
    }
  }
}
