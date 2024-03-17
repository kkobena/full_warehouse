import { Moment } from 'moment';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { ICustomer } from 'app/shared/model/customer.model';
import { SalesStatut } from 'app/shared/model/enumerations/sales-statut.model';
import { Payment } from './payment.model';
import { IUser } from '../../core/user/user.model';
import { IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';
import { IThirdPartySaleLine } from 'app/shared/model/third-party-sale-line';

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
  natureVente?: string;
  typePrescription?: string;
  categorieVente?: string;
  paymentStatus?: string;
  seller?: IUser;
  user?: IUser;
  cassier?: IUser;
  cassierId?: number;
  sellerId?: number;
  type?: string;
  amountToBePaid?: number;
  categorie?: string;
  montantVerse?: number;
  montantRendu?: number;
  restToPay?: number;
  payrollAmount?: number;
  differe?: boolean;
  caisseEndNum?: string;
  caisseNum?: string;
  ayantDroitId?: number;
  tiersPayants?: IClientTiersPayant[];
  thirdPartySaleLines?: IThirdPartySaleLine[];
  partTiersPayant?: number;
  sansBon?: boolean;
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
    public customerId?: number,
    public cassierId?: number,
    public sellerId?: number,
    public natureVente?: string,
    public typePrescription?: string,
    public categorieVente?: string,
    public paymentStatus?: string,
    public seller?: IUser,
    public user?: IUser,
    public cassier?: IUser,
    public type?: string,
    public amountToBePaid?: number,
    public categorie?: string,
    public montantVerse?: number,
    public montantRendu?: number,
    public restToPay?: number,
    public payrollAmount?: number,
    public differe?: boolean,
    public caisseEndNum?: string,
    public caisseNum?: string,
    public ayantDroitId?: number,
    public tiersPayants?: IClientTiersPayant[],
    public thirdPartySaleLines?: IThirdPartySaleLine[],
    public partTiersPayant?: number,
    public sansBon?: boolean,
  ) {
    this.statut = this.statut || SalesStatut.ACTIVE;
    this.differe = this.differe || false;
    this.sansBon = this.sansBon || false;
  }
}

export class QuantitySaleLine {
  newValue: number;
  saleLine: ISalesLine;
}

export class SaveResponse {
  success: boolean;
  sale?: ISales;
  error?: any;
}

export class InputToFocus {
  control: string;
}
