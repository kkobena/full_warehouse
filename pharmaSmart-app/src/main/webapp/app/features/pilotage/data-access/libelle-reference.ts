import { RequetePilotage } from '../models/pilotage.model';

/** Nom de la série de référence d'après la comparaison de la barre : une légende « Référence » ne dit pas à quoi l'on compare. */
export function libellerReference(requete: RequetePilotage): string {
  switch (requete.comparaison) {
    case 'MEME_PERIODE_N_1':
      return 'Même période, année précédente (N-1)';
    case 'ANNEE_N_MOINS_K':
      return `Même période, il y a ${requete.anneesEnArriere} an${requete.anneesEnArriere > 1 ? 's' : ''} (N-${requete.anneesEnArriere})`;
    case 'PERIODE_PRECEDENTE':
      return 'Période précédente (juste avant)';
    case 'PERSONNALISEE':
      return 'Période de comparaison choisie';
    case 'OBJECTIF':
      return 'Objectif';
    default:
      return 'Référence';
  }
}
