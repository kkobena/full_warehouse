import { Moment } from 'moment';
import { ICommande } from 'app/shared/model/commande.model';
import { IPaymentMode } from 'app/shared/model/payment-mode.model';

export interface IPaymentFournisseur {
  id?: number;
  netAmount?: number;
  paidAmount?: number;
  restToPay?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  commande?: ICommande;
  paymentMode?: IPaymentMode;
}

export class PaymentFournisseur implements IPaymentFournisseur {
  constructor(
    public id?: number,
    public netAmount?: number,
    public paidAmount?: number,
    public restToPay?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public commande?: ICommande,
    public paymentMode?: IPaymentMode
  ) {}
}
