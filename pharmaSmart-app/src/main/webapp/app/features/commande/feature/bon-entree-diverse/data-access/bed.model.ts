export type MotifBed =
  | 'RETOUR_CLIENT'
  | 'ECHANTILLON'
  | 'TRANSFERT_ENTRANT'
  | 'REGULARISATION'
  | 'CORRECTION_ERREUR'
  | 'BASCULEMENT'
  | 'BASCULEMENT_PRESTIGE'
  | 'AUTRE';

export type BedStatut = 'REQUESTED' | 'CLOSED' | 'ARCHIVED';

export const MOTIFS_BED: { value: MotifBed; label: string }[] = [
  { value: 'RETOUR_CLIENT', label: 'Retour client' },
  { value: 'ECHANTILLON', label: 'Echantillon / Don laboratoire' },
  { value: 'TRANSFERT_ENTRANT', label: 'Transfert entrant inter-pharmacie' },
  { value: 'REGULARISATION', label: 'Régularisation positive' },
  { value: 'CORRECTION_ERREUR', label: "Correction d'erreur" },
  { value: 'BASCULEMENT', label: 'Basculement depuis autre logiciel' },
  { value: 'BASCULEMENT_PRESTIGE', label: 'Basculement depuis Prestige' },
  { value: 'AUTRE', label: 'Autre' },
];

export const MOTIFS_BED_CREATION: { value: MotifBed; label: string }[] = [
  { value: 'RETOUR_CLIENT', label: 'Retour client' },
  { value: 'ECHANTILLON', label: 'Echantillon / Don laboratoire' },
  { value: 'TRANSFERT_ENTRANT', label: 'Transfert entrant inter-pharmacie' },
  { value: 'REGULARISATION', label: 'Régularisation positive' },
  { value: 'CORRECTION_ERREUR', label: "Correction d'erreur" },
  { value: 'AUTRE', label: 'Autre' },
];




export interface IBedLigne {
  id?: number;
  orderDate?: string;
  produitId?: number;
  produitLibelle?: string;
  codeCip?: string;
  fournisseurProduitId?: number;
  quantite?: number;
  prixAchat?: number;
  prixVente?: number;
}

export interface IBed {
  id?: number;
  orderDate?: string;
  receiptReference?: string;
  motifBed?: MotifBed;
  commentaireBed?: string;
  fournisseurId?: number;
  fournisseurLibelle?: string;
  orderStatus?: BedStatut;
  lignes?: IBedLigne[];
  grossAmount?: number;
  createdAt?: string;
}

export interface IBedSummary {
  id?: number;
  orderDate?: string;
  receiptReference?: string;
  motifBed?: MotifBed;
  motifLabel?: string;
  fournisseurLibelle?: string;
  orderStatus?: BedStatut;
  lignesCount?: number;
  grossAmount?: number;
  createdAt?: string;
}
