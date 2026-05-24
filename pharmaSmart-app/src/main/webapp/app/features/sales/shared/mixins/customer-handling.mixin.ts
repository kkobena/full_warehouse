import { Signal, WritableSignal } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { ICustomer } from '../../../../shared/model';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { CustomerSearchService } from '../../data-access/services/customer-search.service';
import { NotificationService } from '../../../../shared/services/notification.service';

/**
 * Configuration pour le mixin de gestion des clients
 */
export interface CustomerHandlingConfig {
  saleType: 'COMPTANT' | 'CARNET' | 'ASSURANCE';
  /** Le client est-il obligatoire pour ce type de vente */
  customerRequired: boolean;
  /** Message affiché si client manquant */
  customerRequiredMessage?: string;
}

/**
 * Interface pour les composants utilisant la recherche client
 */
export interface CustomerSearchHost {
  searchInput?(): { nativeElement: HTMLElement } | undefined;
}

/**
 * Contexte partagé pour les opérations de gestion client
 */
export interface CustomerHandlingContext {
  facade: SalesFacade;
  /** Requis si searchFn n'est pas fourni */
  customerSearchService?: CustomerSearchService;
  notificationService: NotificationService;
  modalService: NgbModal;
  config: CustomerHandlingConfig;
  // Signals
  selectedCustomer: Signal<ICustomer | null>;
  customers: WritableSignal<ICustomer[]>;
  // Host optionnel pour focus
  host?: CustomerSearchHost;
  // Callback après sélection client
  onCustomerSelectedCallback?: (customer: ICustomer) => void;
  // Composants modaux spécifiques au type de vente
  customerListComponent?: unknown;
  customerFormComponent?: unknown;
  /** Fonction custom pour sélectionner le client (par défaut: facade.setCustomer) */
  selectCustomerFn?: (customer: ICustomer) => void;
  /** Fonction custom de recherche (par défaut: customerSearchService.search) */
  searchFn?: (term: string, limit: number) => Observable<ICustomer[]>;
  /** Auto-sélectionner si 1 seul résultat, ouvrir form si 0 résultat, ouvrir liste si N résultats */
  smartSearch?: boolean;
}

/**
 * Mixin pour la gestion des clients dans les composants de vente
 *
 * Fournit les méthodes communes pour :
 * - Recherche de clients
 * - Sélection de client
 * - Création de client
 * - Validation client obligatoire
 *
 * @example
 * ```typescript
 * // Dans le composant
 * private customerHandling = createCustomerHandling({
 *   facade: this.facade,
 *   customerSearchService: this.customerSearchService,
 *   notificationService: this.notificationService,
 *   modalService: this.modalService,
 *   config: { saleType: 'CARNET', customerRequired: true },
 *   selectedCustomer: this.facade.selectedCustomer,
 *   customers: this.customers,
 * });
 *
 * // Utilisation
 * onCustomerSearchChange(searchTerm: string): void {
 *   this.customerHandling.searchCustomers(searchTerm);
 * }
 * ```
 */
