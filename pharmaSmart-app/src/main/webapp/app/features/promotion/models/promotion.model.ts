/**
 * Promotion : remise datée, sur une liste d'articles du magasin, appliquée par le moteur au comptoir
 * sans saisie (docs/PLAN-GESTION-PROMOTIONS.md). Le statut porte la décision humaine ; l'état
 * (à venir, en cours, terminée) se lit sur les dates et vient du serveur.
 */
/** PROMO_OFFRE_NM : `quantiteMin` porte N (achetés), `valeur` M (offerts, les moins chers du groupe). */
/** PROMO_PACK : `valeur` porte le prix du pack, chaque article sa quantité dans le pack. */
/** PROMO_SEUIL_MONTANT : `valeur` porte le taux, `seuilMontant` le montant d'achat des articles qui le déclenche. */
export type TypePromotion =
  'PROMO_POURCENTAGE' | 'PROMO_MONTANT' | 'PROMO_PRIX_FIXE' | 'PROMO_OFFRE_NM' | 'PROMO_PACK' | 'PROMO_SEUIL_MONTANT';

export type StatutPromotion = 'BROUILLON' | 'VALIDEE' | 'SUSPENDUE' | 'ANNULEE';

/** EPUISEE : en cours, mais le plafond total d'unités est atteint ; le comptoir ne la pose plus. */
export type EtatPromotion = 'BROUILLON' | 'A_VENIR' | 'EN_COURS' | 'EPUISEE' | 'TERMINEE' | 'SUSPENDUE' | 'ANNULEE';

export interface IPromotionProduit {
  produitId: number;
  libelle: string;
  codeCip?: string | null;
  prixReference: number;
  prixPromo?: number | null;
  prixApresPromo: number;
  /** Unités dans le pack ; 1 pour les autres mécaniques. */
  quantite?: number;
  /** Médicament à prix administré (TVA 0) : signalé, c'est au pharmacien d'en décider. */
  prixAdministre?: boolean;
  /** Vendu en promotion au prix d'achat ou en dessous (un pack se juge en entier). */
  aPerte?: boolean;
}

export interface IPromotion {
  id: number;
  code: string;
  libelle: string;
  type: TypePromotion;
  valeur?: number | null;
  /** AAAA-MM-JJ, bornes incluses. */
  dateDebut: string;
  dateFin: string;
  statut: StatutPromotion;
  etat: EtatPromotion;
  priorite: number;
  quantiteMin: number;
  quantiteMaxParVente?: number | null;
  /** « Dans la limite de N unités », toutes ventes clôturées confondues. */
  quantiteMaxTotale?: number | null;
  /** Unités déjà remisées sur les ventes clôturées et non annulées. */
  unitesRemisees: number;
  nombreProduits: number;
  /** Vide dans la liste : seul le détail charge les articles. */
  produits: IPromotionProduit[];
  /** Code-barres imprimé sur l'emballage d'un pack ; absent pour les autres mécaniques. */
  codeBarrePack?: string | null;
  /** Montant d'achat des articles de la promotion qui déclenche une remise de panier. */
  seuilMontant?: number | null;
  /** S'applique aussi après la remise produit de la vente. */
  cumulable?: boolean;
  /** Prix administré, vente à perte… en phrases : signalé à la création, dans la liste et à la validation. */
  alertes?: string[];
}

/** Les ventes des articles d'une promotion sur une période. `chiffreAffaires` est net des remises. */
export interface IPeriodeBilan {
  unites: number;
  chiffreAffaires: number;
  marge: number;
  tickets: number;
  clients: number;
}

/**
 * Le bilan d'une promotion : ce qu'elle a coûté, ce qu'elle a fait vendre, et l'effet sur la marge de
 * ses articles comparé à la période précédente de même durée (`debutAvant` → `finAvant`).
 */
