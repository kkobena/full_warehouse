import { InputSignal, OutputEmitterRef, Type } from '@angular/core';

export const DASHBOARD_CONFIG_VERSION = 2;

/** Une tuile de la grille : position GridStack + widget affiché. */
export interface DashboardItem {
  id: string;
  x: number;
  y: number;
  w: number;
  h: number;
  widgetKey: string;
  /** Titre personnalisé ; à défaut, le libellé du widget. */
  title?: string;
  params?: Record<string, string>;
}

/** Contenu de `dashboard_layout.layout_config` (format 2). */
export interface DashboardConfig {
  version: typeof DASHBOARD_CONFIG_VERSION;
  items: DashboardItem[];
  /** Période et rafraîchissement communs à toutes les tuiles ; absent = valeurs par défaut. */
  context?: DashboardContextSettings;
}

export type PeriodPreset = 'TODAY' | 'YESTERDAY' | 'LAST_7_DAYS' | 'LAST_30_DAYS' | 'THIS_MONTH' | 'LAST_MONTH' | 'THIS_YEAR';

export interface DashboardContextSettings {
  period: PeriodPreset;
  /** 0 = pas de rafraîchissement automatique. */
  refreshSeconds: number;
}

/** Bornes au format ISO `YYYY-MM-DD`, telles qu'attendues par l'API. */
export interface DateRange {
  start: string;
  end: string;
}

/** Visualisations proposées pour un widget de données. `HBAR` : barres horizontales. */
export type VizType = 'KPI' | 'LINE' | 'BAR' | 'HBAR' | 'PIE' | 'DOUGHNUT' | 'TABLE';

export const VIZ_LABELS: Record<VizType, string> = {
  KPI: 'Indicateur',
  LINE: 'Courbe',
  BAR: 'Barres',
  HBAR: 'Barres horizontales',
  PIE: 'Camembert',
  DOUGHNUT: 'Anneau',
  TABLE: 'Tableau',
};

export interface DataWidgetSpec {
  visualizations: VizType[];
  defaultViz: VizType;
  /** Le widget suit la période du dashboard ; sinon c'est un état à l'instant présent. */
  usesPeriod: boolean;
  /**
   * Colonnes d'un tableau tracées en graphique. À préciser dès que le tableau porte des mesures
   * d'échelles différentes (quantité et montant) : elles ne partagent jamais un axe.
   */
  chartColumns?: string[];
  /** Paramètres réglables dans la fenêtre de réglages, transmis au serveur. */
  params?: WidgetParamDef[];
}

/** Paramètre d'un widget de données ; sa valeur est stockée en texte dans `DashboardItem.params`. */
export interface WidgetParamDef {
  name: string;
  label: string;
  type: 'number' | 'choice';
  defaultValue: string;
  min?: number;
  max?: number;
  options?: { value: string; label: string }[];
}

/**
 * Paramètres de tuile gérés par le front, jamais envoyés au serveur : visualisation, texte des
 * widgets de mise en page, période propre, seuil d'alerte d'un indicateur.
 */
export const FRONT_ONLY_PARAMS = ['viz', 'text', 'periode', 'seuil', 'alerte'] as const;

/** Un indicateur passe en alerte au-dessous (stock, CA) ou au-dessus (créances) de son seuil. */
export type KpiAlertDirection = 'DESSOUS' | 'DESSUS';

// ── Réponse de GET /api/dashboard-widgets/{key} (cf. WidgetData.java) ──────────

export interface KpiData {
  kind: 'KPI';
  value: number | null;
  previousValue: number | null;
  unit: string | null;
  detail: string | null;
  /** À quoi la valeur est comparée ; absent : la période précédente de même durée. */
  previousLabel?: string | null;
}

export interface SeriesData {
  kind: 'SERIES';
  labels: string[];
  series: { name: string; values: (number | null)[] }[];
  footer: string | null;
}

export type ColumnType = 'string' | 'number' | 'amount' | 'percent' | 'date' | 'datetime';

export interface TableColumn {
  field: string;
  header: string;
  type: ColumnType;
}

export interface TableData {
  kind: 'TABLE';
  columns: TableColumn[];
  rows: Record<string, unknown>[];
  footer: string | null;
}

export type WidgetData = KpiData | SeriesData | TableData;

export type GridPosition = Pick<DashboardItem, 'id' | 'x' | 'y' | 'w' | 'h'>;

export type WidgetCategory = 'VENTES' | 'CAISSE' | 'STOCK' | 'ACHATS' | 'FINANCES' | 'CLIENTS' | 'MISE_EN_PAGE';

export const WIDGET_CATEGORY_LABELS: Record<WidgetCategory, string> = {
  VENTES: 'Ventes',
  CAISSE: 'Caisse',
  STOCK: 'Stock',
  ACHATS: 'Achats',
  FINANCES: 'Finances',
  CLIENTS: 'Clients',
  MISE_EN_PAGE: 'Mise en page',
};

/**
 * Contrat d'un composant de widget : la tuile lui passe son item et le mode édition. Un widget
 * éditable sur place (note, titre) émet ses nouveaux paramètres par `paramsChange` ; les autres
 * déclarent la sortie sans l'utiliser, la tuile s'y abonnant toujours.
 */
export interface WidgetComponent {
  item: InputSignal<DashboardItem>;
  editMode: InputSignal<boolean>;
  paramsChange: OutputEmitterRef<Record<string, string>>;
}

/** Déclaration front d'un widget. Son droit d'usage vient du backend (`widget.<key>`). */
export interface WidgetDefinition {
  key: string;
  label: string;
  description: string;
  category: WidgetCategory;
  icon: string;
  defaultSize: { w: number; h: number };
  /** Sans titre ni cadre : le widget occupe toute la tuile (titre de section). */
  frameless?: boolean;
  /** Présent pour un widget de données, servi par `/api/dashboard-widgets/{key}`. */
  data?: DataWidgetSpec;
  loadComponent: () => Promise<Type<WidgetComponent>>;
}

/** Réponse de `GET /api/dashboard-widgets/allowed`. */
export interface AllowedWidget {
  key: string;
  licensed: boolean;
}

/** État d'une tuile au regard des droits de l'utilisateur. */
export type WidgetAvailability = 'OK' | 'NON_AUTORISE' | 'NON_SOUSCRIT' | 'INCONNU';
