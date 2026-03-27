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
  /** true = qté verrouillée manuellement — le batch SEMOIS ne la modifiera pas. */
  quantiteModifieeManuel: boolean;
  commandee: boolean;
  /** S4.4 — Nombre d'unités par colis. 1 = pas de contrainte. */
  qteColis?: number;
  /** S4.4 — Quantité minimale de commande (en unités). 0 = pas de minimum. */
  qteMinimaleCommande?: number;
}

export interface CommanderRequest {
  suggestionId: number;
  fournisseurId: number;
  fournisseurLibelle: string;
  lignes: SuggestionLigneEnrichie[];
  montantTotal: number;
}
