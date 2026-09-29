import { DataWidgetSpec, VizType, WidgetCategory, WidgetComponent, WidgetDefinition, WidgetParamDef } from '../models/dashboard.model';
import { Type } from '@angular/core';

/**
 * Widgets que le front sait afficher. Un widget n'est proposé que s'il figure ici ET dans la
 * réponse de `/api/dashboard-widgets/allowed` : la clé doit correspondre au `nav_item`
 * `widget.<key>` créé par migration, et, pour un widget de données, à un WidgetDataProvider.
 */
const dataWidget = (): Promise<Type<WidgetComponent>> => import('./data-widget/data-widget.component').then(m => m.DataWidgetComponent);

const KPI: VizType[] = ['KPI'];
/** Parts d'un tout : barres par défaut, plus lisibles qu'un camembert dès quatre parts. */
const PARTS: VizType[] = ['HBAR', 'BAR', 'DOUGHNUT', 'PIE', 'TABLE'];

function data(
  key: string,
  label: string,
  description: string,
  category: WidgetCategory,
  icon: string,
  size: [number, number],
  spec: DataWidgetSpec,
): WidgetDefinition {
  return { key, label, description, category, icon, defaultSize: { w: size[0], h: size[1] }, data: spec, loadComponent: dataWidget };
}

const kpi = (usesPeriod: boolean): DataWidgetSpec => ({ visualizations: KPI, defaultViz: 'KPI', usesPeriod });
const table = (usesPeriod: boolean, params?: WidgetParamDef[]): DataWidgetSpec => ({
  visualizations: ['TABLE'],
  defaultViz: 'TABLE',
  usesPeriod,
  params,
});

/** Nombre de lignes : mêmes bornes que celles appliquées par le serveur (WidgetPeriods.intParam). */
const limite = (defaultValue: number, max: number): WidgetParamDef => ({
  name: 'limite',
  label: 'Nombre de lignes',
  type: 'number',
  defaultValue: String(defaultValue),
  min: 1,
  max,
});
const TRI: WidgetParamDef = {
  name: 'tri',
  label: 'Classer par',
  type: 'choice',
  defaultValue: 'montant',
  options: [
    { value: 'montant', label: 'Montant' },
    { value: 'quantite', label: 'Quantité' },
  ],
};

