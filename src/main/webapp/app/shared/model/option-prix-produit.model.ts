export interface IOptionPrixProduit {
  id?: number;
  price?: number;
  tiersPayantId?: number;
  tiersPayantName?: string;
  enabled?: boolean;
  rate?: number;
  /** REFERENCE | POURCENTAGE | MIXED_REFERENCE_POURCENTAGE */
  type?: string;
  produitId?: number;
}

export const OPTION_PRIX_TYPE_OPTIONS = [
  { value: 'REFERENCE', label: 'Prix de référence', description: 'Montant fixe accordé par l\'assurance' },
  { value: 'POURCENTAGE', label: 'Pourcentage', description: 'Taux appliqué sur le prix de vente' },
  { value: 'MIXED_REFERENCE_POURCENTAGE', label: 'Mixte', description: 'Taux appliqué sur un prix de référence' },
];

