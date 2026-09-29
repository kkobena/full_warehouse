import { AllowedWidget, DashboardContextSettings, DashboardItem } from './dashboard.model';
import { newItemId } from './layout-config';
import { findWidgetDefinition } from '../widgets/widget-registry';

export interface DashboardTemplate {
  key: string;
  label: string;
  description: string;
  icon: string;
  context: DashboardContextSettings;
  items: Omit<DashboardItem, 'id'>[];
}

/**
 * Points de départ, qui reprennent les tableaux de bord historiques. Ils ne sont pas en base :
 * choisir un modèle ouvre un nouveau dashboard, que l'utilisateur modifie puis enregistre.
 */
export const DASHBOARD_TEMPLATES: readonly DashboardTemplate[] = [
  {
    key: 'pharmacien',
    label: 'Pilotage officine',
    description: 'CA, marge, achats, meilleures ventes, alertes',
    icon: 'pi pi-chart-line',
    context: { period: 'TODAY', refreshSeconds: 0 },
    items: [
      { widgetKey: 'ca-net', x: 0, y: 0, w: 3, h: 2 },
      { widgetKey: 'marge-brute', x: 3, y: 0, w: 3, h: 2 },
      { widgetKey: 'panier-moyen', x: 6, y: 0, w: 3, h: 2 },
      { widgetKey: 'achats-periode', x: 9, y: 0, w: 3, h: 2 },
      { widgetKey: 'ca-evolution', x: 0, y: 2, w: 8, h: 4 },
      { widgetKey: 'modes-reglement', x: 8, y: 2, w: 4, h: 4 },
      { widgetKey: 'top-produits', x: 0, y: 6, w: 6, h: 5 },
      { widgetKey: 'alertes-officine', x: 6, y: 6, w: 3, h: 5 },
      { widgetKey: 'ventes-par-type', x: 9, y: 6, w: 3, h: 5 },
      { widgetKey: 'stock-valorise', x: 0, y: 11, w: 3, h: 2 },
      { widgetKey: 'differes-encours', x: 3, y: 11, w: 3, h: 2 },
      { widgetKey: 'creances-tp', x: 6, y: 11, w: 6, h: 4 },
    ],
  },
  {
    key: 'caisse',
    label: 'Ma caisse',
    description: 'Caisse, encaissements, dernières ventes, différés',
    icon: 'pi pi-wallet',
    context: { period: 'TODAY', refreshSeconds: 300 },
    items: [
      { widgetKey: 'ma-caisse', x: 0, y: 0, w: 4, h: 2 },
      { widgetKey: 'mes-encaissements', x: 4, y: 0, w: 4, h: 4 },
      { widgetKey: 'livraisons-du-jour', x: 8, y: 0, w: 4, h: 4 },
      { widgetKey: 'mes-ventes-recentes', x: 0, y: 4, w: 6, h: 5 },
      { widgetKey: 'differes-a-relancer', x: 6, y: 4, w: 6, h: 5 },
    ],
  },
  {
    key: 'stock',
    label: 'Stock et achats',
    description: 'Alertes, péremptions, commandes, fournisseurs',
    icon: 'pi pi-box',
    context: { period: 'THIS_MONTH', refreshSeconds: 0 },
    items: [
      { widgetKey: 'commandes-en-cours', x: 0, y: 0, w: 3, h: 2 },
      { widgetKey: 'achats-periode', x: 3, y: 0, w: 3, h: 2 },
      { widgetKey: 'alertes-stock', x: 6, y: 0, w: 6, h: 3 },
      { widgetKey: 'peremptions', x: 0, y: 2, w: 6, h: 4 },
      { widgetKey: 'rotation-stock', x: 6, y: 3, w: 6, h: 4 },
      { widgetKey: 'suggestions-reappro', x: 0, y: 6, w: 6, h: 5 },
      { widgetKey: 'performance-fournisseurs', x: 6, y: 7, w: 6, h: 4 },
    ],
  },
];

/** Tuiles du modèle que l'utilisateur a le droit de voir ; un widget interdit en est retiré. */
export function instantiateTemplate(template: DashboardTemplate, allowed: readonly AllowedWidget[]): DashboardItem[] {
  return template.items
    .filter(item => findWidgetDefinition(item.widgetKey) && allowed.some(a => a.key === item.widgetKey))
    .map(item => ({ ...item, id: newItemId() }));
}
