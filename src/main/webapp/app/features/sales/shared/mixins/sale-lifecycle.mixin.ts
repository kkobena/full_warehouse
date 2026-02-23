import { DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { ProductHandling } from './product-handling.mixin';

/**
 * Contexte pour le mixin de cycle de vie des ventes
 */
export interface SaleLifecycleContext {
  facade: SalesFacade;
  destroyRef: DestroyRef;
  productHandling: ProductHandling;
  resetForNewSale: () => void;
  /** Extra callback after product added (e.g., emit output event) */
  onProductAddedExtra?: () => void;
  /**
   * Handler for resumePendingSaleSuccess$.
   * - undefined → subscribe with default behavior (focusProductSearch)
   * - false → don't subscribe
   * - () => void → subscribe with custom handler
   */
  onResumePendingSale?: (() => void) | false;
}

/**
 * Mixin pour les subscriptions communes du cycle de vie des ventes
 *
 * Factorise les 7-8 souscriptions `facade.*Success$` dupliquées dans
 * sale-creation, sale-carnet, sale-assurance et sale-devis.
 *
 * @example
 * ```typescript
 * private lifecycle = createSaleLifecycle({
 *   facade: this.facade,
 *   destroyRef: this.destroyRef,
 *   productHandling: this.productHandling,
 *   resetForNewSale: () => this.resetForNewSale(),
 * });
 *
 * ngOnInit(): void {
 *   this.lifecycle.initializeSubscriptions();
 *   // ... component-specific init
 * }
 * ```
 */
export function createSaleLifecycle(context: SaleLifecycleContext) {
  const { facade, destroyRef, productHandling, resetForNewSale } = context;

  /**
   * Initialise toutes les souscriptions communes du cycle de vie
   * Doit être appelé dans ngOnInit()
   */
  function initializeSubscriptions(): void {
    facade.standbySuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
      resetForNewSale();
    });

    facade.productAddedSuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
      productHandling.updatePendingDisplay();
      productHandling.resetProductSelection();
      context.onProductAddedExtra?.();
    });

    facade.lineUpdatedSuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
      productHandling.focusProductSearch();
    });

    facade.lineRemovedSuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
      productHandling.focusProductSearch();
    });

    facade.remiseUpdatedSuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
      productHandling.focusProductSearch();
    });

    facade.saleReloadedSuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
      productHandling.resetProductSelection();
    });

    facade.cancelSaleSuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
      resetForNewSale();
    });

    if (context.onResumePendingSale !== false) {
      facade.resumePendingSaleSuccess$.pipe(takeUntilDestroyed(destroyRef)).subscribe(() => {
        if (context.onResumePendingSale) {
          context.onResumePendingSale();
        } else {
          productHandling.focusProductSearch();
        }
      });
    }
  }

  return {
    initializeSubscriptions,
  };
}

/**
 * Type retourné par createSaleLifecycle
 */
export type SaleLifecycle = ReturnType<typeof createSaleLifecycle>;
