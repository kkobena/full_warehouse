import { Component, OnInit, AfterViewInit, inject, signal, viewChild, effect, HostListener } from '@angular/core';
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
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { NotificationService } from '../../../../shared/services/notification.service';
import { UserVendeurService } from '../../../../entities/sales/service/user-vendeur.service';
import { IUser } from '../../../../core/user/user.model';
import { ProduitSearchAutocompleteScannerComponent } from '../../../../shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { CustomerOverlayPanelComponent } from '../../../../entities/sales/customer-overlay-panel/customer-overlay-panel.component';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../../shared/toast-alert/toast-alert.component';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { MagasinService } from '../../../../entities/magasin/magasin.service';

@Component({
  selector: 'app-sales-home',
  templateUrl: './sales-home.component.html',
  styleUrls: ['./sales-home.component.scss'],
  imports: [
    CommonModule, FormsModule, Button, TooltipModule, Toast, Drawer, NgbNav, NgbNavItem, NgbNavLink,
    NgbNavContent, NgbNavOutlet, Select, SaleCreationComponent, SaleAssuranceComponent, SaleCarnetComponent,
    ProduitSearchAutocompleteScannerComponent, QuantiteProdutSaisieComponent,
    CustomerOverlayPanelComponent,
    ConfirmDialogComponent, ToastAlertComponent,
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
  protected produitbox = viewChild<ProduitSearchAutocompleteScannerComponent>('produitbox');
  protected produitQteCmpt = viewChild<QuantiteProdutSaisieComponent>('produitQteCmpt');
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
  protected appendTo = 'body';
  protected produitSelected: any | null = null;
  protected isScannedProduct = signal(false);
  protected showStock = true;
  protected disableButton = true;
  protected PRODUIT_COMBO_RESULT_SIZE = 10;
  protected pendingSalesSidebar = signal(false);
  protected countPendingSales = signal('0');
  
  // Responsive state
  protected isSmallScreen = signal(false);
  
  constructor() { 
    // Auto-disable button when no product selected
    effect(() => { this.disableButton = !this.produitSelected; });
    
    // Update pending sales count from store
    effect(() => {
      const pendingSales = this.salesFacade.pendingSales();
      this.countPendingSales.set(pendingSales.length.toString());
    });
  }
  
  @HostListener('window:resize', ['$event'])
  onResize(event: any): void {
    this.checkScreenSize();
  }
  
  private checkScreenSize(): void {
    this.isSmallScreen.set(window.innerWidth < 768);
  }
  ngOnInit(): void {
    this.route.params.subscribe(params => { this.isPresale.set(params['isPresale'] === 'true'); });
    const currentSeller = this.salesFacade.seller();
    if (currentSeller) { 
      this.userSeller.set(currentSeller);
      // Initialise l'afficheur client avec le nom du magasin
      this.magasinService.findCurrentUserMagasin().then(magasin => {
        const storeName = magasin?.name || 'PHARMA SMART';
        this.customerDisplayService.initialize(storeName, currentSeller);
      }).catch(error => {
        console.error('Error loading store name:', error);
        this.customerDisplayService.initialize('PHARMA SMART', currentSeller);
      });
    }
    
    // Check initial screen size
    this.checkScreenSize();
  }
  ngAfterViewInit(): void { setTimeout(() => { this.produitbox()?.getFocus(); }, 100); }
  protected onNavChange(evt: NgbNavChangeEvent): void {
    const newTab = evt.nextId;
    const currentSale = this.salesFacade.currentSale();
    if (currentSale && currentSale.salesLines && currentSale.salesLines.length > 0) {
      this.confirmDialog().onConfirm(() => { this.active.set(newTab); setTimeout(() => this.produitbox()?.getFocus(), 100); },
        'Changement de type de vente', 'Vous avez une vente en cours. Voulez-vous vraiment changer de type de vente ?');
      evt.preventDefault();
    } else { this.active.set(newTab); setTimeout(() => this.produitbox()?.getFocus(), 100); }
  }
  protected onSelectUser(): void { const seller = this.userSeller(); if (seller) { this.salesFacade.setSeller(seller); } }
  protected toggleSidebar(): void { this.sidebarCollapsed.update(collapsed => !collapsed); }
  protected previousState(): void { this.router.navigate(['/']); }
  protected getPageTitle(): string {
    switch (this.active()) {
      case 'comptant': return 'Vente Comptant';
      case 'assurance': return 'Vente Assurance';
      case 'carnet': return 'Vente Carnet';
      default: return 'Point de Vente';
    }
  }
  protected onSelectProduct(product: any | null): void {
    if (!product) return;
    this.produitSelected = product;
    this.isScannedProduct.set(false);
    // Focus sur champ quantité pour saisie manuelle
    setTimeout(() => { this.produitQteCmpt()?.focusProduitControl(); }, 50);
  }
  protected onScannedProduct(product: any | null): void {
    if (!product) return;
    this.produitSelected = product;
    this.isScannedProduct.set(true);
    // Ajout direct avec quantité 1 (pas de saisie quantité pour scan)
    this.addQuantity(1);
  }
  protected addQuantity(quantity: number): void {
    if (!this.produitSelected || quantity <= 0) return;
    
    // Déléguer l'ajout au composant enfant actif (pattern de l'ancien selling-home.component)
    const product = this.produitSelected;
    
    switch (this.active()) {
      case 'comptant':
        // Le composant SaleCreationComponent gère création de vente si nécessaire
        this.saleCreation()?.onProductSelected(product);
        break;
        
      case 'assurance':
        this.saleAssurance()?.onProductSelected(product);
        break;
        
      case 'carnet':
        this.saleCarnet()?.onProductSelected(product);
        break;
        
      default:
        console.warn(`Unknown sale type: ${this.active()}`);
        return;
    }
    
    // NOTE: Le reset est délégué à onProductAddedSuccess() qui sera appelé
    // par l'événement Output du composant enfant APRÈS le succès de l'ajout.
    // Règle métier: ne pas reset avant confirmation du succès pour ne pas perdre
    // le produit en cas d'erreur d'ajout.
  }
  private resetProductSearch(): void {
    this.produitSelected = null;
    this.isScannedProduct.set(false);
    this.produitbox()?.reset();
    setTimeout(() => { this.produitbox()?.getFocus(); }, 50);
  }
  protected onSaveKeyDown(event: KeyboardEvent): void {
    if (this.produitSelected) { 
      setTimeout(() => { this.produitQteCmpt()?.focusProduitControl(); }, 50); 
    }
  }
  protected onProductAddedSuccess(): void {
    // Appelé par l'événement Output des composants enfants APRÈS le succès de l'ajout
    // Règle métier: reset uniquement après confirmation du succès
    this.resetProductSearch();
  }
  protected openPendingSales(): void { this.pendingSalesSidebar.set(true); }
  protected onCustomerOverlay(closed: boolean): void { if (closed) { setTimeout(() => this.produitbox()?.getFocus(), 100); } }
}
