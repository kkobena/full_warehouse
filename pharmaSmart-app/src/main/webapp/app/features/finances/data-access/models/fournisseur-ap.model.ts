export type StatutFournisseur = 'A_JOUR' | 'EN_RETARD' | 'CRITIQUE';
export type StatutLigne = 'EN_ATTENTE' | 'PARTIEL' | 'REGLE' | 'EN_RETARD';

export interface ICompteFournisseurAP {
  fournisseurId: number;
  fournisseurName: string;
  fournisseurCode: string;
  phone?: string;
  mobile?: string;
  totalCommande: number;
  totalRegle: number;
  solde: number;
  nbCommandesEnAttente: number;
  prochaineEcheance?: string;
  statut: StatutFournisseur;
}

export interface ILigneFournisseurAP {
  commandeId: number;
  numBon: string;
  dateCommande: string;
  dateEcheance?: string;
  montant: number;
  montantRegle: number;
  restantDu: number;
  statut: StatutLigne;
}

export interface IFournisseurAPSummary {
  totalDu: number;
  echeancesDepassees: number;
  echeancesProchaines: number;
  nbFournisseursActifs: number;
}

export interface IReglementFournisseurCommand {
  montant: number;
  dateReglement: string;
  reference: string;
  modeReglement: string;
  commentaire?: string;
  commandeId?: number;
}

export interface IReglementBL {
  id?: number;
  dateReglement: string;
  montant: number;
  reference: string;
  commentaire?: string;
  operateur?: string;
}
