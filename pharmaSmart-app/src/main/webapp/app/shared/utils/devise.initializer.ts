import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of, tap } from 'rxjs';

import { setCurrencyUnit } from './format-utils';

/** Ce que renvoie `GET /api/app/{id}` — seule la valeur nous intéresse ici. */
interface AppConfigurationDto {
  value?: string;
}

/**
 * Charge la devise de l'officine au démarrage et la pose dans `format-utils`.
 *
 * <p>Un seul appel, avant le premier écran : la devise ne change pas en cours de session, et la
 * relire à chaque montant formaté coûterait une injection dans des formateurs qui n'en ont pas.
 *
 * <p><strong>Ne bloque jamais le démarrage.</strong> Une officine dont la configuration est
 * absente, ou dont le serveur répond mal, doit pouvoir vendre : l'échec est avalé et le repli
 * « FCFA » de `format-utils` s'applique.
 */
export function deviseInitializer() {
  return inject(HttpClient)
    .get<AppConfigurationDto>('api/app/APP_DEVISE')
    .pipe(
      tap(config => setCurrencyUnit(config?.value)),
      catchError(() => of(null)),
    );
}
