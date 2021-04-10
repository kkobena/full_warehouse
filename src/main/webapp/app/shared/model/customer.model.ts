import { Moment } from 'moment';
import { ISales } from 'app/shared/model/sales.model';
import { IProduit } from 'app/shared/model/produit.model';
import { IPayment } from 'app/shared/model/payment.model';

export interface ICustomer {
  id?: number;
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  createdAt?: Moment;
  updatedAt?: Moment;
  produits?: IProduit[];
  sales?: ISales[];
  encours?: number;
  payments?: IPayment[];
  fullName?: string;
}

export class Customer implements ICustomer {
  constructor(
    public id?: number,
    public firstName?: string,
    public lastName?: string,
    public phone?: string,
    public email?: string,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public sales?: ISales[],
    public produits?: IProduit[],
    public payments?: IPayment[],
    public encours?: number,
    public fullName?: string
  ) {}
}
