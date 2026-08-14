import {Routes} from '@angular/router';

import {AuthGuard} from 'app/core/auth/auth.guard';
import {errorRoute} from './layouts/error/error.route';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home.component'),
    title: 'home.title',
  },
  {
    path: '',
    loadComponent: () => import('./layouts/navbar/navbar.component'),
    outlet: 'navbar',
  },
  {
    path: '',
    loadComponent: () => import('./layouts/sidebar/sidebar.component'),
    outlet: 'sidebar',
  },
  {
    path: 'admin',

    data: {abilitySubject: 'admin'},
    canActivate: [AuthGuard],
    loadChildren: () => import('./admin/admin.routes'),
  },
  {

    path: 'licence',
    data: {abilitySubject: 'gestion-licence'},
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/license/license.routes'),
  },
  {
    path: 'account',
    loadChildren: () => import('./account/account.route'),
  },
  {
    // Cible de licenseFeatureGuard. Volontairement hors de toute garde : y placer un contrôle
    // reviendrait à refuser l'accès à la page qui explique le refus.
    path: 'module-non-souscrit',
    loadComponent: () => import('./core/license/feature-not-included.component'),
    title: 'Module non souscrit',
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login.component'),
    title: 'login.title',
  },
  {
    path: 'cahier-recette',
    loadComponent: () =>
      import('./features/cahier-recette/cahier-recette.component').then(m => m.CahierRecetteComponent),
    canActivate: [AuthGuard],
    title: 'Cahier de recette',
  },
  {
    path: '',
    loadChildren: () => import(`./entities/entity.routes`),
  },
  ...errorRoute,
];

export default routes;
