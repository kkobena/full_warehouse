import { IFactureId } from './facture.model';

export interface IPaymentId {
  id: number;
  transactionDate: string;
}

export interface IInvoicePaymentItem {
  numBon: string;
  montant: string;
  montantVente: string;
  montantAttendu: string;
  customer: string;
  created: string;
  heure: string;
  customerMatricule: string;
  montantRestant: string;
}

export interface IReglement {
  id: IPaymentId;
  organismeId: number;
  organisme: string;
  codeFacture: string;
  montantAttendu: string;
  montantVerse: string;
  montantRestant: string;
  paymentMode: string;
  change: string;
  user: string;
  created: string;
  paidAmount: string;
  grouped: boolean;
  invoicePaymentItemsCount: number;
  totalAmount: number;
  invoicePayments: IReglement[];
  invoicePaymentItems: IInvoicePaymentItem[];
}

export interface IBanque {
  nom?: string;
  adresse?: string;
  code?: string;
  beneficiaire?: string;
}

export interface IReglementFactureDossier {
  id: number;
  montantPaye?: number;
  parentId?: number;
  montantTotal?: number;
  numFacture?: string;
  organismeName?: string;
  itemsCount?: number;
  debutPeriode?: Date;
  finPeriode?: Date;
  montantDetailRegle?: number;
  facturationDate?: Date;
  saleDate?: Date;
  matricule?: string;
  customerFullName?: string;
  bonNumber?: string;
  groupe?: boolean;
  montantVerse?: number;
  invoiceDate?: string;
  parentInvoiceDate?: string;
}

export interface IDossierFactureProjection {
  montantPaye?: number;
  montantTotal?: number;
  numFacture?: string;
  categorie?: string;
  name?: string;
  itemCount?: number;
  montantDetailRegle?: number;
  facturationDate?: Date;
  id?: number;
  montantVerse?: number;
  invoiceDate?: string;
  factureItemId: IFactureId;
}

export interface ILigneSelectionnes {
  id: IFactureId | number;
  montantVerse: number;
  montantAttendu: number;
  montantFacture?: number;
}

export interface IReglementParams {
  amount?: number;
  id: IFactureId;
  montantFacture: number;
  totalAmount?: number;
  amountToPaid?: number;
  comment?: string;
  mode?: ModeEditionReglement;
  modePaimentCode: string;
  partialPayment: boolean;
  banqueInfo?: IBanque;
  dossierIds?: number[];
  ligneSelectionnes?: ILigneSelectionnes[];
  paymentDate?: string;
}

export interface IResponseReglement {
  id: IPaymentId;
  total: boolean;
}

export interface ISelectedFacture {
  isGroup: boolean;
  facture: IDossierFactureProjection;
}

export interface IInvoicePaymentParam {
  search?: string;
  organismeId?: number;
  fromDate?: string;
  toDate?: string;
  grouped: boolean;
}

export enum ModeEditionReglement {
  GROUPE_TOTAL   = 'GROUPE_TOTAL',
  FACTURE_TOTAL  = 'FACTURE_TOTAL',
  FACTURE_PARTIEL = 'FACTURE_PARTIEL',
  GROUPE_PARTIEL  = 'GROUPE_PARTIEL',
  GROUP  = 'GROUP',
  SINGLE = 'SINGLE',
}
