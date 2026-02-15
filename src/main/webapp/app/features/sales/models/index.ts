/**
 * Barrel export des models Sales
 * Redirige vers les models existants dans shared/model pour compatibilité
 */

// Models principaux
export type {
  ISales,
  FinalyseSale,
  SaveResponse,
  StockError,
  SaleId,
  InputToFocus,
  SaleForEditInfo,
} from '../../../shared/model/sales.model';
export type { ISalesLine } from '../../../shared/model/sales-line.model';
export type { ICustomer } from '../../../shared/model/customer.model';
export type { ProduitSearch } from '../../../shared/model/produit.model';
export type { IPayment } from '../../../shared/model/payment.model';
export type { IRemise, GroupRemise } from '../../../shared/model/remise.model';
export type { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
export type { IThirdPartySaleLine } from '../../../shared/model/third-party-sale-line';
export type { IMagasin } from '../../../shared/model/magasin.model';
export type { IUser } from '../../../core/user/user.model';

// Enums
export { SalesStatut } from '../../../shared/model/enumerations/sales-statut.model';
