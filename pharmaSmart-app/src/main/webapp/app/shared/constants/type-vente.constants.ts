import { AppBadgeSeverity } from 'app/shared/ui';

/**
 * Types de vente : codes de l'API, libellés d'écran, et couleur associée.
 *
 * Deux vocabulaires cohabitent et il faut les distinguer :
 *
 *   * les CODES D'AFFICHAGE — `VNO`, `VO`, `VENTES_DEPOTS` — rendus par `TypeVenteDTO` dans
 *     les synthèses et les tableaux de bord ;
 *   * les NOMS D'ENTITÉ — `CashSale`, `ThirdPartySales`, `VenteDepot` — attendus par les
 *     filtres qui interrogent la vue `mv_daily_sales_summary`.
 *
 * Les deux désignent les mêmes trois réalités, et c'est justement ce qui rendait leur
 * dispersion coûteuse : chaque écran retraduisait de son côté, avec ses propres mots. Le
 * tableau de bord affichait « Tiers payant (ordonnancée) » là où la synthèse affichait
 * « Ordonnancée (tiers payant) » — le même chiffre sous deux noms.
 */
export type CodeTypeVente = 'VNO' | 'VO' | 'VENTES_DEPOTS';

/** Libellés d'écran, par code d'affichage. */
export const LIBELLES_TYPE_VENTE: Record<CodeTypeVente, string> = {
  VNO: 'Comptant',
  VO: 'Ordonnancée (tiers payant)',
  VENTES_DEPOTS: 'Dépôt',
};

/**
 * NATURE de vente : le second vocabulaire, celui de la colonne `sales.nature_vente`.
 *
 * Il ne recouvre pas le précédent et il ne faut pas les confondre. Les codes d'affichage
 * distinguent la vente au comptant, l'ordonnancée et le dépôt ; la nature distingue le
 * comptant, l'assurance et le CARNET — un carnet est une vente au comptant du point de vue de
 * l'entité, mais une nature à part du point de vue du règlement. Le dépôt, lui, n'a pas de
 * nature : il est hors du chiffre d'affaires déclaré.
 *
 * Les mots retenus sont ceux que la majorité des écrans emploie déjà — journal des ventes,
 * annulations, devis, ventes en cours — plutôt que ceux du tableau ci-dessus : sur cet axe,
 * « Assurance » est le terme d'usage, et l'aligner sur « Ordonnancée (tiers payant) » aurait
 * harmonisé un écran contre quatre.
 */
export type CodeNatureVente = 'COMPTANT' | 'ASSURANCE' | 'CARNET';

export const LIBELLES_NATURE_VENTE: Record<CodeNatureVente, string> = {
  COMPTANT: 'Comptant',
  ASSURANCE: 'Assurance',
  CARNET: 'Carnet',
};

/** Libellé d'une nature de vente, ou le code brut s'il n'est pas connu. */
export function libelleNatureVente(code?: string | null): string {
  if (!code) {
    return '';
  }
  return LIBELLES_NATURE_VENTE[code as CodeNatureVente] ?? code;
}

/**
 * Couleur du type de vente, la même partout : le vert pour ce qui est encaissé, le bleu pour
 * ce qui attend un règlement, l'orange pour ce qui n'est pas une recette.
 */
export const SEVERITES_TYPE_VENTE: Record<CodeTypeVente, AppBadgeSeverity> = {
  VNO: 'success',
  VO: 'info',
  VENTES_DEPOTS: 'warn',
};

/** Libellé d'un type de vente ; rend le code lui-même s'il est inconnu, jamais une chaîne vide. */
export function libelleTypeVente(code: string | undefined | null): string {
  if (!code) {
    return 'N/A';
  }
  return LIBELLES_TYPE_VENTE[code as CodeTypeVente] ?? code;
}

/** Couleur d'un type de vente, neutre pour un code inconnu. */
export function severiteTypeVente(code: string | undefined | null): AppBadgeSeverity {
  if (!code) {
    return 'secondary';
  }
  return SEVERITES_TYPE_VENTE[code as CodeTypeVente] ?? 'secondary';
}

/**
 * Options de filtrage par type de vente.
 *
 * La VALEUR est le nom d'entité, seul accepté par l'API de filtrage ; le LIBELLÉ vient de la
 * table ci-dessus, pour qu'un filtre et le tableau qu'il filtre parlent la même langue.
 */
export const OPTIONS_FILTRE_TYPE_VENTE: { label: string; value: string | null }[] = [
  { label: 'Tous', value: null },
  { label: LIBELLES_TYPE_VENTE.VO, value: 'ThirdPartySales' },
  { label: LIBELLES_TYPE_VENTE.VNO, value: 'CashSale' },
  { label: LIBELLES_TYPE_VENTE.VENTES_DEPOTS, value: 'VenteDepot' },
];
