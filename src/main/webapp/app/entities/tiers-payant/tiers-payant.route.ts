import { Routes } from '@angular/router';

export const tiersPayantRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./tiers-payant-home.component').then(m => m.TiersPayantHomeComponent),
  },
];

export default tiersPayantRoute;
