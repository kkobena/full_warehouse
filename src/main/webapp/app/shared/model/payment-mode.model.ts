import { IPaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { IPayment } from 'app/shared/model/payment.model';
import { PaymentGroup } from 'app/shared/model/enumerations/payment-group.model';

export interface IPaymentMode {
  libelle?: string;
  code?: string;
  group?: PaymentGroup;
  paymentFournisseurs?: IPaymentFournisseur[];
  payments?: IPayment[];
  enable?: boolean;
  disabled?: boolean;
  order?: number;
  amount?: number;
  styleImageClass?: string;
  styleBtnClass?: string;
  isReadonly?: boolean;
}

export class PaymentMode implements IPaymentMode {
  constructor(
    public libelle?: string,
    public code?: string,
    public group?: PaymentGroup,
    public paymentFournisseurs?: IPaymentFournisseur[],
    public payments?: IPayment[],
    public enable?: boolean,
    public disabled?: boolean,
    public order?: number,
    public amount?: number,
    public styleImageClass?: string,
    public styleBtnClass?: string,
    public isReadonly?: boolean
  ) {}
}
