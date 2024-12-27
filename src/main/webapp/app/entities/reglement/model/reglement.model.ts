import { Facture } from '../../facturation/facture.model';

export class Reglement {}

export class Banque {
  nom?: string;
  adresse?: string;
  code?: string;
  beneficiaire?: string;
}

export class ReglementParams {
  amount?: number;
  id: number;
  montantFacture: number;
  totalAmount?: number;
  amountToPaid?: number;
  comment?: string;
  mode?: ModeEditionReglement;
  modePaimentCode: string;
  partialPayment: boolean;
  banqueInfo?: Banque;
  dossierIds?: number[];
  ligneSelectionnes?: LigneSelectionnes[];
  paymentDate?: string;
}

export class LigneSelectionnes {
  id: number;
  montantVerse: number;
  montantAttendu: number;
  montantFacture: number;
}

export class ResponseReglement {
  id: number;
  total: boolean;
}

export enum ModeEditionReglement {
  /**
   * Reglement total  facture groupée
   */
  GROUPE_TOTAL = 'GROUPE_TOTAL',
  /**
   * Reglement total  facture individuelle
   */
  FACTURE_TOTAL = 'FACTURE_TOTAL',
  /**
   * Reglement partiel facture individuelle
   */
  FACTURE_PARTIEL = 'FACTURE_PARTIEL',
  /**
   * Reglement partiel facture groupée
   */
  GROUPE_PARTIEL = 'GROUPE_PARTIEL',

  GROUP = 'GROUP',
  SINGLE = 'SINGLE',
}

export class SelectedFacture {
  isGroup: boolean;
  facture: Facture;
}
