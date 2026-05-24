/**
 * Valeurs envoyées au backend par les composants de rapports de ventes.
 * ⚠️  R3 — Ces valeurs correspondent exactement aux chaînes acceptées par
 *     l'API backend (confirmé par audit des HttpParams existants).
 *
 * Endpoint concerné : GET /api/reports/sales-summary (param typeVente)
 * Source d'audit : sales-summary.component.ts, lignes 45-50
 *
 * NE PAS utiliser ces valeurs pour construire des params vers d'autres
 * endpoints sans vérification préalable (ex: pnl-analytique utilise
 * 'COMPTANT'/'ASSURANCE'/'CARNET' — valeurs différentes, endpoint différent).
 */
export enum SaleType {
  THIRD_PARTY = 'ThirdPartySales',
  CASH        = 'CashSale',
  DEPOT       = 'VenteDepot',
}

/** Labels d'affichage uniquement — ne pas utiliser comme valeurs API */
export const SALE_TYPE_LABEL: Record<SaleType, string> = {
  [SaleType.THIRD_PARTY]: 'Vente ordonnancée (VO)',
  [SaleType.CASH]:        'Vente au comptant (VNO)',
  [SaleType.DEPOT]:       'Vente aux dépôts',
};
