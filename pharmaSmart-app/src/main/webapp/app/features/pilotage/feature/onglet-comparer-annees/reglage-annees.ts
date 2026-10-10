import { ParamMap, Params } from '@angular/router';

import { AxeAnalyse, ModeAnnees, ReglageAnnees } from '../../models/pilotage.model';

export const REGLAGE_ANNEES_PAR_DEFAUT: ReglageAnnees = { indicateur: 'CA_TTC', annees: 3, mode: 'MENSUEL', parJourOuvre: false, filtre: null };

/** Réglage de l'onglet dans l'URL, sous des noms propres à l'onglet (`ann*`) : il ne se mêle pas à celui d'Analyser. */
export function lireReglageAnnees(params: ParamMap | undefined): ReglageAnnees {
  if (!params) {
    return REGLAGE_ANNEES_PAR_DEFAUT;
  }
  const filtre = params.get('annF');
  return {
    indicateur: params.get('annInd') ?? REGLAGE_ANNEES_PAR_DEFAUT.indicateur,
    annees: params.has('annNb') ? Number(params.get('annNb')) : REGLAGE_ANNEES_PAR_DEFAUT.annees,
    mode: (params.get('annMode') as ModeAnnees | null) ?? REGLAGE_ANNEES_PAR_DEFAUT.mode,
    parJourOuvre: params.get('annJo') === 'true',
    filtre: filtre ? lireFiltre(filtre) : null,
  };
}

export function versParametresAnnees(reglage: ReglageAnnees): Params {
  return {
    annInd: reglage.indicateur,
    annNb: reglage.annees,
    annMode: reglage.mode,
    annJo: reglage.parJourOuvre ? 'true' : null,
    annF: reglage.filtre ? `${reglage.filtre.axe}:${reglage.filtre.cle}:${reglage.filtre.libelle}` : null,
  };
}

function lireFiltre(valeur: string): ReglageAnnees['filtre'] {
  const [axe, cle, ...libelle] = valeur.split(':');
  return { axe: axe as AxeAnalyse, cle, libelle: libelle.join(':') };
}
