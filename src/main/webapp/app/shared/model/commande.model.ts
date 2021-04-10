import { Moment } from 'moment';
import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { OrderStatut } from 'app/shared/model/enumerations/order-statut.model';

export interface ICommande {
  id?: number;
  orderRefernce?: string;
  receiptDate?: Moment;
  discountAmount?: number;
  orderAmount?: number;
  grossAmount?: number;
  netAmount?: number;
  taxAmount?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  orderStatus?: OrderStatut;
  paymentFournisseurs?: IPaymentFournisseur[];
  orderLines?: IOrderLine[];
}

export class Commande implements ICommande {
  constructor(
    public id?: number,
    public orderRefernce?: string,
    public receiptDate?: Moment,
    public discountAmount?: number,
    public orderAmount?: number,
    public grossAmount?: number,
    public netAmount?: number,
    public taxAmount?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public orderStatus?: OrderStatut,
    public paymentFournisseurs?: IPaymentFournisseur[],
    public orderLines?: IOrderLine[]
  ) {}
}
