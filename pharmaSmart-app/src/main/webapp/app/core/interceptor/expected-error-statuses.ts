import { HttpContext, HttpContextToken } from '@angular/common/http';

/**
 * Statuts d'erreur qu'une requête considère comme une réponse NORMALE : ErrorHandlerInterceptor ne
 * les diffuse pas, et aucune alerte ne s'affiche. L'appelant reste seul juge de ce qu'il en fait.
 *
 * Exemple : « ce poste n'est pas enregistré » arrive en 404 sur `api/postes/current`. C'est un cas
 * prévu — l'application retombe sur la configuration locale — et non une erreur à montrer à
 * l'utilisateur à chaque démarrage.
 */
export const EXPECTED_ERROR_STATUSES = new HttpContextToken<readonly number[]>(() => []);

export function expectErrorStatuses(...statuses: number[]): HttpContext {
  return new HttpContext().set(EXPECTED_ERROR_STATUSES, statuses);
}
