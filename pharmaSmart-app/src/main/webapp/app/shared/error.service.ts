import { inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ErrorService {
  translateService = inject(TranslateService);

  getErrorMessageTranslation(errorKey: string): Observable<any> {
    if (errorKey) {
      return this.translateService.get('error.' + errorKey);
    }
    return new Observable<{}>();
  }

  /**
   * Message à présenter à l'utilisateur pour une erreur HTTP.
   *
   * Lit le message renvoyé par le back (`error.error.message`) plutôt que celui de
   * `HttpErrorResponse#message`, qui n'est qu'une trace technique du type
   * « Http failure response for /api/… : 500 Internal Server Error ».
   *
   * @param fallback texte utilisé quand le back n'a rien fourni d'exploitable. Le préciser
   *                 permet de nommer l'action qui a échoué plutôt que de servir un générique.
   */
  getErrorMessage(error: any, fallback = 'Erreur interne du serveur.'): string {
    const status = error?.status;
    if (status < 405) {
      // `detail` en second : ExceptionTranslator renseigne les deux champs à l'identique,
      // mais les erreurs qui ne passent pas par lui (validation Spring MVC, par exemple)
      // ne portent qu'un ProblemDetail standard, donc `detail` seul.
      return error?.error?.message || error?.error?.detail || fallback;
    }
    return fallback;
  }
}
