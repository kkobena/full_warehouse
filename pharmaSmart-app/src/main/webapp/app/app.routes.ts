import { Routes } from '@angular/router';

import { AuthGuard } from 'app/core/auth/auth.guard';
import { errorRoute } from './layouts/error/error.route';

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
    // abilitySubject 'admin' : ROLE_ADMIN est bypassed (step 2 du guard).
    // Tout autre rôle : can('access','admin')=false (pas de nav_item 'admin' dans son tree) → refusé.
    data: { abilitySubject: 'admin' },
    canActivate: [AuthGuard],
    loadChildren: () => import('./admin/admin.routes'),
  },
  {
    path: 'account',
    loadChildren: () => import('./account/account.route'),
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
