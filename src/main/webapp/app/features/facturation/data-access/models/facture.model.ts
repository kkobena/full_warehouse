import { IUser } from '../../../../core/user/user.model';
import { Customer } from '../../../../shared/model/customer.model';
import { SaleId } from '../../../../shared/model/sales.model';

export interface IFactureId {
  id: number;
  invoiceDate: string;
}

export interface IFneResponse {
  reference: string;
  token: string;
}

export interface IFactureItem {
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
  matricule?: string;
  numeroAssurance?: string;
}

export interface IFacture {
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
  enRetard?: boolean;
  dateEcheance?: Date;
  createdBy?: string;
  lastModifiedBy?: string;
  items?: IFactureItem[];
  factures?: IFacture[];
  factureItemId?: IFactureId;
  fneResponse?: IFneResponse;
}

export interface IDossierFacture {
  id?: number;
  assuredCustomer?: IUser;
  createdAt: Date;
  numBon: string;
  montantVente?: number;
  montantBon?: number;
}

export interface ITiersPayantDossierFacture {
  id: number;
  name: string;
  totalAmount: number;
  factureItemCount: number;
}

export interface IFactureEditionResponse {
  generationCode?: number;
  isGroup?: boolean;
}

export interface IEditionSearchParams {
  modeEdition?: string;
  startDate: string;
  endDate: string;
  groupIds?: number[];
  tiersPayantIds?: number[];
  ids?: number[];
  all?: boolean;
  categorieTiersPayants?: string[];
  factureProvisoire?: boolean;
}

export interface IInvoiceSearchParams {
  search?: string;
  startDate: string;
  endDate: string;
  groupIds?: number[];
  tiersPayantIds?: number[];
  statuts?: string[];
  factureProvisoire?: boolean;
  factureGroupees?: boolean;
}

export interface IFacturationKpi {
  totalFacture: number;
  totalRegle: number;
  totalRestant: number;
  tauxRecouvrement: number;
  countFactures: number;
  countImpayees: number;
  countEnRetard: number;
}
