import { ClasseCriticite } from './classe-criticite.model';

/**
 * Répartition par classe de criticité (A+, A, B, C, D)
 */
export interface IClasseBreakdown {
  classeCriticite: ClasseCriticite;
  nbProduits: number;
  nbRupture: number;
  nbSousSeuil: number;
  nbOk: number;
  nbSurstock: number;
}

/**
 * Produit urgent nécessitant une commande immédiate
 */
export interface ITopUrgentDTO {
  produitId: number;
  libelle: string;
  codeCip?: string;
  fournisseurLibelle?: string;
  classeCriticite?: ClasseCriticite;
  vmm: number;
  margeSecurite: number;
  stockObjectif: number;
  stockActuel: number;
  quantiteACommander: number;
  /** Couverture actuelle (Stock Actuel / VMM) — calculée par le backend */
  tauxCouvertureMois: number;
}

/**
 * DTO complet du tableau de bord réapprovisionnement SEMOIS
 */
export interface IReapproDashboard {
  totalProduits: number;
  nbRupture: number;
  nbSousSeuil: number;
  nbOk: number;
  nbSurstock: number;
  nbSansConfig: number;
  totalUnitesManquantes: number;
  parClasse: IClasseBreakdown[];
  topUrgents: ITopUrgentDTO[];
}

