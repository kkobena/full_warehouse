import { Routes } from '@angular/router';

import { magasinResolver, nouvelleUniteDeGestionResolver } from './data-access/resolvers/magasin.resolvers';

const MAGASIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/magasin-home/magasin-home.component').then(m => m.MagasinHomeComponent),
  },
  {
    path: 'unite-gestion/new',
    loadComponent: () => import('./feature/magasin-edit/magasin-edit.component').then(m => m.MagasinEditComponent),
    resolve: { magasin: nouvelleUniteDeGestionResolver },
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./feature/magasin-edit/magasin-edit.component').then(m => m.MagasinEditComponent),
    resolve: { magasin: magasinResolver },
  },
];

export default MAGASIN_ROUTES;
