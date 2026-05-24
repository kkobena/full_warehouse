export type AvoirFournisseurStatut = 'EN_ATTENTE' | 'REMBOURSE' | 'IMPUTE' | 'ANNULE';

export interface IAvoirFournisseur {
  id?: number;
  reference?: string;
  dateMtv?: string;
  montant?: number;
  statut?: AvoirFournisseurStatut;
  commentaire?: string;
  fournisseurId?: number;
  fournisseurLibelle?: string;
  retourBonId?: number;
  retourBonReference?: string;
}

export interface IAvoirEncoursFournisseur {
  fournisseurId?: number;
  fournisseurLibelle?: string;
  montantEncours?: number;
}

export interface IAvoirLigneCommand {
  retourBonItemId: number;
  qtyAcceptee: number;
  prixAchat?: number;
}

export interface IAvoirFournisseurCommand {
  retourBonId: number;
  commentaire?: string;
  lignes: IAvoirLigneCommand[];
}

export interface IBonLigneItem {
  orderLineId: number;
  orderLineOrderDate: string;
  produitId: number;
  produitCip?: string;
  qtyRetour: number;
  motifRetourId: number;
  prixAchat?: number;
}

export interface IAvoirFromBonLignesCommand {
  commandeId: number;
  commandeOrderDate: string;
  commentaire?: string;
  lignes: IBonLigneItem[];
}
