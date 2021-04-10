import { Moment } from 'moment';
import { IPaymentMode } from 'app/shared/model/payment-mode.model';
export interface IPayment {
  id?: number;
  netAmount?: number;
  paidAmount?: number;
  restToPay?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  paymentMode?: IPaymentMode;
}

export class Payment implements IPayment {
  constructor(
    public id?: number,
    public netAmount?: number,
    public paidAmount?: number,
    public restToPay?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public paymentMode?: IPaymentMode
  ) {}
}
