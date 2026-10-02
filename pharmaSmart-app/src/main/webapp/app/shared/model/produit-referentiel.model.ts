export type DecisionRapprochement = 'EN_ATTENTE' | 'AUTO' | 'VALIDE' | 'REJETE';
export type StatutRapprochement = 'SUR' | 'A_VERIFIER' | 'PAR_DCI' | 'NON_TROUVE';

/** Ce que le référentiel médicament dit d'un produit (GET /api/referentiel-medicament/produits/{id}). */
export interface IProduitReferentiel {
  produitId: number;
  statut: StatutRapprochement;
  decision: DecisionRapprochement;
  score: number;
  motif?: string;
  /** Vrai quand l'acceptation automatique a posé les DCI du produit. */
  dciPoseeAutomatiquement: boolean;
  specialite?: {
    cis: string;
    libelle: string;
    forme?: string;
    titulaire?: string;
    groupeGenerique?: string;
  };
}
