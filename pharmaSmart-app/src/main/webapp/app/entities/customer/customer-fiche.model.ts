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

/** Fiche d'un groupe de doublons (GET /api/customers/doublons). */
export interface IDoublonClient {
  id: number;
  code: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  datNaiss?: string | null;
  typeAssure: 'PRINCIPAL' | 'AYANT_DROIT';
  nombreAchats: number;
  derniereVisite?: string | null;
}

/** Fiches présumées du même patient ; la plus utilisée vient en tête. */
export interface IDoublonClientGroupe {
  type: 'ASSURE' | 'STANDARD';
  criteres: string[];
  clients: IDoublonClient[];
}

export interface IFusionClientApercu {
  targetId: number;
  sourceIds: number[];
  /** Fiches écartées, par identifiant, avec la raison. */
  rejets: Record<number, string>;
  counts: Record<string, number>;
  avertissements: string[];
}

export interface IFusionClientResult {
  targetId: number;
  mergedIds: number[];
  counts: Record<string, number>;
}

export type SuiviTraitement = 'ARRETE' | 'SANS_DELIVRANCE' | 'A_JOUR' | 'A_RENOUVELER' | 'EN_RETARD' | 'RUPTURE';

/** Traitement chronique et son suivi (GET /api/customers/{id}/traitements-chroniques). */
export interface ITraitementChronique {
  id: number;
  dciId?: number | null;
  dciLibelle?: string | null;
  /** Produit imposé : seul lui renouvelle le traitement (patient non substituable). */
  produitId?: number | null;
  produitLibelle?: string | null;
  dosage?: string | null;
  posologie?: string | null;
  dureeJours: number;
  dateOrdonnance?: string | null;
  dateFinOrdonnance?: string | null;
  note?: string | null;
  actif: boolean;
  derniereDelivrance?: string | null;
  /** Produit alors délivré : celui que « Re-délivrer » propose. */
  dernierProduitId?: number | null;
  dernierProduitLibelle?: string | null;
  prochaineDelivrance?: string | null;
  /** Négatif en cas de retard. */
  joursRestants?: number | null;
  suivi: SuiviTraitement;
  ordonnance: 'NON_RENSEIGNEE' | 'VALIDE' | 'EXPIREE';
  renouvellementExceptionnelJusquau?: string | null;
  renouvellementExceptionnelMotif?: string | null;
}

export interface ITraitementChroniqueSaisie {
  dciId?: number | null;
  produitId?: number | null;
  dosage?: string | null;
  posologie?: string | null;
  dureeJours: number;
  dateOrdonnance?: string | null;
  dateFinOrdonnance?: string | null;
  note?: string | null;
  actif: boolean;
}

export interface ITraitementARenouveler {
  customerId: number;
  customerCode: string;
  customerNom: string;
  telephone?: string | null;
  traitement: ITraitementChronique;
}

export type CanalConsentement = 'SMS' | 'WHATSAPP' | 'EMAIL';

/** `accorde` absent : le client n'a jamais été interrogé sur ce canal. */
export interface IConsentement {
  canal: CanalConsentement;
  accorde?: boolean | null;
  date?: string | null;
  recueilliPar?: string | null;
}
