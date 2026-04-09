import { IUser } from '../../../../core/user/user.model';
import { Customer } from '../../../../shared/model/customer.model';
import { SaleId } from '../../../../shared/model/sales.model';
import { Reglement } from "../../../../entities/reglement/model/reglement.model";
import { IReglement } from "./reglement.model";

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
  reglements?: IReglement[];
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

// Récapitulatif mensuel
export interface IRecapitulatifMensuelRow {
  numFacture?: string;
  invoiceDate?: string;
  echeance?: string;
  montantNet?: number;
  montantRegle?: number;
  restantDu?: number;
  statut?: string;
}

export interface IRecapitulatifMensuelDto {
  tiersPayantName?: string;
  tiersPayantCode?: string;
  periode?: string;
  soldePrecedent?: number;
  totalFacture?: number;
  tiersPayantId?: number;
  totalRegle?: number;
  soldeActuel?: number;
  soldeCumule?: number;
  nombreFactures?: number;
  nombreImpayees?: number;
  lignes?: IRecapitulatifMensuelRow[];
}

export interface IRecapitulatifKpi {
  totalFacture: number;
  totalRegle: number;
  totalRestant: number;
  soldeCumule: number;
  tauxRecouvrement: number;
  countFactures: number;
  countImpayees: number;
}

export interface IRecapitulatifParams {
  annee?: number;
  mois?: number;
  tiersPayantIds?: number[];
  typeFacture?: string;
}

// Rapprochement
export interface IRapprochementKpi {
  totalFacture: number;
  totalRegle: number;
  ecartTotal: number;
  tauxRecouvrement: number;
  countOrganismes: number;
  countLignesEnRetard: number;
}

export interface IReglementDto {
  id?: number;
  transactionDate?: string;
  paidAmount?: number;
  transactionNumber?: string;
  paymentMode?: string;
  banque?: string;
  commentaire?: string;
}

export interface ILigneRapprochement {
  factureId?: number;
  numFacture?: string;
  invoiceDate?: string;
  echeance?: string;
  montantFacture?: number;
  montantRegle?: number;
  ecart?: number;
  statut?: string;
  reglements?: IReglementDto[];
}

export interface IEtatRapprochement {
  tiersPayantName?: string;
  debutPeriode?: string;
  finPeriode?: string;
  totalFacture?: number;
  totalRegle?: number;
  ecartTotal?: number;
  lignes?: ILigneRapprochement[];
}

export interface IReglementFactureCommand {
  factureId?: number;
  factureDate?: string;
  montantRegle?: number;
  dateReglement?: string;
  transactionNumber?: string;
  paymentModeCode?: string;
  commentaire?: string;
}

// Avoir
export interface IAvoirLine {
  id?: number;
  saleLineId?: number;
  saleLineDate?: string;
  montantAvoir?: number;
  motifRejet?: string;
}

export interface IAvoir {
  id?: number;
  numAvoir?: string;
  factureOrigineId?: number;
  factureOrigineDate?: string;
  montantAvoir?: number;
  montantTva?: number;
  montantHt?: number;
  motif?: string;
  avoirDate?: string;
  statut?: 'DRAFT' | 'EMIS' | 'IMPUTE' | 'ANNULE';
  tiersPayantId?: number;
  tiersPayantName?: string;
  lignes?: IAvoirLine[];
}

export interface IAvoirCommand {
  factureId?: number;
  factureDate?: string;
  tiersPayantId?: number;
  montantAvoir?: number;
  montantTva?: number;
  montantHt?: number;
  motif?: string;
  lignes?: IAvoirLine[];
}

// Planification
export interface IPlanification {
  id?: number;
  libelle?: string;
  periodicite?: 'HEBDOMADAIRE' | 'MENSUEL' | 'BIMENSUEL' | 'QUINZAINE';
  jourDeclenchement?: number;
  heureDeclenchement?: string;
  modeEdition?: string;
  tiersPayantIds?: number[];
  groupeIds?: number[];
  factureProvisoire?: boolean;
  actif?: boolean;
  prochaineExecution?: string;
  derniereExecution?: string;
  dernierStatut?: 'SUCCESS' | 'ECHEC' | 'EN_COURS';
  dernierMessage?: string;
}

export interface IHistoriquePlanification {
  id?: number;
  planificationId?: number;
  executionDebut?: string;
  executionFin?: string;
  statut?: string;
  generationCode?: number;
  nombreFactures?: number;
  message?: string;
}

// Planification certification FNE
export interface IPlanificationFne {
  id?: number;
  libelle?: string;
  heureDeclenchement?: string;
  actif?: boolean;
  prochaineExecution?: string;
  derniereExecution?: string;
  dernierStatut?: 'SUCCESS' | 'ECHEC' | 'EN_COURS' | 'PARTIAL';
  dernierMessage?: string;
  created?: string;
  updated?: string;
}

export interface IHistoriqueCertificationFne {
  id?: number;
  planificationId?: number;
  executionDebut?: string;
  executionFin?: string;
  statut?: string;
  nbCertifiees?: number;
  nbEchecs?: number;
  message?: string;
}
