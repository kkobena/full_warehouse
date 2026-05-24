import { TypeFinancialTransaction } from '../cash-register/model/cash-register.model';
import { IPaymentMode } from '../../shared/model/payment-mode.model';
import { IUser } from '../../core/user/user.model';

export const getTypeName = (type: TypeFinancialTransaction): string => {
  switch (type) {
    case TypeFinancialTransaction.ENTREE_CAISSE:
      return 'ENTREE_CAISSE';
    case TypeFinancialTransaction.SORTIE_CAISSE:
      return 'SORTIE_CAISSE';
    case TypeFinancialTransaction.REGLEMENT_DIFFERE:
      return 'REGLEMENT_DIFFERE';
    case TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT:
      return 'REGLEMENT_TIERS_PAYANT';
    case TypeFinancialTransaction.REGLMENT_FOURNISSEUR:
      return 'REGLMENT_FOURNISSEUR';
    case TypeFinancialTransaction.FONDS_CAISSE:
      return 'FONDS_CAISSE';
    case TypeFinancialTransaction.CREDIT_SALE:
      return 'CREDIT_SALE';
    case TypeFinancialTransaction.CASH_SALE:
      return 'CASH_SALE';
    default:
      return '';
  }
};

export const getTypeVentes = (type: TypeFinancialTransaction): string[] => {
  switch (type) {
    case TypeFinancialTransaction.CREDIT_SALE:
      return ['CREDIT_SALE', 'VENTES_DEPOT_AGREE'];
    case TypeFinancialTransaction.CASH_SALE:
      return ['CASH_SALE'];
    default:
      return null;
  }
};

export class MvtCaisseParams {
  fromDate?: Date;
  toDate?: Date;
  type?: TypeFinancialTransaction;
  groupBy?: string;
  groupByTva?: string;
  selectedVente?: TypeFinancialTransaction;
  selectedTypes?: TypeFinancialTransaction[];
  paymentModes?: IPaymentMode[];
  selectedUser?: IUser;
  search?: string;
}
