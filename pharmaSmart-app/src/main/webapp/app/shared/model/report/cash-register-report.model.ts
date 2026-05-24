export interface IPaymentModeBreakdown {
  modePaiement?: string;
  amount?: number;
  count?: number;
}

export class PaymentModeBreakdown implements IPaymentModeBreakdown {
  constructor(
    public modePaiement?: string,
    public amount?: number,
    public count?: number,
  ) {}
}

export interface IDailyCashRegisterReport {
  cashRegisterId?: number;
  caisseLibelle?: string;
  date?: string;
  openingDate?: string;
  closingDate?: string;
  openingBalance?: number;
  closingBalance?: number;
  expectedBalance?: number;
  discrepancy?: number;
  totalSales?: number;
  numberOfTransactions?: number;
  paymentModeBreakdowns?: IPaymentModeBreakdown[];
  userName?: string;
  isClosed?: boolean;
}

export class DailyCashRegisterReport implements IDailyCashRegisterReport {
  constructor(
    public cashRegisterId?: number,
    public caisseLibelle?: string,
    public date?: string,
    public openingDate?: string,
    public closingDate?: string,
    public openingBalance?: number,
    public closingBalance?: number,
    public expectedBalance?: number,
    public discrepancy?: number,
    public totalSales?: number,
    public numberOfTransactions?: number,
    public paymentModeBreakdowns?: IPaymentModeBreakdown[],
    public userName?: string,
    public isClosed?: boolean,
  ) {
    this.isClosed = this.isClosed ?? false;
  }
}

export interface ICashMovement {
  id?: number;
  transactionDate?: string;
  cashRegisterName?: string;
  userName?: string;
  movementType?: string;
  amount?: number;
  paymentMode?: string;
  saleNumber?: string;
  customerName?: string;
}

export class CashMovement implements ICashMovement {
  constructor(
    public id?: number,
    public transactionDate?: string,
    public cashRegisterName?: string,
    public userName?: string,
    public movementType?: string,
    public amount?: number,
    public paymentMode?: string,
    public saleNumber?: string,
    public customerName?: string,
  ) {}
}
