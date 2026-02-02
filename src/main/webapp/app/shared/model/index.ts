/**
 * Barrel export for shared models
 * Central export point for all domain models
 */

// Sales & Customer
export type { ISales } from './sales.model';
export type { ISalesLine } from './sales-line.model';
export type { IClientTiersPayant } from './client-tiers-payant.model';
export type { ICustomer } from './customer.model';

// Payment & Financial
export type { IPayment } from './payment.model';
export type { IRemise } from './remise.model';
export type { IMagasin } from './magasin.model';
export type { ITiersPayant } from './tierspayant.model';

// Products
export { ProduitSearch } from './produit.model';
export type { IProduit } from './produit.model';
export type { IFournisseurProduit } from './fournisseur-produit.model';

// Inventory & Stock
export type { IStoreInventory } from './store-inventory.model';
export type { IStoreInventoryLine } from './store-inventory-line.model';

// Enums
export { SalesStatut } from './enumerations/sales-statut.model';
