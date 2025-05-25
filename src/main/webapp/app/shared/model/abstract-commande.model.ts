import {Moment} from "moment/moment";
import {OrderStatut} from "./enumerations/order-statut.model";
import {IPaymentFournisseur} from "./payment-fournisseur.model";
import {IMagasin} from "./magasin.model";
import {IUser} from "../../core/user/user.model";
import {IFournisseur} from "./fournisseur.model";
import {AbstractOrderItem} from "./abstract-order-item.model";

export interface AbstractCommande {
  id?: number;
  fournisseurId?: number;
  orderRefernce?: string;
  receiptDate?: string;
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
  orderLines?: AbstractOrderItem[];
  magasin?: IMagasin;
  user?: IUser;
  fournisseur?: IFournisseur;
  numberTransaction?: string;
  receiptRefernce?: string;
  fournisseurLibelle?: string;
  orderReference?: string;
  itemSize?: number;
  statut?: string;
}