export const WIDGET_DEFINITIONS: readonly WidgetDefinition[] = [
  // ── Ventes ────────────────────────────────────────────────────────────────
  data('ca-net', 'CA net', "Chiffre d'affaires net et son évolution", 'VENTES', 'pi pi-shopping-cart', [3, 2], kpi(true)),
  data('ca-vs-n1', "CA comparé à l'an dernier", "CA de la période et des mêmes dates de l'an dernier", 'VENTES', 'pi pi-calendar', [3, 2], kpi(true)),
  data('marge-brute', 'Marge brute', 'Marge sur le CA net, et son taux', 'VENTES', 'pi pi-percentage', [3, 2], kpi(true)),
  data('panier-moyen', 'Panier moyen', 'Montant HT moyen par vente', 'VENTES', 'pi pi-shopping-bag', [3, 2], kpi(true)),
  data('ventes-annulees', 'Ventes annulées', 'Montant et nombre de ventes annulées', 'VENTES', 'pi pi-ban', [3, 2], kpi(true)),
  data('ca-evolution', 'Évolution du CA', 'CA au fil de la période, comparé à la période précédente', 'VENTES', 'pi pi-chart-line', [8, 4], {
    visualizations: ['LINE', 'BAR', 'TABLE'],
    defaultViz: 'LINE',
    usesPeriod: true,
  }),
  data('ventes-par-type', 'Ventes par type', 'Comptant, tiers payant, dépôt', 'VENTES', 'pi pi-chart-pie', [4, 4], {
    visualizations: PARTS,
    defaultViz: 'DOUGHNUT',
    usesPeriod: true,
  }),
  data('modes-reglement', 'Modes de règlement', 'Montants encaissés par mode', 'VENTES', 'pi pi-credit-card', [4, 4], {
    visualizations: PARTS,
    defaultViz: 'HBAR',
    usesPeriod: true,
  }),
  data('top-produits', 'Meilleures ventes', 'Produits les plus vendus en valeur', 'VENTES', 'pi pi-star', [6, 5], {
    visualizations: ['TABLE', 'HBAR'],
    defaultViz: 'TABLE',
    usesPeriod: true,
    chartColumns: ['montant'],
    params: [limite(10, 50), TRI],
  }),

  data('ventes-par-tiers-payant', 'Ventes par tiers payant', 'Montants facturés par organisme', 'VENTES', 'pi pi-id-card', [4, 5], {
    visualizations: ['TABLE', 'HBAR', 'DOUGHNUT'],
    defaultViz: 'TABLE',
    usesPeriod: true,
    params: [limite(10, 50)],
  }),
  data('pareto-produits', 'Pareto 20/80', 'Les produits qui font 80 % des ventes', 'VENTES', 'pi pi-sort-amount-down', [6, 5], {
    visualizations: ['TABLE', 'HBAR'],
    defaultViz: 'TABLE',
    usesPeriod: true,
    chartColumns: ['total'],
    params: [TRI],
  }),

  // ── Caisse ────────────────────────────────────────────────────────────────
  data('ma-caisse', 'Ma caisse', 'Espèces théoriques de ma caisse ouverte', 'CAISSE', 'pi pi-wallet', [3, 2], kpi(false)),
  data('mes-encaissements', 'Mes encaissements', 'Encaissements de ma session, par mode', 'CAISSE', 'pi pi-money-bill', [4, 4], {
    visualizations: PARTS,
    defaultViz: 'HBAR',
    usesPeriod: false,
  }),
  data('mes-ventes-recentes', 'Mes dernières ventes', 'Dernières ventes de ma session', 'CAISSE', 'pi pi-list', [6, 5], table(false, [limite(8, 30)])),
  data('differes-a-relancer', 'Différés à relancer', 'Ventes différées échues, à recouvrer', 'CAISSE', 'pi pi-calendar-times', [6, 5], table(false)),

  // ── Stock ─────────────────────────────────────────────────────────────────
  data('alertes-officine', "Alertes de l'officine", 'Péremptions, ruptures, ajustements, prix, factures', 'STOCK', 'pi pi-bell', [4, 4], {
    visualizations: ['TABLE', 'HBAR'],
    defaultViz: 'TABLE',
    usesPeriod: false,
  }),
  data('alertes-stock', 'Alertes de stock', 'Ruptures, stock critique, réassort', 'STOCK', 'pi pi-exclamation-triangle', [4, 3], {
    visualizations: ['HBAR', 'BAR', 'TABLE'],
    defaultViz: 'HBAR',
    usesPeriod: false,
  }),
  data('peremptions', 'Péremptions à venir', 'Produits qui périment dans les six mois', 'STOCK', 'pi pi-clock', [4, 4], {
    visualizations: ['BAR', 'HBAR', 'TABLE'],
    defaultViz: 'BAR',
    usesPeriod: false,
  }),
  data('rotation-stock', 'Rotation du stock', 'Produits à rotation rapide, normale, lente', 'STOCK', 'pi pi-sync', [4, 4], {
    visualizations: ['DOUGHNUT', 'PIE', 'HBAR', 'TABLE'],
    defaultViz: 'DOUGHNUT',
    usesPeriod: false,
  }),
  data('analyse-abc', 'Analyse ABC', 'Poids des classes A, B et C dans le CA', 'STOCK', 'pi pi-sort-amount-down', [6, 3], {
    visualizations: ['TABLE', 'BAR'],
    defaultViz: 'TABLE',
    usesPeriod: false,
    chartColumns: ['partCa'],
  }),

  data('a-faire', "À faire aujourd'hui", 'Commander, relancer, écouler : les actions prioritaires', 'STOCK', 'pi pi-check-square', [6, 5], table(false, [limite(10, 50)])),
  data('peremptions-valorisees', 'Péremptions valorisées', 'Valeur des lots qui périment dans les six mois', 'STOCK', 'pi pi-hourglass', [3, 2], kpi(false)),
  data('stock-dormant', 'Stock dormant', 'Produits lents classés par valeur immobilisée', 'STOCK', 'pi pi-pause-circle', [6, 5], {
    visualizations: ['TABLE', 'HBAR'],
    defaultViz: 'TABLE',
    usesPeriod: false,
    chartColumns: ['valeur'],
    params: [limite(10, 50)],
  }),
  data('stock-valorise', 'Stock valorisé', "Valeur d'achat du stock et marge potentielle", 'STOCK', 'pi pi-box', [3, 2], kpi(false)),

  // ── Achats ────────────────────────────────────────────────────────────────
  data('achats-par-fournisseur', 'Achats par fournisseur', 'Montants achetés sur la période, par fournisseur', 'ACHATS', 'pi pi-truck', [6, 5], {
    visualizations: ['TABLE', 'HBAR'],
    defaultViz: 'TABLE',
    usesPeriod: true,
    chartColumns: ['montant'],
    params: [limite(10, 50)],
  }),
  data('qualite-fournisseurs', 'Qualité fournisseurs', 'Conformité et délai moyen sur 12 mois', 'ACHATS', 'pi pi-verified', [3, 2], kpi(false)),
  data('achats-periode', 'Achats de la période', 'Montant des réceptions fournisseurs', 'ACHATS', 'pi pi-truck', [3, 2], kpi(true)),
  data('commandes-en-cours', 'Commandes en cours', 'Commandes en attente et à réceptionner', 'ACHATS', 'pi pi-inbox', [3, 2], kpi(false)),
  data('suggestions-reappro', 'Suggestions de commande', 'Produits à réapprovisionner', 'ACHATS', 'pi pi-lightbulb', [6, 5], table(false)),
  data('performance-fournisseurs', 'Performance fournisseurs', 'Délai, conformité et note des fournisseurs', 'ACHATS', 'pi pi-chart-bar', [6, 4], {
    visualizations: ['TABLE', 'HBAR'],
    defaultViz: 'TABLE',
    usesPeriod: false,
    chartColumns: ['conformite'],
    params: [limite(5, 20)],
  }),
  data('livraisons-du-jour', 'Livraisons du jour', "Commandes attendues aujourd'hui", 'ACHATS', 'pi pi-box', [4, 3], table(false)),

  // ── Finances ──────────────────────────────────────────────────────────────
  data('marge-12-mois', 'Marge sur 12 mois', 'Marge brute et taux moyen, 12 mois glissants', 'FINANCES', 'pi pi-chart-line', [3, 2], kpi(false)),
  data('creances-tp', 'Créances tiers payant', "Encours des organismes par ancienneté", 'FINANCES', 'pi pi-credit-card', [4, 4], {
    visualizations: ['BAR', 'HBAR', 'TABLE'],
    defaultViz: 'BAR',
    usesPeriod: false,
  }),
  data('creances-par-organisme', 'Créances par organisme', 'Encours et part de plus de 90 jours', 'FINANCES', 'pi pi-building', [6, 5], {
    visualizations: ['TABLE', 'HBAR'],
    defaultViz: 'TABLE',
    usesPeriod: false,
    chartColumns: ['encours'],
  }),
  data('differes-encours', 'Différés clients', 'Montant restant dû sur les ventes différées', 'FINANCES', 'pi pi-users', [3, 2], kpi(false)),

  // ── Mise en page ──────────────────────────────────────────────────────────
  {
    key: 'note',
    label: 'Note',
    description: 'Texte libre : consigne, rappel, commentaire',
    category: 'MISE_EN_PAGE',
    icon: 'pi pi-align-left',
    defaultSize: { w: 4, h: 2 },
    loadComponent: () => import('./note/note-widget.component').then(m => m.NoteWidgetComponent),
  },
  {
    key: 'titre-section',
    label: 'Titre de section',
    description: 'Intertitre pour regrouper les tuiles',
    category: 'MISE_EN_PAGE',
    icon: 'pi pi-minus',
    defaultSize: { w: 12, h: 1 },
    frameless: true,
    loadComponent: () => import('./section-title/section-title-widget.component').then(m => m.SectionTitleWidgetComponent),
  },
];

export function findWidgetDefinition(key: string): WidgetDefinition | undefined {
  return WIDGET_DEFINITIONS.find(d => d.key === key);
}
