import { Moment } from 'moment';
import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { OrderStatut } from 'app/shared/model/enumerations/order-statut.model';
import { IMagasin } from 'app/shared/model/magasin.model';
import { IUser } from 'app/core/user/user.model';
import { IFournisseur } from 'app/shared/model/fournisseur.model';
import { AbstractCommande } from './abstract-commande.model';
import { AbstractOrderItem } from './abstract-order-item.model';

export interface ICommande extends AbstractCommande {
  orderReference?: string;
}

export class Commande implements ICommande {
  constructor(
    public id?: number,
    public fournisseurId?: number,
    public receiptDate?: string,
    public discountAmount?: number,
    public orderAmount?: number,
    public grossAmount?: number,
    public netAmount?: number,
    public taxAmount?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public orderStatus?: OrderStatut,
    public paymentFournisseurs?: IPaymentFournisseur[],
    public orderLines?: AbstractOrderItem[],
    public magasin?: IMagasin,
    public user?: IUser,
    public lastUserEdit?: IUser,
    public fournisseur?: IFournisseur,
    public itemSize?: number,
    public receiptAmount?: number,
  ) {}
}
