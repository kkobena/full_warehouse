import { TypeFinancialTransaction } from '../cash-register/model/cash-register.model';

export const getTypeName = (type: TypeFinancialTransaction): string => {
  console.log('TypeFinancialTransaction', type);
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