export interface IBilanPromotion {
  promotionId: number;
  code: string;
  libelle: string;
  debut: string;
  fin: string;
  debutAvant: string;
  finAvant: string;
  remise: number;
  unitesRemisees: number;
  tickets: number;
  clients: number;
  pendant: IPeriodeBilan;
  avant: IPeriodeBilan;
  articles: {
    produitId: number;
    libelle: string;
    unites: number;
    unitesAvant: number;
    unitesRemisees: number;
    remise: number;
    chiffreAffaires: number;
    marge: number;
    margeAvant: number;
  }[];
}

/** « +25 % », « −8 % », « = » ; « nouveau » quand il n'y avait rien avant. */
export function variation(avant: number, pendant: number): string {
  if (avant === 0) {
    return pendant === 0 ? '=' : 'nouveau';
  }
  const ecart = Math.round(((pendant - avant) / Math.abs(avant)) * 100);
  if (ecart === 0) {
    return '=';
  }
  return ecart > 0 ? `+${ecart} %` : `−${-ecart} %`;
}

/** Le bilan se lit dès le premier jour de l'opération. `aujourdhui` au format AAAA-MM-JJ. */
export function bilanDisponible(promotion: Pick<IPromotion, 'dateDebut'>, aujourdhui: string): boolean {
  return promotion.dateDebut <= aujourdhui;
}

/** Un rayon du magasin ou une famille, et le nombre de ses articles actifs. */
export interface IOptionPerimetre {
  id: number;
  libelle: string;
  articles: number;
}

export interface IChoixPerimetre {
  rayons: IOptionPerimetre[];
  familles: IOptionPerimetre[];
}

/** Les articles d'un rayon ou d'une famille, à ajouter en bloc ; les prix administrés y sont, signalés. */
export interface IPerimetre {
  articles: {
    produitId: number;
    libelle: string;
    codeCip?: string | null;
    prix: number;
    prixAchat: number;
    prixAdministre?: boolean;
  }[];
  administres: string[];
}

/** Un pack lu au scan de son code-barres : ses articles, à ajouter au panier chacun à sa quantité. */
export interface IPackComptoir {
  promotionId: number;
  libelle: string;
  prix: number;
  composants: {
    produitId: number;
    libelle: string;
    regularUnitPrice: number;
    quantite: number;
    itemQty: number;
    parentId?: number | null;
  }[];
}

/** Un article en promotion aujourd'hui dans le magasin : la promotion que le comptoir lui appliquera. */
export interface IPromotionArticle {
  produitId: number;
  promotionId: number;
  libelle: string;
  prixReference: number;
  prixApresPromo: number;
  /** AAAA-MM-JJ, incluse. */
  dateFin: string;
  /** Au-delà de 1, le prix remisé ne vaut qu'à partir de cette quantité sur la ligne (N+M pour une offre). */
  quantiteMin: number;
  /** « 2 achetés + 1 offert » pour une offre N+M : elle n'a pas de prix unitaire remisé à afficher. */
  offre?: string | null;
}

export interface IPromotionProduitForm {
  produitId: number;
  prixPromo?: number | null;
  quantite?: number | null;
}

export interface IPromotionForm {
  libelle: string;
  type: TypePromotion;
  valeur?: number | null;
  dateDebut: string;
  dateFin: string;
  priorite: number;
  quantiteMin: number;
  quantiteMaxParVente?: number | null;
  quantiteMaxTotale?: number | null;
  produits: IPromotionProduitForm[];
  seuilMontant?: number | null;
  cumulable?: boolean;
}

export const TYPE_PROMOTION_OPTIONS: { value: TypePromotion; label: string }[] = [
  { value: 'PROMO_POURCENTAGE', label: 'Remise en pourcentage' },
  { value: 'PROMO_MONTANT', label: 'Remise en montant par unité' },
  { value: 'PROMO_PRIX_FIXE', label: 'Prix fixe par article' },
  { value: 'PROMO_OFFRE_NM', label: 'Offre N achetés + M offerts' },
  { value: 'PROMO_PACK', label: 'Pack : articles vendus ensemble' },
  { value: 'PROMO_SEUIL_MONTANT', label: "Remise dès un montant d'achat" },
];

