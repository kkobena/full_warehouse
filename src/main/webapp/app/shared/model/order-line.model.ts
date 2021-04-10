import { Moment } from 'moment';
import { ICommande } from 'app/shared/model/commande.model';
import { IProduit } from 'app/shared/model/produit.model';

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
    public produit?: IProduit
  ) {}
}
