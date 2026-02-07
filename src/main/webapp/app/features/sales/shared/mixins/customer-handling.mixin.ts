import { Signal, WritableSignal } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
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
  customerSearchService: CustomerSearchService;
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
  const { facade, customerSearchService, notificationService, config, customers } = context;

  /**
   * Recherche des clients par terme de recherche
   */
  function searchCustomers(searchTerm: string, minLength = 2, limit = 10): void {
    if (searchTerm && searchTerm.length >= minLength) {
      customerSearchService.search(searchTerm, limit).subscribe({
        next: (results: ICustomer[]) => {
          customers.set(results);
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
   * Sélectionne un client pour la vente
   */
  function selectCustomer(customer: ICustomer): void {
    facade.setCustomer(customer);
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
  }): NgbModalRef | null {
    if (!context.customerListComponent) {
      notificationService.error('Erreur', 'Composant de liste client non configuré');
      return null;
    }

    const modalRef = context.modalService.open(context.customerListComponent as unknown, {
      size: options?.size || 'lg',
      backdrop: options?.backdrop ?? 'static',
      centered: true,
    });

    if (options?.title && modalRef.componentInstance) {
      modalRef.componentInstance.title = options.title;
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
    });

    if (modalRef.componentInstance) {
      modalRef.componentInstance.entity = customer;
      if (options?.title) {
        modalRef.componentInstance.title = options.title;
      }
    }

    modalRef.result.then(
      (savedCustomer: ICustomer) => {
        if (savedCustomer?.id) {
          selectCustomer(savedCustomer);
          notificationService.success('Succès', customer ? 'Client modifié' : 'Client créé');
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
