import dayjs, { Dayjs } from 'dayjs/esm';

import { Granularite, RequetePilotage } from '../models/pilotage.model';

const FORMAT_ISO = 'YYYY-MM-DD';
const TRANCHES = 13;
/** Les années remontent moins loin : la comparaison s'arrête à N-5. */
const ANNEES = 6;

/**
 * La période de la barre élargie aux 13 dernières tranches de sa granularité (6 pour l'année) : une courbe montre la tendance,
 * pas un point isolé. Jamais rétrécie : une période déjà plus longue reste telle quelle.
 */
export function elargirAuxTreizeTranches(requete: RequetePilotage): RequetePilotage {
  const du = lireDebutHorizon(dayjs(requete.au), requete.granularite).format(FORMAT_ISO);
  return du < requete.du ? { ...requete, du } : requete;
}

/** « 13 derniers mois », « 6 dernières années »… pour le titre de la courbe. */
export function libellerHorizon(granularite: Granularite): string {
  return {
    JOUR: '13 derniers jours',
    SEMAINE: '13 dernières semaines',
    MOIS: '13 derniers mois',
    TRIMESTRE: '13 derniers trimestres',
    ANNEE: `${ANNEES} dernières années`,
  }[granularite];
}

function lireDebutHorizon(fin: Dayjs, granularite: Granularite): Dayjs {
  switch (granularite) {
    case 'JOUR':
      return fin.subtract(TRANCHES - 1, 'day');
    case 'SEMAINE':
      return fin.subtract((fin.day() + 6) % 7, 'day').subtract(TRANCHES - 1, 'week');
    case 'MOIS':
      return fin.startOf('month').subtract(TRANCHES - 1, 'month');
    case 'TRIMESTRE':
      return fin
        .startOf('month')
        .subtract(fin.month() % 3, 'month')
        .subtract((TRANCHES - 1) * 3, 'month');
    case 'ANNEE':
      return fin.startOf('year').subtract(ANNEES - 1, 'year');
  }
}
