import { Moment } from 'moment';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { ICustomer } from 'app/shared/model/customer.model';
import { SalesStatut } from 'app/shared/model/enumerations/sales-statut.model';
import { Payment } from './payment.model';

export interface ISales {
  id?: number;
  discountAmount?: number;
  salesAmount?: number;
  grossAmount?: number;
  netAmount?: number;
  taxAmount?: number;
  costAmount?: number;
  statut?: SalesStatut;
  createdAt?: Moment;
  updatedAt?: Moment;
  salesLines?: ISalesLine[];
  payments?: Payment[];
  customer?: ICustomer;
  customerId?: number;
  numberTransaction?: string;
}

export class Sales implements ISales {
  constructor(
    public id?: number,
    public discountAmount?: number,
    public salesAmount?: number,
    public grossAmount?: number,
    public netAmount?: number,
    public taxAmount?: number,
    public costAmount?: number,
    public statut?: SalesStatut,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public salesLines?: ISalesLine[],
    public payments?: Payment[],
    public customer?: ICustomer,
    public numberTransaction?: string,
    public customerId?: number
  ) {
    this.statut = this.statut || SalesStatut.PENDING;
  }
}
