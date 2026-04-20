export type AvoirStatut = 'EN_ATTENTE' | 'REMBOURSE' | 'IMPUTE';

export interface IAvoirFournisseur {
  id?: number;
  reference?: string;
  dateMtv?: string;
  montant?: number;
  statut?: AvoirStatut;
  commentaire?: string;
  fournisseurId?: number;
  fournisseurLibelle?: string;
  reponseRetourBonId?: number;
  retourBonId?: number;
  retourBonReference?: string;
}

export interface IAvoirEncoursFournisseur {
  fournisseurId?: number;
  fournisseurLibelle?: string;
  montantEncours?: number;
}
