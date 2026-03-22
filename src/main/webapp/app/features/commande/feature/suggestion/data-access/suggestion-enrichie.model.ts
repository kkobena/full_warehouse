// Models for the enriched suggestion view
import { ClasseCriticite } from 'app/shared/model/semois/classe-criticite.model';
import { EtatProduit } from 'app/shared/model/etat-produit.model';

export type NiveauUrgence = 'URGENT' | 'NORMAL' | 'OK';
export type SourceCalcul = 'SEMOIS' | 'P2' | 'CLASSIQUE';

export interface FournisseurSuggestionSummary {
  fournisseurId: number;
  libelle: string;
  suggestionId?: number;
  nbProduits: number;
  nbUrgents: number;
  montantEstime: number;
  source: 'STANDARD' | 'SEMOIS' | 'MIXTE';
  statut?: string;
}

export interface SuggestionLigneEnrichie {
  id?: number;
  produitId?: number;
  fournisseurProduitId?: number;
  libelle: string;
  codeCip: string;
  currentStock: number;
  quantite: number;
  quantiteCalculee: number;
  prixAchat: number;
  prixVente?: number;
  etatProduit?: EtatProduit;
  consommationMensuelle?: Record<string, number>;
  // SEMOIS
  niveauUrgence: NiveauUrgence;
  joursRestants?: number;
  tauxCouvertureMois?: number;
  quantiteSemois?: number;
  classeCriticite?: ClasseCriticite;
  sourceCalcul: SourceCalcul;
  // UI state
  selected: boolean;
  quantiteModifiee: boolean;
  commandee: boolean;
}

export interface CommanderRequest {
  suggestionId: number;
  fournisseurId: number;
  fournisseurLibelle: string;
  lignes: SuggestionLigneEnrichie[];
  montantTotal: number;
}
