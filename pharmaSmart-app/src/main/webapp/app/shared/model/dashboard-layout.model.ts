/**
 * Dashboard Scope Enumeration
 */
export enum DashboardScope {
  PRIVATE = 'PRIVATE',
  SHARED = 'SHARED',
  PUBLIC = 'PUBLIC',
}

/**
 * Dashboard Layout Interface
 *
 * Le contenu de `layoutConfig` (grille de widgets) est décrit dans
 * `features/dashboard/models/dashboard.model.ts`.
 */
export interface IDashboardLayout {
  id?: number;
  /**
   * Nom du layout.
   * Si isRoute=true : contient la route Angular (ex: /sales-home/prevente).
   * Si isRoute=false : nom d'affichage du layout.
   */
  name?: string;
  description?: string;
  userId?: number;
  userLogin?: string;
  /** Rôles auxquels ce layout est assigné (many-to-many via dashboard_layout_authority). */
  authorityNames?: string[];
  scope?: DashboardScope;
  isDefault?: boolean;
  /**
   * Si true : name est une route Angular → HomeComponent redirige.
   * Si false : componentKey détermine le composant.
   */
  isRoute?: boolean;
  /**
   * Clé de dispatch Angular — composant à rendre (ignorée si isRoute=true).
   * Valeurs : 'PHARMACIEN' | 'CAISSIER' | 'COMMANDE' | 'ROUTE' | 'CUSTOM' (dashboard personnalisable)
   * Indépendant du rôle : un nouveau rôle peut réutiliser un composant existant.
   */
  componentKey?: string;
  layoutConfig?: string; // JSON string (null si isRoute=true)
  createdAt?: Date;
  updatedAt?: Date;
}
