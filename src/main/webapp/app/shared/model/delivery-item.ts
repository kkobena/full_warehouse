import { ILot } from './lot.model';

export interface IDeliveryItem {
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
  effectifGrossIncome?: number;
  effectifOrderAmount?: number;
  ugQuantity?: number;
  quantityUG?: number;
  afterStock?: number;
  fournisseurProduitId?: number;
  produitId?: number;
  fournisseurProduitLibelle?: string;
  fournisseurProduitCip?: string;
  fournisseurProduitEan?: string;
  lots?: ILot[];
  updated?: boolean;
  costAmount?: number;
  quantityReceivedTmp?: number;
  provisionalCode?: boolean;
  tva?: number;
  datePeremption?: Date;
  datePeremptionTmp?: string;
}

export class DeliveryItem implements IDeliveryItem {
  constructor(
    public id?: number,
    public quantityReceived?: number,
    public initStock?: number,
    public quantityRequested?: number,
    public quantityReturned?: number,
    public discountAmount?: number,
    public netAmount?: number,
    public taxAmount?: number,
    public orderUnitPrice?: number,
    public regularUnitPrice?: number,
    public orderCostAmount?: number,
    public effectifGrossIncome?: number,
    public effectifOrderAmount?: number,
    public ugQuantity?: number,
    public quantityUG?: number,
    public fournisseurProduitId?: number,
    public produitId?: number,
    public fournisseurProduitLibelle?: string,
    public fournisseurProduitCip?: string,
    public fournisseurProduitEan?: string,
    public lots?: ILot[],
    public updated?: boolean,
    public costAmount?: number,
    public quantityReceivedTmp?: number,
    public provisionalCode?: boolean,
  ) {}
}
