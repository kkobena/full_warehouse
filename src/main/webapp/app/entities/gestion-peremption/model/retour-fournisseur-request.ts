/**
 * Requête de création d'un RetourBon depuis un lot périmé.
 * Le backend résout automatiquement la Commande source via Lot → OrderLine → Commande.
 * Si {@link commandeId} et {@link commandeOrderDate} sont fournis, ils sont utilisés en fallback.
 * Si {@link fournisseurId} est fourni, un retour "hors commande" est créé avec ce fournisseur.
 */
export interface RetourFournisseurRequest {
  lotId: number;
  motifRetourId: number;
  quantity: number;
  commentaire?: string;
  /**  commande sélectionnée manuellement */
  commandeId?: number;
  commandeOrderDate?: string;
  /** fournisseur sélectionné pour un retour hors commande (cas HORS_COMMANDE_MULTI) */
  fournisseurId?: number;
}

export interface RetourFournisseurBatchRequest {
  lots: RetourFournisseurRequest[];
}

export interface RetourBonBatchResult {
  totalCreated: number;
  totalErrors: number;
  created: any[];
  errors: RetourBonBatchError[];
}

export interface RetourBonBatchError {
  lotId: number;
  lotNumero: string;
  message: string;
}

// ── Types de résolution du lot ─────────────────────────────────────────────

export type ResolutionStatut =
  | 'COMMANDE_TROUVEE'
  | 'HORS_COMMANDE_UN_FOURN'
  | 'HORS_COMMANDE_MULTI'
  | 'FOURNISSEUR_INCONNU';

export interface FournisseurSimple {
  id: number;
  libelle: string;
}

export interface RetourBonLotResolution {
  statut: ResolutionStatut;
  // COMMANDE_TROUVEE
  commandeId?: number;
  commandeOrderDate?: string;
  commandeReference?: string;
  // HORS_COMMANDE_UN_FOURN
  fournisseurId?: number;
  fournisseurLibelle?: string;
  // HORS_COMMANDE_MULTI
  fournisseurs?: FournisseurSimple[];
}


