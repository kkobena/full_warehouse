/** En-tête de la fiche client (GET /api/customers/{id}/synthese). */
export interface ICustomerSynthese {
  customerId: number;
  /** Reste dû sur les ventes différées. */
  encours: number;
  derniereVisite?: string;
  /** Achats clôturés des douze derniers mois. */
  nombreAchats: number;
  montantAchats: number;
}

/** Produit délivré au client, agrégé sur une période (GET /api/customers/{id}/produits-delivres). */
export interface IProduitDelivre {
  produitId: number;
  libelle: string;
  nombreDelivrances: number;
  quantite: number;
  montant: number;
  derniereDelivrance?: string;
}

/** Allergie à une molécule (`dciId`) ou, à défaut, à un libellé libre. */
export interface IAllergie {
  id?: number;
  dciId?: number;
  dciLibelle?: string;
  libelle?: string;
  reaction?: string;
}

/** Dossier de sécurité du patient (GET/PUT /api/customers/{id}/dossier-sante). */
export interface IDossierSante {
  allergies: IAllergie[];
  pathologies: string[];
  grossesse: boolean;
  dateTerme?: string;
  allaitement: boolean;
  poidsKg?: number;
  datePesee?: string;
  note?: string;
  updatedAt?: string;
  updatedBy?: string;
}

/** Alerte levée par l'ajout d'un produit : BLOQUANTE (allergie) ou INFO (grossesse, allaitement). */
export interface IAlerteSante {
  niveau: 'BLOQUANTE' | 'INFO';
  type: 'ALLERGIE' | 'GROSSESSE' | 'ALLAITEMENT';
  message: string;
}

/** Situation face à la limite de crédit de l'officine (GET /api/customers/{id}/limite-credit). */
export interface ISituationCredit {
  /** 0 = aucune limite. */
  limite: number;
  encours: number;
  /** Marge avant la limite ; absente sans limite. */
  disponible?: number | null;
}

/** Relance SMS envoyée pour les différés. */
export interface IRelanceDiffere {
  date: string;
  telephone: string;
  montant: number;
  message: string;
  envoyePar: string;
}
