import { Moment } from 'moment';
import { ICommande } from 'app/shared/model/commande.model';
import { IProduit } from 'app/shared/model/produit.model';
import { ILot } from './lot.model';

export interface IOrderLine {
  id?: number;
  receiptDate?: Moment;
  quantityReceived?: number;
  quantityRequested?: number;
  quantityReturned?: number;
  discountAmount?: number;
  orderAmount?: number;
  grossAmount?: number;
  netAmount?: number;
  taxAmount?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  costAmount?: number;
  commande?: ICommande;
  produit?: IProduit;
  totalQuantity?: number;
  regularUnitPrice?: number;
  orderUnitPrice?: number;
  fournisseurProduitId?: number;
  produitLibelle?: string;
  produitCip?: string;
  produitCodeEan?: string;
  orderCostAmount?: number;
  produitId?: number;
  initStock?: number;
  quantityReceivedTmp?: number;
  quantityUG?: number;
  provisionalCode?: boolean;
  ugQuantity?: number;

  lots?: ILot[];
}

export class OrderLine implements IOrderLine {
  constructor(
    public id?: number,
    public receiptDate?: Moment,
    public quantityReceived?: number,
    public quantityRequested?: number,
    public quantityReturned?: number,
    public discountAmount?: number,
    public orderAmount?: number,
    public grossAmount?: number,
    public netAmount?: number,
    public taxAmount?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public costAmount?: number,
    public commande?: ICommande,
    public produit?: IProduit,
    public totalQuantity?: number,
    public regularUnitPrice?: number,
    public orderUnitPrice?: number,
    public fournisseurProduitId?: number,
    public produitLibelle?: string,
    public produitCip?: string,
    public produitCodeEan?: string,
    public orderCostAmount?: number,
    public produitId?: number,
    public initStock?: number,
    public provisionalCode?: boolean,
    public quantityReceivedTmp?: number,
    public quantityUG?: number,
    public ugQuantity?: number,
    public lots?: ILot[]
  ) {}
}
