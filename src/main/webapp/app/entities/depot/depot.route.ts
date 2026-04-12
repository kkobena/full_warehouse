import { Routes } from '@angular/router';
import { AuthGuard } from '../../core/auth/auth.guard';

const depotRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('../depot/depot-home/depot-home.component').then(m => m.DepotHomeComponent),
    data: { pageTitle: 'Gestion des Dépôts' },
  },
  {
    path: 'new',
    loadComponent: () => import('./depot-form.component').then(m => m.DepotFormComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Nouveau Dépôt', abilitySubject: 'depot.liste-depots' },
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./depot-form.component').then(m => m.DepotFormComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Modifier Dépôt', abilitySubject: 'depot.liste-depots' },
  },
  {
    path: 'new-return',
    loadComponent: () => import('./depot-returns/depot-returns.component').then(m => m.DepotReturnsComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Nouveau Retour Dépôt', abilitySubject: 'depot.retour-depot' },
  },
];

export default depotRoutes;
