import { IUser } from '../../../core/user/user.model';
import { Ticketing } from './ticketing.model';
import { IPaymentMode, PaymentMode } from '../../../shared/model/payment-mode.model';
import { TypeCa } from '../../../shared/model/enumerations/type-ca.model';

export class CashRegister {
  id?: number;
  created?: string;
  updated?: string;
  initAmount?: number;
  finalAmount?: number;
  beginTime?: string;
  endTime?: string;
  cashRegisterItems?: CashRegisterItem[];
  ticketing?: Ticketing;
  statut?: CashRegisterStatut;
  user?: IUser;
  updatedUser?: IUser;
  cashFund?: number;
}

export class CashRegisterItem {
  amount?: number;
  paymentMode?: PaymentMode;
  typeTransaction?: TypeFinancialTransaction;
}

export class CashFund {
  amount?: number;
  statut: CashFundStatut;
  cashFundType: CashFundType;
}

export const enum CashRegisterStatut {
  OPEN = 'OPEN',
  CLOSED = 'CLOSED',
  VALIDETED = 'VALIDETED',
  PENDING = 'PENDING',
}

export const enum CashFundStatut {
  PROCESSING,
  VALIDETED,
  PENDING,
}

export const enum CashFundType {
  AUTO,
  MANUAL,
}

export const enum TypeFinancialTransaction {
  CASH_SALE = 'VNO',
  CREDIT_SALE = 'VO',
  VENTES_DEPOTS = 'Ventes dépôts',
  REGLEMENT_DIFFERE = 'Règlements différés',
  REGLEMENT_TIERS_PAYANT = 'Règlements tiers payant',
  SORTIE_CAISSE = 'Sortie de caisse',
  ENTREE_CAISSE = 'Entrée de caisse',
  FONDS_CAISSE = 'Fonds de caisse',
  REGLMENT_FOURNISSEUR = 'Règlement facture fournisseur',
  ALL = 'Tout',
}

export class TransactionFilter {
  fromDate?: string;
  toDate?: string;
  fromTime?: string;
  toTime?: string;
  typeFinancialTransactions?: TypeFinancialTransaction[];
  paymentModes?: string[];
  categorieChiffreAffaires?: TypeCa[];
  userId?: number;
  order?: string;
}

export class MvtCaisse {
  reference?: string;
  date?: string;
  heure?: string;
  transactionType?: string;
  organisme?: string;
  montant?: number;
  order?: string;
  userFullName?: string;
  ticketCode?: string;
  paymentModeLibelle?: string;
  paymentMode?: string;
  id?: number;
  numBon?: string;
  categorieChiffreAffaire?: TypeCa;
  transactionDate?: string;
  netAmount?: number;
  htAmount?: number;
  partAssure?: number;
  partAssureur?: number;
  discount?: number;
}

export class MvtCaisseSum {
  amount?: number;
  paymentModeLibelle?: string;
  paymentModeCode?: string;
  typeTransaction?: TypeFinancialTransaction;
}

export class FinancialTransaction {
  id?: number;
  credit?: boolean;
  organismeId?: number;
  paymentMode?: IPaymentMode;
  transactionDate?: string;
  organismeName?: string;
  commentaire?: string;
  userFullName?: string;
  createdAt?: string;
  ticketCode?: string;
  typeFinancialTransaction?: TypeFinancialTransaction;
  typeTransaction?: string;
  amount?: number;
}
