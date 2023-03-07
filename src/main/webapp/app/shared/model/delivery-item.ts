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

  fournisseurProduitId?: number;
  fournisseurProduitLibelle?: string;
  fournisseurProduitCip?: string;
  fournisseurProduitEan?: string;
  lots?: ILot[];
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
    public fournisseurProduitId?: number,
    public fournisseurProduitLibelle?: string,
    public fournisseurProduitCip?: string,
    public fournisseurProduitEan?: string,
    public lots?: ILot[]
  ) {}
}
