/**
 * Contrats de l'API du journal des remises (`/api/journal-remises`), miroir des DTO de
 * `service/remise/dto`. Montants en francs TTC, sauf ceux marqués HT.
 */

export type NatureJournal = 'PROMOTIONS' | 'REMISES' | 'UG';
export type OrigineRemise = 'PROMOTION' | 'REMISE_PRODUIT' | 'CUMUL';
export type NatureVente = 'COMPTANT' | 'ASSURANCE' | 'CARNET';
export type FormatExport = 'PDF' | 'XLSX' | 'CSV';
export type TypePromotion =
  'PROMO_POURCENTAGE' | 'PROMO_MONTANT' | 'PROMO_PRIX_FIXE' | 'PROMO_OFFRE_NM' | 'PROMO_PACK' | 'PROMO_SEUIL_MONTANT';

export interface ILigneJournal {
  date: string;
  heure: string;
  ticket: string;
  natureVente: NatureVente;
  caissier: string | null;
  vendeur: string | null;
  client: string | null;
  cip: string | null;
  produit: string;
  famille: string | null;
  quantite: number;
  prixUnitaire: number;
  montantBrut: number;
  remise: number;
  partPromotion: number;
  partRemiseProduit: number;
  origine: OrigineRemise | null;
  promotionId: number | null;
  promotionLibelle: string | null;
  unitesPromo: number;
  grille: string | null;
  /** En pourcentage. */
  tauxRemise: number;
  montantNet: number;
  montantHtNet: number;
  quantiteUg: number;
  valeurUg: number;
  remiseUg: number;
  htNetUg: number;
  prixAchat: number;
  coutAchatComptabilise: number;
  coutAchatReel: number;
  coutEviteUg: number;
  margeComptabilisee: number;
  margeReelle: number;
  margeUg: number;
}

export interface ITotaux {
  lignes: number;
  tickets: number;
  unites: number;
  montantBrut: number;
  remise: number;
  montantNet: number;
}

export interface ITotauxUg {
  lignes: number;
  tickets: number;
  unites: number;
  valeur: number;
  remise: number;
  htNet: number;
  coutEvite: number;
  marge: number;
  margeComptabilisee: number;
  margeReelle: number;
}

export interface IParPromotion {
  promotionId: number;
  libelle: string;
  type: TypePromotion | null;
  lignes: number;
  tickets: number;
  unitesRemisees: number;
  /** Offre N+M seulement : M unités par groupe complet. */
  unitesOffertes: number | null;
  remise: number;
  montantNet: number;
}

export interface IParGrille {
  grille: string;
  lignes: number;
  tickets: number;
  remise: number;
  montantNet: number;
}

export interface ISyntheseJournal {
  du: string;
  au: string;
  /** Promotions et remises produit, sans les UG. */
  remiseTotale: number;
  promotions: ITotaux;
  remisesProduit: ITotaux;
  ug: ITotauxUg;
  parPromotion: IParPromotion[];
  parGrille: IParGrille[];
}

/** Période et filtres d'une demande ; une liste vide ne filtre rien. */
export interface IFiltreJournal {
  du: string;
  au: string;
  naturesVente: NatureVente[];
  origines: OrigineRemise[];
}

export const NATURE_JOURNAL_LIBELLES: Record<NatureJournal, string> = {
  PROMOTIONS: 'Promotions',
  REMISES: 'Remises produit',
  UG: 'UG vendues',
};

export const ORIGINE_LIBELLES: Record<OrigineRemise, string> = {
  PROMOTION: 'Promotion',
  REMISE_PRODUIT: 'Remise produit',
  CUMUL: 'Promotion et remise produit',
};

export const NATURE_VENTE_LIBELLES: Record<NatureVente, string> = {
  COMPTANT: 'Comptant',
  ASSURANCE: 'Assurance',
  CARNET: 'Carnet',
};

export const MECANIQUE_LIBELLES: Record<TypePromotion, string> = {
  PROMO_POURCENTAGE: 'Pourcentage',
  PROMO_MONTANT: 'Montant',
  PROMO_PRIX_FIXE: 'Prix fixe',
  PROMO_OFFRE_NM: 'Offre N+M',
  PROMO_PACK: 'Pack',
  PROMO_SEUIL_MONTANT: 'Seuil en montant',
};

/** Les paramètres de requête d'un filtre : une liste vide n'est pas envoyée. */
export function parametres(filtre: IFiltreJournal): Record<string, string | string[]> {
  const params: Record<string, string | string[]> = { du: filtre.du, au: filtre.au };
  if (filtre.naturesVente.length) {
    params['naturesVente'] = filtre.naturesVente;
  }
  if (filtre.origines.length) {
    params['origines'] = filtre.origines;
  }
  return params;
}

/** Le nom du fichier exporté, comme le serveur le donne : le navigateur ne lit pas son en-tête. */
export function nomFichier(format: FormatExport, nature: NatureJournal | null, filtre: IFiltreJournal): string {
  const objet = nature ? `journal-${nature.toLowerCase()}` : 'journal-remises';
  const jour = (iso: string): string => iso.replaceAll('-', '');
  return `${objet}-${jour(filtre.du)}-${jour(filtre.au)}.${format.toLowerCase()}`;
}

/**
 * L'écart que la gratuité des UG creuse dans la marge des états : ils comptent le coût d'achat sur
 * toute la quantité, UG comprises (docs/PLAN-COUT-ACHAT-UG.md).
 */
export function ecartMargeUg(ug: ITotauxUg): number {
  return ug.margeReelle - ug.margeComptabilisee;
}
