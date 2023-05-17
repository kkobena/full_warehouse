import { Moment } from 'moment';
import { IDeliveryItem } from './delivery-item';

export interface IDelivery {
  id?: number;
  numberTransaction?: string;
  sequenceBon?: string;
  receiptRefernce?: string;
  receiptDate?: Moment;
  receiptFullDate?: Moment;
  discountAmount?: number;
  receiptAmount?: number;
  createdDate?: Moment;
  modifiedDate?: Moment;
  createdUser?: string;
  modifiedUser?: string;
  fournisseurId?: number;
  fournisseurLibelle?: string;
  orderReference?: string;
  netAmount?: number;
  taxAmount?: number;
  itemSize?: number;
  receiptItems?: IDeliveryItem[];
  statut?: string;
}

export class Delivery implements IDelivery {
  constructor(
    public id?: number,
    public numberTransaction?: string,
    public sequenceBon?: string,
    public receiptRefernce?: string,
    public receiptDate?: Moment,
    public discountAmount?: number,
    public receiptAmount?: number,
    public createdDate?: Moment,
    public modifiedDate?: Moment,
    public createdUser?: string,
    public modifiedUser?: string,
    public fournisseurId?: number,
    public fournisseurLibelle?: string,
    public netAmount?: number,
    public taxAmount?: number,
    public itemSize?: number,
    public receiptItems?: IDeliveryItem[],
    public orderReference?: string,
    public receiptFullDate?: Moment,
    public statut?: string
  ) {}
}
