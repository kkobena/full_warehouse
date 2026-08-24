import { IconProp } from '@fortawesome/fontawesome-svg-core';

export interface NavItem {

  id: string;
  label: string;
  routerLink?: string;
  authorities?: string[];
  faIcon?: IconProp;
  children?: NavItem[];
  click?: () => void;
  /** Nombre affiché dans le badge (0 ou undefined = pas de badge) */
  badge?: number;
  /** Couleur Bootstrap du badge : 'danger' | 'warning' | 'info' | 'success' (défaut: 'danger') */
  badgeSeverity?: string;
  /** Séparateur visuel dans un sous-menu (dropdown-divider) */
  divider?: boolean;
  /** En-tête de groupe non-cliquable dans un sous-menu */
  groupLabel?: string;
}

/** Marques diacritiques combinantes (U+0300–U+036F), retirées après normalisation NFD. */
const COMBINING_MARKS = /[̀-ͯ]/g;

/**
 * Repli lorsqu'aucun identifiant métier n'est disponible : dérive une clé
 * lisible depuis un libellé (accents retirés, non-alphanumériques en tirets).
 */
export function navItemIdFromLabel(label: string): string {
  return label
    .normalize('NFD')
    .replace(COMBINING_MARKS, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}
