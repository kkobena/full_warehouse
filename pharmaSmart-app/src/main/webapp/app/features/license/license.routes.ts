import { Routes } from '@angular/router';

/**
 * Écran de gestion de la licence.
 *
 * Aucune garde de licence n'est — et ne devra jamais être — posée ici : c'est la seule porte de
 * sortie d'une officine dont l'abonnement a expiré. Le contrôle d'accès est porté par la route
 * `/licence` déclarée dans `app.routes.ts` : `abilitySubject: 'gestion-licence'`, donc délégable à
 * un rôle non administrateur (cf. docs/PLAN-GESTION-LICENCE.md §5.5).
 */
const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./license-admin.component'),
    title: 'Gérer ma licence',
  },
];

export default routes;
