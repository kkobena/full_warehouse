import { Routes } from '@angular/router';
import { AuthGuard } from 'app/core/auth/auth.guard';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./client-update.component').then(m => m.ClientUpdateComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Mise à jour du poste' },
  },
];

export default routes;
