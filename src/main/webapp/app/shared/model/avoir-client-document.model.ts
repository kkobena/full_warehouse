export type AvoirClientStatut = 'OUVERT' | 'CLOTURE' | 'ANNULE' | 'EXPIRE';

export interface IAvoirClientDocument {
  id?: number;
  reference?: string;
  createdAt?: string;
  clotureLe?: string;
  statut?: AvoirClientStatut;
  modeCloture?: string;
  quantite?: number;
  montant?: number;
  montantUtilise?: number;
  montantRestant?: number;
  dateExpiration?: string;
  procheExpiration?: boolean;
  commentaire?: string;
  customerName?: string;
  produitLibelle?: string;
  codeCip?: string;
  salesLineId?: number;
  salesLineDate?: string;
  numberTransaction?: string;
  commandeReference?: string;
  closedByName?: string;
}
