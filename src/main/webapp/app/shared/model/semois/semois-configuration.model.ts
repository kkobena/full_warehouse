import { ClasseCriticite } from './classe-criticite.model';

/**
 * Configuration SEMOIS par classe de criticité (5 lignes globales : A+, A, B, C, D)
 */
export interface ISemoisClasseConfig {
  classeCriticite: ClasseCriticite;
  coefficientSecurite: number;
  nbMoisHistorique: number;
  limitePeremption: boolean;
}

/**
 * Configuration SEMOIS pour un produit
 */
export interface ISemoisConfiguration {
  id?: number;
  produitId?: number;
  classeCriticite?: ClasseCriticite;
  coefficientSecurite?: number;
  nbMoisHistorique?: number;
  delaiLivraisonJours?: number;
  facteurSaisonnierActuel?: number;
  limitePeremption?: boolean;
  vmmCalcule?: number;
  stockObjectifCalcule?: number;
  dateDernierCalcul?: Date;
  createdAt?: Date;
  updatedAt?: Date;
}

//Exclusion temporaire ──────────────────────────────────────────────

/**
 * Représente une exclusion temporaire d'un produit du batch SEMOIS.
 * Un produit exclu n'apparaît plus dans la vue v_semois_suggestion
 * et n'est pas réintégré dans le panier par creerSuggestionBatch() jusqu'à expiration.
 */
export interface ISemoisExclusion {
  produitId: number;
  produitLibelle: string;
  exclusionDureeJours: number;
  exclusionMotif?: string;
  exclusionDate: string;       // ISO datetime
  exclusionDateFin: string;    // ISO datetime
  exclActif: boolean;
}

/** Payload envoyé à POST /api/semois/configuration/{produitId}/exclure */
export interface IExclusionRequest {
  dureeJours?: number;
  motif?: string;
}

/** Réponse de GET /api/semois/exclusions/count */
export interface IExclusionCount {
  count: number;
}

export class SemoisConfiguration implements ISemoisConfiguration {
  constructor(
    public id?: number,
    public produitId?: number,
    public classeCriticite?: ClasseCriticite,
    public coefficientSecurite?: number,
    public nbMoisHistorique?: number,
    public delaiLivraisonJours?: number,
    public facteurSaisonnierActuel?: number,
    public limitePeremption?: boolean,
    public vmmCalcule?: number,
    public stockObjectifCalcule?: number,
    public dateDernierCalcul?: Date,
    public createdAt?: Date,
    public updatedAt?: Date,
  ) {
    this.limitePeremption = this.limitePeremption ?? false;
    this.facteurSaisonnierActuel = this.facteurSaisonnierActuel ?? 1.0;
    this.nbMoisHistorique = this.nbMoisHistorique ?? 6;
    this.delaiLivraisonJours = this.delaiLivraisonJours ?? 7;
  }
}

/**
 * Request pour initialiser une configuration
 */
export interface IInitConfigurationRequest {
  produitId: number;
  classeCriticite?: ClasseCriticite;
}

/**
 * Request pour import historique
 */
export interface IImportHistoricalRequest {
  nbMois: number;
}

/**
 * Statut de l'agrégation mensuelle
 */
export interface IAggregationStatus {
  currentMonth: string;
  currentMonthProductCount: number;
  lastMonth: string;
  lastMonthProductCount: number;
  lastMonthFrozen: boolean;
  freezeDelayDays: number;
}

/**
 * Response générique
 */
export interface IMessageResponse {
  message: string;
}

/**
 * Response initialisation en masse
 */
export interface IInitAllResponse {
  configurationsCreated: number;
}
