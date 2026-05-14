import { Routes } from '@angular/router';
import { AuthGuard } from '../../core/auth/auth.guard';

export const PARTNERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/fournisseur-home/fournisseur-home.component').then(m => m.FournisseurHomeComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Gestion des fournisseurs' },
  },
];
