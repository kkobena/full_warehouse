export type StatutOrdonnance = 'EN_COURS' | 'TERMINEE' | 'EXPIREE';

export interface IPrescripteur {
  id?: number;
  nom: string;
  prenom?: string | null;
  specialite?: string | null;
  numeroOrdre?: string | null;
  structure?: string | null;
  telephone?: string | null;
  actif: boolean;
}

export interface IOrdonnanceLigne {
  id: number;
  rang: number;
  texteLu?: string | null;
  produitId?: number | null;
  produitLibelle?: string | null;
  posologie?: string | null;
  dureeJours?: number | null;
  quantitePrescrite: number;
  quantiteDelivree: number;
  resteADelivrer: number;
}

export interface IVenteLiee {
  salesId: number;
  salesDate: string;
  statut: string;
  annulee: boolean;
}

export interface IOrdonnance {
  id: number;
  customerId: number;
  prescripteurId?: number | null;
  prescripteurNom?: string | null;
  datePrescription: string;
  source: string;
  renouvellements: number;
  renouvellementsRestants: number;
  dateFinValidite?: string | null;
  statut: StatutOrdonnance;
  cloturee: boolean;
  note?: string | null;
  lignes: IOrdonnanceLigne[];
  ventes: IVenteLiee[];
}

/** Résultat du rattachement d'une vente : lignes liées, dont celles liées par équivalence générique. */
export interface IAppariement {
  ordonnance: IOrdonnance;
  lignesLiees: number;
  lignesGeneriques: number;
}

export interface IOrdonnanceLigneCreation {
  produitId?: number | null;
  texteLu?: string | null;
  posologie?: string | null;
  dureeJours?: number | null;
  quantitePrescrite: number;
}

export interface IOrdonnanceCreation {
  customerId: number;
  prescripteurId?: number | null;
  datePrescription: string;
  renouvellements: number;
  dateFinValidite?: string | null;
  note?: string | null;
  lignes: IOrdonnanceLigneCreation[];
}
