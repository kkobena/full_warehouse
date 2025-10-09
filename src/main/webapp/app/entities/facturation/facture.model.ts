import { Customer } from '../../shared/model/customer.model';
import { SaleId } from '../../shared/model/sales.model';

export class Facture {
  numFacture?: string;
  tiersPayantName?: string;
  name?: string;
  factureId?: number;
  groupeFactureId?: number;
  groupeNumFacture?: string;
  montantRegle?: number;
  montant?: number;
  remiseForfetaire?: number;
  montantVente?: number;
  montantRemiseVente?: number;
  montantNetVente?: number;
  montantNet?: number;
  created?: Date;
  itemsCount?: number;
  itemsBonCount?: number;
  montantAttendu?: number;
  itemMontantRegle?: number;
  montantRestant?: number;
  invoiceTotalAmount?: number;
  debutPeriode?: Date;
  finPeriode?: Date;
  factureProvisoire?: boolean;
  periode?: string;
  statut?: string;
  items?: FactureItem[];
  factures?: Facture[];
  factureItemId?: FactureId;
}

export class FactureItem {
  saleId?: number;
  numBon?: string;
  saleNumber?: string;
  montant?: number;
  montantClient?: number;
  montantRemise?: number;
  montantVente?: number;
  created?: Date;
  updated?: Date;
  statut?: string;
  taux?: number;
  montantRegle?: number;
  customer?: Customer;
  ayantsDroit?: Customer;
  comppsiteSaleId?: SaleId;
}
export class FactureId {
  id: number;
  invoiceDate: string;
}
