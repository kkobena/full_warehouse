export class Reglement {}

export class Banque {
  nom?: string;
  adresse?: string;
  code?: string;
  beneficiaire?: string;
}

export class ReglementParams {
  amount?: number;
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
  /**
   * Reglement partiel de toutes les  factures selectionnées
   */
  FACTURE_PARTIEL_ALL = 'FACTURE_PARTIEL_ALL',
  /**
   * Reglement partiel de toutes les  factures groupées selectionnées
   */
  GROUPE_PARTIEL_ALL = 'GROUPE_PARTIEL_ALL',
  GROUP = 'GROUP',
  SINGLE = 'SINGLE',
}
