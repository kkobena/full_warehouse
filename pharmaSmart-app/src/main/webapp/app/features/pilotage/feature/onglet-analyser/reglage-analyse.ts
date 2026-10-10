import { ParamMap, Params } from '@angular/router';

import { AffichageAnalyse, AxeAnalyse, EtapeDescente, ReglageAnalyse, TriAnalyse } from '../../models/pilotage.model';

/** Nombre de séries d'une courbe : au-delà, elle ne se lit plus. */
export const SERIES_MAX_COURBE = 8;

export const REGLAGE_PAR_DEFAUT: ReglageAnalyse = {
  indicateurs: ['CA_TTC'],
  axe: 'FAMILLE',
  axe2: null,
  top: 20,
  tri: 'VALEUR',
  affichage: 'BARRES',
  chemin: [],
};

/**
 * Réglage de l'onglet lu dans l'URL. Une étape de descente s'écrit `AXE:cle:libellé` : le libellé, qui peut contenir des « : »,
 * vient en dernier ; il ne sert qu'au fil d'Ariane.
 */
export function lireReglage(params: ParamMap | undefined): ReglageAnalyse {
  if (!params) {
    return REGLAGE_PAR_DEFAUT;
  }
  const indicateurs = params.get('ind');
  return {
    indicateurs: indicateurs ? indicateurs.split(',') : REGLAGE_PAR_DEFAUT.indicateurs,
    axe: (params.get('axe') as AxeAnalyse | null) ?? REGLAGE_PAR_DEFAUT.axe,
    axe2: (params.get('axe2') as AxeAnalyse | null) ?? null,
    top: params.has('top') ? Number(params.get('top')) : REGLAGE_PAR_DEFAUT.top,
    tri: (params.get('tri') as TriAnalyse | null) ?? REGLAGE_PAR_DEFAUT.tri,
    affichage: (params.get('aff') as AffichageAnalyse | null) ?? REGLAGE_PAR_DEFAUT.affichage,
    chemin: params.getAll('f').map(lireEtape),
  };
}

/** Paramètres de requête du réglage ; `null` efface un paramètre devenu inutile. */
export function versParametresUrl(reglage: ReglageAnalyse): Params {
  return {
    ind: reglage.indicateurs.join(','),
    axe: reglage.axe,
    axe2: reglage.axe2,
    top: reglage.top,
    tri: reglage.tri,
    aff: reglage.affichage,
    f: reglage.chemin.length ? reglage.chemin.map(etape => `${etape.axe}:${etape.cle}:${etape.libelle}`) : null,
  };
}

/** La courbe suit les éléments dans le temps : son second axe est la période, et le nombre de séries est borné. */
export function preparerRequete(reglage: ReglageAnalyse): ReglageAnalyse {
  if (reglage.affichage !== 'COURBE') {
    return reglage;
  }
  const top = reglage.top > 0 ? Math.min(reglage.top, SERIES_MAX_COURBE) : SERIES_MAX_COURBE;
  return { ...reglage, axe2: reglage.axe === 'PERIODE' ? null : 'PERIODE', top };
}

function lireEtape(valeur: string): EtapeDescente {
  const [axe, cle, ...libelle] = valeur.split(':');
  return { axe: axe as AxeAnalyse, cle, libelle: libelle.join(':') };
}
