import { OrderLineId } from './abstract-order-item.model';

export interface ILot {
  id?: number;
  receiptItemId?: OrderLineId;
  quantity?: number;
  numLot?: string;
  receiptReference?: string;
  createdDate?: string;
  manufacturingDate?: string;
  expiryDate?: string;
  ugQuantityReceived?: number;
  quantityReceived?: number;
  linkedId?: number;
  freeQuantity?: number;
  freeQty?: number;
  /** Utilisé pour la saisie de lot hors commande (sans OrderLine). */
  produitId?: number;
  /** Storage cible (optionnel) — si absent, storage principal de l'utilisateur connecté. */
  storageId?: number;
}

export class Lot implements ILot {
  constructor(
    public id?: number,
    public receiptItemQuantityRequested?: number,
    public receiptItemId?: OrderLineId,
    public quantity?: number,
    public numLot?: string,
    public receiptReference?: string,
    public createdDate?: string,
    public manufacturingDate?: string,
    public expiryDate?: string,
    public ugQuantityReceived?: number,
    public quantityReceived?: number,
    public produitId?: number,
  ) {}
}
