import { IPaymentMode } from 'app/shared/model/payment-mode.model';

export interface IPayment {
  id?: number;
  netAmount?: number;
  paidAmount?: number;
  restToPay?: number;
  createdAt?: string;
  updatedAt?: string;
  paymentMode?: IPaymentMode;
  montantVerse?: number;
}

export class Payment implements IPayment {
  constructor(
    public id?: number,
    public netAmount?: number,
    public paidAmount?: number,
    public restToPay?: number,
    public createdAt?: string,
    public updatedAt?: string,
    public paymentMode?: IPaymentMode,
    public montantVerse?: number,
  ) {}
}
