import { EtatProduit } from '../../../../shared/model/etat-produit.model';

export class SuggestionLine {
  id?: number;
  quantity?: number;
  updatedAt?: Date;
  fournisseurProduitLibelle?: string;
  fournisseurProduitCip?: string;
  fournisseurProduitCodeEan?: string;
  produitId?: number;
  fournisseurProduitId?: number;
  currentStock?: number;
  etatProduit?: EtatProduit;
  prixAchat?: number;
  prixVente?: number;
  consommationMensuelle?: Record<string, number>;
  niveauUrgence?: string;
  joursRestants?: number | null;
  sourceCalcul?: string;
  /** true = qté modifiée manuellement par le pharmacien (protégée du batch SEMOIS). */
  quantiteModifieeManuel?: boolean;
  /** S4.4 — Nombre d'unités par colis. 1 = pas de contrainte. */
  qteColis?: number;
  /** S4.4 — Quantité minimale de commande (en unités). 0 = pas de minimum. */
  qteMinimaleCommande?: number;
}
