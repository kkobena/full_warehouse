/**
 * Onglets de la page « Pilotage de l'officine » (docs/PLAN-PILOTAGE-ONGLETS.md). Le `code` est celui du `nav_item`
 * (migration V2.1.32) : il porte le droit, le libellé et l'icône ; les replis servent tant que l'arbre n'est pas chargé.
 */
export interface OngletPilotage {
  id: string;
  code: string;
  libelle: string;
  icone: string;
  question: string;
  /** Ce qu'on trouve dans l'onglet, dit dans l'infobulle de l'onglet. */
  aide: string;
  phase: number;
}

export const ONGLETS_PILOTAGE: readonly OngletPilotage[] = [
  {
    id: 'tableau-de-bord',
    code: 'pilotage.tableau-de-bord',
    libelle: 'Tableau de bord',
    icone: 'pi pi-gauge',
    question: "Comment va l'officine ?", aide: 'Les chiffres clés de la période face à la référence choisie, ce qui explique l\'écart de CA, et les alertes du jour.',
    phase: 2,
  },
  { id: 'analyser', code: 'pilotage.analyser', libelle: 'Analyser', icone: 'pi pi-chart-scatter', question: 'Pourquoi ça bouge ?', aide: 'Ventilez un ou plusieurs indicateurs par famille, produit, vendeur… et descendez d\'un niveau en cliquant sur une ligne.', phase: 3 },
  {
    id: 'comparer-annees',
    code: 'pilotage.comparer-annees',
    libelle: 'Comparer les années',
    icone: 'pi pi-chart-line',
    question: 'Où en est-on par rapport aux années passées ?', aide: 'Les années civiles mois par mois ; l\'année en cours se compare à la même date de l\'année précédente.',
    phase: 4,
  },
  {
    id: 'rentabilite-remises',
    code: 'pilotage.rentabilite-remises',
    libelle: 'Rentabilité & remises',
    icone: 'pi pi-percentage',
    question: "Où est-ce que je gagne ou perds de l'argent ?", aide: 'La marge et ce qui la fait bouger, les remises accordées et ce qu\'elles coûtent, la démarque (périmés, casse…).',
    phase: 4,
  },
  {
    id: 'achats-stock',
    code: 'pilotage.achats-stock',
    libelle: 'Achats & stock',
    icone: 'pi pi-box',
    question: 'Mes achats et mon stock sont-ils maîtrisés ?', aide: 'Achats reçus, achats face aux ventes, valeur et rotation du stock, ruptures et péremptions.',
    phase: 6,
  },
  {
    id: 'tresorerie-tiers-payant',
    code: 'pilotage.tresorerie-tiers-payant',
    libelle: 'Trésorerie & tiers payant',
    icone: 'pi pi-wallet',
    question: 'Mon argent rentre-t-il ?', aide: 'Encaissements par mode et par caissier, créances tiers payant et encaissements attendus, différés clients.',
    phase: 6,
  },
  {
    id: 'clients-equipe',
    code: 'pilotage.clients-equipe',
    libelle: 'Clients & équipe',
    icone: 'pi pi-users',
    question: 'Qui vient, qui vend ?', aide: 'Fréquentation par heure et par jour, clients identifiés (actifs, nouveaux, perdus), résultats de chaque vendeur.',
    phase: 6,
  },
  { id: 'objectifs', code: 'pilotage.objectifs', libelle: 'Objectifs', icone: 'pi pi-flag', question: 'Suis-je en avance ou en retard ?', aide: 'Objectifs mensuels du titulaire : suivi mois par mois et projection de la fin du mois.', phase: 7 },
];
