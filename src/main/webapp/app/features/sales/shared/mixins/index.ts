/**
 * Mixins pour les composants de vente
 *
 * Ces mixins encapsulent le code dupliqué entre les composants de vente
 * (sale-creation, sale-carnet, sale-assurance).
 *
 * UTILISATION PROGRESSIVE (pas de régression):
 *
 * Ces mixins sont des UTILITAIRES OPTIONNELS. Le code existant dans les
 * composants continue de fonctionner tel quel. Les mixins peuvent être
 * adoptés graduellement :
 *
 * 1. D'abord importer et tester un mixin dans un seul composant
 * 2. Vérifier que les fonctionnalités sont identiques
 * 3. Puis étendre aux autres composants
 *
 * @example
 * // Import dans un composant
 * import {
 *   createProductHandling,
 *   createPaymentHandling,
 *   createForceStockHandling,
 *   createCustomerHandling
 * } from '../../shared/mixins';
 *
 * // Création des handlers dans le composant
 * private productHandling = createProductHandling({ ... });
 * private paymentHandling = createPaymentHandling({ ... });
 * private forceStockHandling = createForceStockHandling({ ... });
 * private customerHandling = createCustomerHandling({ ... });
 *
 * // Utilisation: déléguer les appels existants au mixin
 * onProductSelected(product: ProduitSearch | null): void {
 *   this.productHandling.onProductSelected(product);
 * }
 */

// Product handling mixin
export {
  createProductHandling,
  type ProductHandling,
  type ProductSearchHost,
  type ProductHandlingConfig,
  type ProductHandlingContext,
  type CreateSaleFunction,
  type AddProductFunction,
  type PendingDisplayProduct,
} from './product-handling.mixin';

// Payment handling mixin
export {
  createPaymentHandling,
  type PaymentHandling,
  type PaymentEntry,
  type PaymentModeHost,
  type PaymentHandlingConfig,
  type PaymentHandlingContext,
} from './payment-handling.mixin';

// Force stock handling mixin
export {
  createForceStockHandling,
  type ForceStockHandling,
  type StockErrorDetails,
  type ForceStockContext,
  type ForceStockConfig,
  type ConfirmDialogHost,
  type ForceStockSaleOperations,
  type ForceStockHandlingContext,
} from './force-stock.mixin';

// Customer handling mixin
export {
  createCustomerHandling,
  type CustomerHandling,
  type CustomerHandlingConfig,
  type CustomerHandlingContext,
  type CustomerSearchHost,
} from './customer-handling.mixin';

// Keyboard shortcuts mixin
export {
  createKeyboardShortcuts,
  type KeyboardShortcutsMixin,
  type SaleShortcutCallbacks,
  type KeyboardShortcutsConfig,
} from './keyboard-shortcuts.mixin';