export function createCustomerHandling(context: CustomerHandlingContext) {
  const { facade, notificationService, config, customers } = context;

  /**
   * Recherche des clients par terme de recherche
   */
  function searchCustomers(searchTerm: string, minLength = 2, limit = 10): void {
    if (searchTerm && searchTerm.length >= minLength) {
      const search$ = context.searchFn
        ? context.searchFn(searchTerm, limit)
        : context.customerSearchService!.search(searchTerm, limit);

      search$.subscribe({
        next: (results: ICustomer[]) => {
          if (context.smartSearch) {
            handleSmartSearchResults(results);
          } else {
            customers.set(results);
          }
        },
        error: () => {
          notificationService.error('Erreur', 'Erreur lors de la recherche client');
          customers.set([]);
        },
      });
    } else {
      customers.set([]);
    }
  }

  /**
   * Gère les résultats de recherche en mode smartSearch :
   * - 1 résultat → auto-sélection
   * - 0 résultats → ouverture formulaire création
   * - N résultats → ouverture modal liste avec résultats préchargés
   */
  function handleSmartSearchResults(results: ICustomer[]): void {
    if (results.length === 1) {
      selectCustomer(results[0]);
    } else if (results.length === 0) {
      openCustomerFormModal(null);
    } else {
      customers.set(results);
      openCustomerListModal({ componentInputs: { customers: results } });
    }
  }

  /**
   * Sélectionne un client pour la vente
   */
  function selectCustomer(customer: ICustomer): void {
    if (context.selectCustomerFn) {
      context.selectCustomerFn(customer);
    } else {
      facade.setCustomer(customer);
    }
    customers.set([]); // Clear search results
    context.onCustomerSelectedCallback?.(customer);
  }

  /**
   * Retire le client de la vente
   */
  function removeCustomer(): void {
    facade.removeCustomer();
    customers.set([]);
  }

  /**
   * Vérifie si un client est sélectionné
   */
  function hasCustomer(): boolean {
    return !!context.selectedCustomer();
  }

  /**
   * Vérifie si le client est requis et présent
   */
  function validateCustomerRequired(): boolean {
    if (!config.customerRequired) {
      return true;
    }

    if (!hasCustomer()) {
      notificationService.warning(
        'Client requis',
        config.customerRequiredMessage || `Un client est obligatoire pour une vente ${config.saleType}`,
      );
      return false;
    }
    return true;
  }

  /**
   * Ouvre le modal de liste de clients
   */
  function openCustomerListModal(options?: {
    title?: string;
    backdrop?: 'static' | boolean;
    size?: 'sm' | 'lg' | 'xl';
    modalDialogClass?: string;
    componentInputs?: Record<string, unknown>;
  }): NgbModalRef | null {
    if (!context.customerListComponent) {
      notificationService.error('Erreur', 'Composant de liste client non configuré');
      return null;
    }

    const modalRef = context.modalService.open(context.customerListComponent as unknown, {
      size: options?.size || 'lg',
      backdrop: options?.backdrop ?? 'static',
      centered: true,
      modalDialogClass: options?.modalDialogClass,
    });

    if (modalRef.componentInstance) {
      if (options?.title) {
        modalRef.componentInstance.title = options.title;
      }
      if (options?.componentInputs) {
        Object.assign(modalRef.componentInstance, options.componentInputs);
      }
    }

    modalRef.result.then(
      (customer: ICustomer) => {
        if (customer?.id) {
          selectCustomer(customer);
        } else if (config.customerRequired) {
          notificationService.warning('Client requis', `Un client est obligatoire pour une vente ${config.saleType}`);
        }
      },
      () => {
        // Modal fermée sans sélection
        if (config.customerRequired && !hasCustomer()) {
          notificationService.info('Information', 'Veuillez sélectionner un client pour continuer');
        }
      },
    );

    return modalRef;
  }

  /**
   * Ouvre le modal de création/édition de client
   */
  function openCustomerFormModal(
    customer: ICustomer | null,
    options?: {
      title?: string;
      backdrop?: 'static' | boolean;
      size?: 'sm' | 'lg' | 'xl';
      modalDialogClass?: string;
      componentInputs?: Record<string, unknown>;
    },
  ): NgbModalRef | null {
    if (!context.customerFormComponent) {
      notificationService.error('Erreur', 'Composant de formulaire client non configuré');
      return null;
    }

    const modalRef = context.modalService.open(context.customerFormComponent as unknown, {
      size: options?.size || 'lg',
      backdrop: options?.backdrop ?? 'static',
      centered: true,
      modalDialogClass: options?.modalDialogClass,
    });

    if (modalRef.componentInstance) {
      modalRef.componentInstance.entity = customer;
      if (options?.title) {
        modalRef.componentInstance.title = options.title;
      }
      if (options?.componentInputs) {
        Object.assign(modalRef.componentInstance, options.componentInputs);
      }
    }

    modalRef.result.then(
      (savedCustomer: ICustomer) => {
        if (savedCustomer?.id) {
          selectCustomer(savedCustomer);
        }
      },
      () => {
        // Modal fermée sans enregistrement
      },
    );

    return modalRef;
  }

  /**
   * Obtient l'affichage formaté du client
   */
  function getCustomerDisplay(customer: ICustomer | null): string {
    if (!customer) return '';
    return `${customer.firstName || ''} ${customer.lastName || ''} ${customer.phone || ''}`.trim();
  }

  /**
   * Vérifie si le client a des tiers payants (pour ASSURANCE)
   */
  function isClientAssure(customer: ICustomer): boolean {
    const tiersPayants = customer.tiersPayants || [];
    return tiersPayants.length > 0;
  }

  /**
   * Vérifie si le client peut faire une vente carnet (doit avoir un tiers payant)
   */
  function isClientCarnet(customer: ICustomer): boolean {
    const tiersPayants = customer.tiersPayants || [];
    return tiersPayants.length > 0;
  }

  return {
    searchCustomers,
    selectCustomer,
    removeCustomer,
    hasCustomer,
    validateCustomerRequired,
    openCustomerListModal,
    openCustomerFormModal,
    getCustomerDisplay,
    isClientAssure,
    isClientCarnet,
  };
}

/**
 * Type retourné par createCustomerHandling
 */
export type CustomerHandling = ReturnType<typeof createCustomerHandling>;
