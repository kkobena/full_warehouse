import { IconProp } from '@fortawesome/fontawesome-svg-core';

export interface NavItem {
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
