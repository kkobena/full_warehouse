import { Component, OnInit, AfterViewInit, inject, signal, viewChild, effect, HostListener, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { Toast } from 'primeng/toast';
import { Drawer } from 'primeng/drawer';
import { NgbNav, NgbNavChangeEvent, NgbNavItem, NgbNavLink, NgbNavContent, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { Select } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { SaleCreationComponent } from '../sale-creation/sale-creation.component';
import { SaleAssuranceComponent } from '../sale-assurance/sale-assurance.component';
import { SaleCarnetComponent } from '../sale-carnet/sale-carnet.component';
import { PendingSalesListComponent } from '../../ui';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { UserVendeurService } from '../../../../entities/sales/service/user-vendeur.service';
import { IUser } from '../../../../core/user/user.model';
import { CustomerOverlayPanelComponent } from '../../../../entities/sales/customer-overlay-panel/customer-overlay-panel.component';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../../shared/toast-alert/toast-alert.component';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { MagasinService } from '../../../../entities/magasin/magasin.service';
import { AccountService } from '../../../../core/auth/account.service';
import { CashRegisterService } from '../../../../entities/cash-register/cash-register.service';

@Component({
  selector: 'app-sales-home',
  templateUrl: './sales-home.component.html',
  styleUrls: ['./sales-home.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TooltipModule,
    Toast,
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
  providers: [MessageService], // Nécessaire pour NotificationService utilisé par SalesFacade
})
export class SalesHomeComponent implements OnInit, AfterViewInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  protected salesFacade = inject(SalesFacade);
  protected userVendeurService = inject(UserVendeurService); // Pour liste vendeurs uniquement
  private customerDisplayService = inject(CustomerDisplayService);
  private magasinService = inject(MagasinService);
  private accountService = inject(AccountService);
  private cashRegisterService = inject(CashRegisterService);
  private destroyRef = inject(DestroyRef);
  protected confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  protected alert = viewChild.required<ToastAlertComponent>('alert');
  // Références aux composants enfants (tabs) pour déléguer l'ajout de produits
  protected saleCreation = viewChild<SaleCreationComponent>(SaleCreationComponent);
  protected saleAssurance = viewChild<SaleAssuranceComponent>(SaleAssuranceComponent);
  protected saleCarnet = viewChild<SaleCarnetComponent>(SaleCarnetComponent);
  protected active = signal('comptant');
  protected sidebarCollapsed = signal(false);
  protected isPresale = signal(false);
  protected userSeller = signal<IUser | null>(null);
  protected appendTo = 'body'; // Utilisé dans p-select du template
  protected produitSelected: any | null = null;
  protected isScannedProduct = signal(false);
  protected showStock = true;
  protected disableButton = true;
  protected PRODUIT_COMBO_RESULT_SIZE = 10;
  protected pendingSalesSidebar = signal(false);
  protected countPendingSales = signal('0');

  // Responsive state - passé aux composants enfants
  protected isSmallScreen = signal(false);
  protected isCashRegisterOpen = signal(false);

  constructor() {
    // Auto-disable button when no product selected
    effect(() => {
      this.disableButton = !this.produitSelected;
    });

    // Update pending sales count from store
    effect(() => {
      const pendingSales = this.salesFacade.pendingSales();
      this.countPendingSales.set(pendingSales.length.toString());
    });
  }

  private checkScreenSize(): void {
    this.isSmallScreen.set(window.innerWidth < 1800);
  }

  ngOnInit(): void {
    this.checkScreenSize();
    
    // Vérifier si une caisse est ouverte
    this.hasCashRegisterOpen();
    
    this.route.params.subscribe(params => {
      this.isPresale.set(params['isPresale'] === 'true');

      // Si mode édition (route /sales/:id/:saleDate/:isPresale/edit)
      const saleId = params['id'];
      const saleDate = params['saleDate'];
      const isEdit = this.route.snapshot.data['isEdit'];

      if (isEdit && saleId && saleDate) {
        this.loadSaleForEdit({ id: +saleId, saleDate });
      }
    });

    // Initialiser le caissier (utilisateur connecté)
    const currentUser = this.accountService.trackCurrentAccount()();
    if (currentUser) {
      this.salesFacade.setCashier(currentUser as IUser);
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

    // Check initial screen size
  }
  ngAfterViewInit(): void {}
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
    this.cashRegisterService
      .getConnectedUserHasOpenCashRegister()
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
    this.router.navigate(['/']);
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

  protected onCustomerOverlay(closed: boolean): void {
    // Géré par le composant customer-overlay-panel
  }

  /**
   * Charge une vente pour édition (vente clôturée ASSURANCE/CARNET)
   * Conforme à l'ancien: selling-home.component.ts onLoadPrevente()
   */
  private loadSaleForEdit(saleId: { id: number; saleDate: string }): void {
    // Appel du rxMethod - il met à jour le store automatiquement
    this.salesFacade.loadSaleForEdit(saleId);

    // Écouter les changements du store pour basculer l'onglet
    // (après que la vente soit chargée)
    effect(() => {
      const sale = this.salesFacade.currentSale();
      const isLoading = this.salesFacade.loading();
      const error = this.salesFacade.error();

      // Si erreur de chargement
      if (error && !isLoading) {
        console.error('Erreur lors du chargement de la vente:', error);
        this.alert().showError('Impossible de charger la vente pour édition');
        this.router.navigate(['/sales']);
        return;
      }

      // Si vente chargée avec succès
      if (sale && sale.saleId && !isLoading) {
        // Basculer vers l'onglet approprié selon le type de vente
        if (sale.natureVente === 'ASSURANCE') {
          this.active.set('assurance');
        } else if (sale.natureVente === 'CARNET') {
          this.active.set('carnet');
        }

        // Charger le vendeur si présent et pas encore défini
        if (sale.sellerId && !this.userSeller()) {
          const seller = this.userVendeurService.vendeurs().find(u => u.id === sale.sellerId);
          if (seller) {
            this.userSeller.set(seller);
          }
        }
      }
    });
  }
}
