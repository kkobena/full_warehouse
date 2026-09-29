import { AllowedWidget, WidgetAvailability } from './dashboard.model';
import { findWidgetDefinition } from '../widgets/widget-registry';

/**
 * Un widget s'affiche s'il est connu du front, autorisé au rôle et inclus dans la licence. Le
 * backend refuse de toute façon ses données : ce calcul ne sert qu'à afficher une tuile explicite
 * plutôt qu'une erreur.
 */
export function widgetAvailability(key: string, allowed: readonly AllowedWidget[]): WidgetAvailability {
  if (!findWidgetDefinition(key)) {
    return 'INCONNU';
  }
  const droit = allowed.find(a => a.key === key);
  if (!droit) {
    return 'NON_AUTORISE';
  }
  return droit.licensed ? 'OK' : 'NON_SOUSCRIT';
}