/**
 * Prix d'une unité de l'article dans le pack : sa part de la remise du pack, au prorata de son prix
 * (même arithmétique que le serveur, `CalculPromotion`).
 */
export function prixDansLePack(prix: number, prixReferencePack: number, prixPack: number | null | undefined): number {
  if (!prix || prixReferencePack <= 0) {
    return prix;
  }
  const remisePack = Math.max(0, prixReferencePack - (prixPack ?? 0));
  return prix - Math.floor((prix * remisePack) / prixReferencePack);
}

/** « 2 achetés + 1 offert » : l'offre dite comme en rayon (même libellé que l'étiquette). */
export function libelleOffre(achetees: number, offertes: number): string {
  return `${achetees} acheté${achetees > 1 ? 's' : ''} + ${offertes} offert${offertes > 1 ? 's' : ''}`;
}

export const ETAT_PROMOTION_LIBELLES: Record<EtatPromotion, string> = {
  BROUILLON: 'Brouillon',
  A_VENIR: 'À venir',
  EN_COURS: 'En cours',
  EPUISEE: 'Épuisée',
  TERMINEE: 'Terminée',
  SUSPENDUE: 'Suspendue',
  ANNULEE: 'Annulée',
};

/**
 * Prix d'une unité après promotion, pour l'aperçu du formulaire. Même arithmétique que le serveur
 * (`CalculPromotion`) : le pourcentage est arrondi au supérieur. Le serveur reste seul juge.
 */
export function prixApresPromotion(
  type: TypePromotion,
  valeur: number | null | undefined,
  prix: number,
  prixPromo?: number | null,
  achetees?: number | null,
): number {
  if (!prix || prix <= 0) {
    return 0;
  }
  switch (type) {
    // Une remise de panier, une fois le seuil atteint, remise chaque unité de son taux.
    case 'PROMO_POURCENTAGE':
    case 'PROMO_SEUIL_MONTANT':
      return prix - Math.min(prix, Math.ceil((prix * (valeur ?? 0)) / 100));
    case 'PROMO_MONTANT':
      return prix - Math.min(prix, valeur ?? 0);
    case 'PROMO_PRIX_FIXE':
      return prixPromo && prixPromo < prix ? prixPromo : prix;
    case 'PROMO_PACK':
      // Dépend de tout le pack : le formulaire le calcule (`prixDansLePack`).
      return prix;
    case 'PROMO_OFFRE_NM': {
      // Le prix moyen d'une unité dans un groupe du même article ; `valeur` porte M, `achetees` N.
      const offertes = Math.max(1, valeur ?? 0);
      const taille = Math.max(1, achetees ?? 1) + offertes;
      return prix - Math.ceil((prix * offertes) / taille);
    }
  }
}

/**
 * Une mécanique qui se calcule ligne par ligne : seule elle peut se cumuler avec la remise produit.
 * Une offre, un pack ou une remise de panier répartissent leur remise sur les prix normaux.
 */
export function estDeLigne(type: TypePromotion): boolean {
  return type === 'PROMO_POURCENTAGE' || type === 'PROMO_MONTANT' || type === 'PROMO_PRIX_FIXE';
}

/** « −10 % dès 20 000 F d'achat » : la remise de panier en une phrase, comme l'étiquette. */
export function libelleSeuil(taux: number, seuil: number, formater: (montant: number) => string): string {
  return `−${taux} % dès ${formater(seuil)} d'achat`;
}

/** Le bilan d'un ajout en bloc, en une phrase. */
export function bilanPerimetre(ajoutes: number, dejaPresents: number, administres: string[]): string {
  const parties = [ajoutes === 0 ? 'Aucun article ajouté' : `${ajoutes} article(s) ajouté(s)`];
  if (dejaPresents) {
    parties.push(`${dejaPresents} déjà dans la promotion`);
  }
  if (administres.length) {
    const cites = administres.slice(0, 3).join(', ') + (administres.length > 3 ? '…' : '');
    parties.push(`dont ${administres.length} à prix administré : ${cites}`);
  }
  return parties.join(' · ') + '.';
}
