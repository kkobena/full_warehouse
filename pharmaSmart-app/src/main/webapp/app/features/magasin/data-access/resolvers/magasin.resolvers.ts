import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, RedirectCommand, ResolveFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { IMagasin, TypeMagasin } from 'app/shared/model/magasin.model';
import { MagasinService } from '../services/magasin.service';
import { nouvelleUniteDeGestion } from '../../models/magasin.model';

/** Magasin à modifier. Introuvable : page 404, plutôt qu'un formulaire vide qui créerait un doublon. */
export const magasinResolver: ResolveFn<IMagasin> = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  return inject(MagasinService)
    .find(Number(route.params['id']))
    .pipe(map(res => res.body ?? new RedirectCommand(router.parseUrl('/404'))));
};

/**
 * Nouvelle unité de gestion, rattachée à l'officine de l'utilisateur. Seule une officine en porte :
 * depuis une unité ou un dépôt, retour à la fiche plutôt qu'un enregistrement que le serveur refuserait.
 */
export const nouvelleUniteDeGestionResolver: ResolveFn<IMagasin> = () => {
  const router = inject(Router);
  return inject(MagasinService)
    .getCurrenttUserMagasin()
    .pipe(
      map(res =>
        res.body?.typeMagasin === TypeMagasin.OFFICINE
          ? nouvelleUniteDeGestion(res.body)
          : new RedirectCommand(router.parseUrl('/magasin')),
      ),
    );
};
