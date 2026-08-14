import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { LicenseService } from 'app/core/license/license.service';
import { NotificationService } from 'app/shared/services/notification.service';

/**
 * Traduit les refus de licence (**HTTP 402**) en message lisible — exigence B4 côté client.
 *
 * <p>Le statut 402 a précisément été retenu côté serveur parce qu'il n'est **pas** intercepté par
 * {@link AuthExpiredInterceptor} : répondre 401 ou 403 déconnecterait l'utilisateur, qui ne pourrait
 * alors plus atteindre l'écran de renouvellement. Cet intercepteur ne déclenche donc **aucune
 * déconnexion ni redirection** — il informe, et laisse l'utilisateur là où il est.
 *
 * <p>Il rafraîchit au passage le statut local : c'est ce qui fait apparaître la bannière (B3) dès la
 * première écriture refusée, sans attendre le rechargement horaire.
 *
 * Cf. docs/PLAN-GESTION-LICENCE.md §5.4.
 */
export const licenseInterceptor: HttpInterceptorFn = (req, next) => {
  const licenseService = inject(LicenseService);
  const notification = inject(NotificationService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 402) {
        notification.error(messageOf(error), titleOf(error));

        // Sauf sur /api/license lui-même : le service vient déjà d'y répondre, et relancer un appel
        // sur l'endpoint qui échoue provoquerait une boucle.
        if (!req.url.includes('api/license')) {
          licenseService.load().subscribe();
        }
      }
      return throwError(() => error);
    }),
  );
};

/**
 * Le serveur renvoie un message métier explicite (échéance, officine, module non souscrit) dans
 * `message`, avec `detail` en repli RFC 7807. On l'affiche tel quel : un « accès refusé » générique
 * n'indiquerait pas à l'utilisateur quoi faire ni qui appeler.
 */
function messageOf(error: HttpErrorResponse): string {
  const body = error.error as { message?: string; detail?: string } | null;
  return body?.message ?? body?.detail ?? "Cette action n'est pas autorisée : votre licence n'est pas valide.";
}

function titleOf(error: HttpErrorResponse): string {
  const body = error.error as { errorKey?: string } | null;
  return body?.errorKey === 'license.feature.notIncluded' ? 'Module non souscrit' : 'Licence';
}
