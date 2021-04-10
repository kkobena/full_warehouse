import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { IPayment } from 'app/shared/model/payment.model';
import { PaymentGroup } from 'app/shared/model/enumerations/payment-group.model';

export interface IPaymentMode {
  id?: number;
  libelle?: string;
  code?: string;
  group?: PaymentGroup;
  paymentFournisseurs?: IPaymentFournisseur[];
  payments?: IPayment[];
}

export class PaymentMode implements IPaymentMode {
  constructor(
    public id?: number,
    public libelle?: string,
    public code?: string,
    public group?: PaymentGroup,
    public paymentFournisseurs?: IPaymentFournisseur[],
    public payments?: IPayment[]
  ) {}
}
