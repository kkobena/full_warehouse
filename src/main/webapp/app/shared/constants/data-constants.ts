import { CodeValue } from '../code-value';

export const MODE_EDITIONS_FACTURE: CodeValue[] = [
  { code: 'SELECTED', value: 'Sélection massive' },
  { code: 'SELECTION_BON', value: 'Par sélection de bons' },
  { code: 'TYPE', value: 'Par type tiers-payant' },
  { code: 'TIERS_PAYANT', value: 'Par tiers-payant' },
  { code: 'GROUP', value: "Par groupes et compagnies d'assurances" },
];

export const CATEGORIE_TIRERS_PAYANT: CodeValue[] = [
  { code: 'ASSURANCE', value: 'Assurance' },
  { code: 'CARNET', value: 'Carnet' },
];
export const INVOICES_STATUT: CodeValue[] = [
  { code: null, value: 'Tout' },
  { code: 'PAID', value: 'Réglées' },
  { code: 'PARTIALLY_PAID', value: 'Partiellement réglées/Non réglées' },
  { code: 'NOT_PAID', value: 'Non réglées' },
];
