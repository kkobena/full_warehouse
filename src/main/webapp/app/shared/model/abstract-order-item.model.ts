import { ILot } from './lot.model';
import { ICommande } from './commande.model';
import { IProduit } from './produit.model';
import { CommandeId } from './abstract-commande.model';

export interface AbstractOrderItem {
  id?: number;
  quantityReceived?: number;
  initStock?: number;
  quantityRequested?: number;
  quantityReturned?: number;
  discountAmount?: number;
  netAmount?: number;
  taxAmount?: number;
  orderUnitPrice?: number;
  regularUnitPrice?: number;
  orderCostAmount?: number;
  freeQty?: number;
  afterStock?: number;
  fournisseurProduitId?: number;
  produitId?: number;
  lots?: ILot[];
  updated?: boolean;
  costAmount?: number;
  quantityReceivedTmp?: number;
  provisionalCode?: boolean;
  tva?: number;
  totalQuantity?: number;
  datePeremption?: Date;
  datePeremptionTmp?: string;
  receiptDate?: string;
  orderAmount?: number;
  grossAmount?: number;
  createdAt?: Date;
  updatedAt?: Date;
  commande?: ICommande;
  produit?: IProduit;
  produitLibelle?: string;
  produitCip?: string;
  produitCodeEan?: string;
  orderLineId?: OrderLineId;
  compositeId?: CommandeId;
  orderDate?: string;
}

export class OrderLineId {
  id: number;
  orderDate: string;
}
