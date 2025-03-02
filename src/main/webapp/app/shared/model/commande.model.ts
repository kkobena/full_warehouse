import { Moment } from 'moment';
import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { OrderStatut } from 'app/shared/model/enumerations/order-statut.model';
import { IMagasin } from 'app/shared/model/magasin.model';
import { IUser } from 'app/core/user/user.model';
import { IFournisseur } from 'app/shared/model/fournisseur.model';

export interface ICommande {
  id?: number;
  fournisseurId?: number;
  orderRefernce?: string;
  receiptDate?: Moment;
  discountAmount?: number;
  orderAmount?: number;
  grossAmount?: number;
  netAmount?: number;
  taxAmount?: number;
  receiptAmount?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  orderStatus?: OrderStatut;
  paymentFournisseurs?: IPaymentFournisseur[];
  orderLines?: IOrderLine[];
  magasin?: IMagasin;
  user?: IUser;
  lastUserEdit?: IUser;
  fournisseur?: IFournisseur;
  receiptRefernce?: string;
  itemSize?: number;
  sequenceBon?: string;
}

export class Commande implements ICommande {
  constructor(
    public id?: number,
    public fournisseurId?: number,
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
    public orderLines?: IOrderLine[],
    public magasin?: IMagasin,
    public user?: IUser,
    public lastUserEdit?: IUser,
    public fournisseur?: IFournisseur,
    public receiptRefernce?: string,
    public itemSize?: number,
    public sequenceBon?: string,
    public receiptAmount?: number,
  ) {}
}
